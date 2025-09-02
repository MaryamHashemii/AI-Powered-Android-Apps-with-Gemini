package com.example.ai_powered_android_apps_with_gemini

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun ImagePickerScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // TODO: Convert bitmap to Uri and update imageUri
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(data = imageUri).build()
                ),
                contentDescription = "Selected image",
                modifier = Modifier
                    .requiredSize(200.dp)
            )
        } else {
            Text("No image selected")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showDialog = true }) {
            Text("Select Image")
        }

        if (showDialog) {
            ImageSourceDialog(
                onDismissRequest = { showDialog = false },
                onGalleryClick = {
                    imagePickerLauncher.launch("image/*")
                    showDialog = false
                },
                onCameraClick = {
                    cameraLauncher.launch()
                    showDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* TODO: Send prompt with selected image */ },
            enabled = imageUri != null
        ) {
            Text("Send Prompt")
        }
    }
}

@Composable
fun ImageSourceDialog(
    onDismissRequest: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Image Source") },
        text = { Text("Choose an image from the gallery or take a new one with the camera.") },
        confirmButton = {
            Button(onClick = onGalleryClick) {
                Text("Gallery")
            }
        },
        dismissButton = {
            Button(onClick = onCameraClick) {
                Text("Camera")
            }
        }
    )
}