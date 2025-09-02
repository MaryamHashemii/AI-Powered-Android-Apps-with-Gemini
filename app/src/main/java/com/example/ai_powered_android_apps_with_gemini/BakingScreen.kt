package com.example.ai_powered_android_apps_with_gemini

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.geministarter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun getTmpFileUri(context: Context): Uri? {
    try {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        // Log the directory you are trying to use
        val storageDir: File? = context.cacheDir
        Log.d("getTmpFileUri", "Attempting to use storage directory: ${storageDir?.absolutePath}")
        if (storageDir == null) {
            Log.e("getTmpFileUri", "Cache directory is null!")
            Toast.makeText(context, "Cache directory not available.", Toast.LENGTH_LONG).show()
            return null
        }
        if (!storageDir.exists()) {
            Log.w("getTmpFileUri", "Cache directory does not exist, attempting to create: ${storageDir.mkdirs()}")
        }


        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        Log.d("getTmpFileUri", "Temp file created: ${imageFile.absolutePath}")

        val authority = "${context.packageName}.provider"
        Log.d("getTmpFileUri", "Using authority: $authority")

        return FileProvider.getUriForFile(context, authority, imageFile)

    } catch (e: IOException) { // More specific catch for file operations
        Log.e("getTmpFileUri", "IOException while preparing image file", e) // Log the full exception
        Toast.makeText(context, "IO Error preparing image file: ${e.message}", Toast.LENGTH_LONG).show()
        return null
    } catch (e: IllegalArgumentException) { // Specific for FileProvider.getUriForFile issues
        Log.e("getTmpFileUri", "IllegalArgumentException while getting URI (likely authority mismatch or bad file path setup)", e)
        Toast.makeText(context, "Config error for image file: ${e.message}", Toast.LENGTH_LONG).show()
        return null
    } catch (e: Exception) { // Catch-all for anything else
        Log.e("getTmpFileUri", "Generic error preparing image file", e) // Log the full exception
        Toast.makeText(context, "Error preparing image file: ${e.message}", Toast.LENGTH_LONG).show()
        return null
    }
}

@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    var prompt by remember { mutableStateOf(placeholderPrompt) }

    val uiState by bakingViewModel.uiState.collectAsState()

    var isProcessingPickerAction by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            isProcessingPickerAction = false
            if (success) {
                selectedImageUri = tempCameraImageUri
            } else {
                Toast.makeText(context, "Camera capture failed or cancelled.", Toast.LENGTH_SHORT)
                    .show()
                tempCameraImageUri?.let { uri ->
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (e: Exception) { /* log */
                    }
                }
            }
            tempCameraImageUri = null
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (isGranted) {
                val uriForCamera = getTmpFileUri(context)
                if (uriForCamera != null) {
                    tempCameraImageUri = uriForCamera
                    cameraLauncher.launch(uriForCamera)
                } else {
                    isProcessingPickerAction = false
                }
            } else {
                isProcessingPickerAction = false
                Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            isProcessingPickerAction = false
            if (uri != null) {
                selectedImageUri = uri
            } else {
                Toast.makeText(context, "No image selected from gallery.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    )


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.baking_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline)),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(data = selectedImageUri)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("select_image_placeholder")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    isProcessingPickerAction = true
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !isProcessingPickerAction && uiState !is UiState.Loading
            ) { Text("From gallery") }

            Button(
                onClick = {
                    isProcessingPickerAction = true
                    if (hasCameraPermission) {
                        val uriForCamera = getTmpFileUri(context)
                        if (uriForCamera != null) {
                            tempCameraImageUri = uriForCamera
                            cameraLauncher.launch(uriForCamera)
                        } else {
                            isProcessingPickerAction = false
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                enabled = !isProcessingPickerAction && uiState !is UiState.Loading
            ) {
                if (isProcessingPickerAction && !hasCameraPermission && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize))
                } else {
                    Text("from_camera")
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextField(
                value = prompt,
                label = { Text(stringResource(R.string.label_prompt)) },
                onValueChange = { prompt = it },
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        var bitmap: Bitmap? = null
                        if (selectedImageUri != null) {
                            try {
                                bitmap = withContext(Dispatchers.IO) {
                                    if (Build.VERSION.SDK_INT < 28) {
                                        @Suppress("DEPRECATION")
                                        MediaStore.Images.Media.getBitmap(
                                            context.contentResolver,
                                            selectedImageUri
                                        )
                                    } else {
                                        val source = ImageDecoder.createSource(
                                            context.contentResolver,
                                            selectedImageUri!!
                                        )
                                        ImageDecoder.decodeBitmap(source)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("BakingScreen", "Error converting URI to Bitmap", e)
                                Toast.makeText(
                                    context,
                                    "Failed to load image: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        if (bitmap != null) {
                            bakingViewModel.sendPrompt(bitmap, prompt)
                        }
                    }
                },
                enabled = prompt.isNotBlank() && uiState !is UiState.Loading,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(text = stringResource(R.string.action_go))
            }
        }

        when (val currentState = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            is UiState.Success -> {
                val scrollState = rememberScrollState()
                Text(
                    text = currentState.outputText,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                )
            }

            is UiState.Error -> {
                val scrollState = rememberScrollState()
                Text(
                    text = currentState.errorMessage,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                )
            }

            is UiState.Initial -> {
                Text(
                    text = stringResource(R.string.results_placeholder),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}


@Preview(showSystemUi = true)
@Composable
fun BakingScreenPreview() {
    BakingScreen()
}
