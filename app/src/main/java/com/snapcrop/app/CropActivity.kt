package com.snapcrop.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.snapcrop.app.ui.CropView
import com.snapcrop.app.ui.SnapCropTheme
import com.snapcrop.app.util.ClipboardHelper
import com.snapcrop.app.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CropActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_IMAGE_NAME = "extra_image_name"
    }

    private var imageUri: Uri? = null
    private var imagePath: String? = null
    private var imageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseIntent(intent)

        setContent {
            SnapCropTheme {
                CropScreen(
                    imageUri = imageUri,
                    onDismiss = { finishAndRemoveTask() },
                    onSave = { croppedBitmap -> saveCropped(croppedBitmap) },
                    onCopyAndDelete = { croppedBitmap -> copyAndDelete(croppedBitmap) },
                    onDeleteOnly = { deleteOriginalOnly() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent(intent)
        recreate()
    }

    private fun parseIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                }
            }
            Intent.ACTION_EDIT -> {
                imageUri = intent.data
            }
            else -> {
                val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
                if (uriString != null) {
                    imageUri = Uri.parse(uriString)
                }
                imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
                imageName = intent.getStringExtra(EXTRA_IMAGE_NAME)
            }
        }

        if (imageUri == null) {
            val latest = MediaStoreHelper.getLatestScreenshot(this)
            if (latest != null) {
                imageUri = latest.uri
                imagePath = latest.path
                imageName = latest.name
            }
        }
    }

    private fun saveCropped(croppedBitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val savedUri = MediaStoreHelper.saveCroppedImage(this@CropActivity, croppedBitmap, imageName)
            withContext(Dispatchers.Main) {
                if (savedUri != null) {
                    Toast.makeText(this@CropActivity, R.string.saved_to_gallery, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CropActivity, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
                }
                finishAndRemoveTask()
            }
        }
    }

    private fun copyAndDelete(croppedBitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val copied = ClipboardHelper.copyBitmapToClipboard(this@CropActivity, croppedBitmap)
            if (copied) {
                imageUri?.let { uri ->
                    MediaStoreHelper.deleteScreenshot(this@CropActivity, uri, imagePath)
                }
            }
            withContext(Dispatchers.Main) {
                if (copied) {
                    Toast.makeText(this@CropActivity, R.string.copied_and_deleted, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CropActivity, "Failed to copy to clipboard", Toast.LENGTH_SHORT).show()
                }
                finishAndRemoveTask()
            }
        }
    }

    private fun deleteOriginalOnly() {
        lifecycleScope.launch(Dispatchers.IO) {
            imageUri?.let { uri ->
                MediaStoreHelper.deleteScreenshot(this@CropActivity, uri, imagePath)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CropActivity, R.string.screenshot_deleted, Toast.LENGTH_SHORT).show()
                finishAndRemoveTask()
            }
        }
    }
}

@Composable
fun CropScreen(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit,
    onCopyAndDelete: (Bitmap) -> Unit,
    onDeleteOnly: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var cropBitmapProvider by remember { mutableStateOf<(() -> Bitmap)?>(null) }

    LaunchedEffect(imageUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val bmp = if (imageUri != null) {
                MediaStoreHelper.loadBitmap(context, imageUri)
            } else {
                null
            }
            loadedBitmap = bmp
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0A0A))
            .systemBarsPadding()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (loadedBitmap == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Could not load screenshot",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crop Screenshot",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0x33FFFFFF), RoundedCornerShape(50))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Interactive Crop Canvas Area with bottom padding to give bottom handle ample room
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 20.dp)
                ) {
                    CropView(
                        bitmap = loadedBitmap!!,
                        modifier = Modifier.fillMaxSize(),
                        onCropRectCalculated = { provider ->
                            cropBitmapProvider = provider
                        }
                    )
                }

                // Bottom 3 Action Buttons Bar
                Surface(
                    color = Color(0xFF1C1B1F),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Save
                        OutlinedButton(
                            onClick = {
                                val bmp = cropBitmapProvider?.invoke() ?: loadedBitmap!!
                                onSave(bmp)
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontSize = 13.sp)
                        }

                        // 2. Copy & Delete
                        Button(
                            onClick = {
                                val bmp = cropBitmapProvider?.invoke() ?: loadedBitmap!!
                                onCopyAndDelete(bmp)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy & Delete", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // 3. Delete Only
                        FilledTonalButton(
                            onClick = onDeleteOnly,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
