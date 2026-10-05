package com.example.medvault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Square thumbnail of an image or the first page of a PDF. */
@Composable
fun Thumb(file: File?, mime: String?, size: Dp, modifier: Modifier = Modifier, emptyIcon: String = "📄") {
    val shape = RoundedCornerShape(12.dp)
    val bmp by produceState<ImageBitmap?>(null, file?.absolutePath, mime) {
        value = if (file == null) null else FileStore.thumbnail(file, mime, 240)
    }
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                if (file == null) emptyIcon else if (mime == "application/pdf") "PDF" else "🖼️",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = mutedColor()
            )
        }
    }
}

@Composable
private fun ZoomableImage(file: File) {
    val bmp by produceState<ImageBitmap?>(null, file.absolutePath) {
        value = withContext(Dispatchers.IO) { FileStore.decodeImage(file, 2400)?.asImageBitmap() }
    }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val b = bmp
    if (b == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading…", color = Color.White)
        }
    } else {
        Image(
            bitmap = b,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

@Composable
private fun PdfPage(file: File, index: Int) {
    val bmp by produceState<ImageBitmap?>(null, file.absolutePath, index) {
        value = withContext(Dispatchers.IO) { FileStore.renderPdfPage(file, index, 1100)?.asImageBitmap() }
    }
    val b = bmp
    if (b == null) {
        Box(
            Modifier.fillMaxWidth().height(400.dp).background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) { Text("Loading page ${index + 1}…", color = Color.White) }
    } else {
        Image(
            bitmap = b,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PdfPages(file: File) {
    val count by produceState(0, file.absolutePath) {
        value = withContext(Dispatchers.IO) { FileStore.pdfPageCount(file) }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(10.dp)
    ) {
        items(count) { i -> PdfPage(file, i) }
    }
}

/** Full-screen viewer for a photo (pinch to zoom) or a PDF (scroll). */
@Composable
fun FileViewerDialog(title: String, file: File, mime: String?, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClose) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (mime == "application/pdf") PdfPages(file) else ZoomableImage(file)
                }
            }
        }
    }
}

/** Returns a function that opens the system file picker (photos and PDFs). */
@Composable
fun rememberFilePicker(sub: String, onImported: (FileStore.Imported?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val imp = withContext(Dispatchers.IO) { FileStore.importFile(context, uri, sub) }
                onImported(imp)
            }
        }
    }
    return { launcher.launch(arrayOf("image/*", "application/pdf")) }
}