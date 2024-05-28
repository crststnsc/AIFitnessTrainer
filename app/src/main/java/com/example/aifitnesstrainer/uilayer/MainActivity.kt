package com.example.aifitnesstrainer.uilayer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.datalayer.models.Constants
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel
import com.example.aifitnesstrainer.datalayer.ml.Detector
import com.example.aifitnesstrainer.ui.theme.AIFitnessTrainerTheme
import com.example.aifitnesstrainer.uilayer.views.OverlayViewComposable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), Detector.DetectorListener {
    private var isFrontCamera by mutableStateOf(true)
    private var detector: Detector? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            AIFitnessTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val results by viewModel.results.collectAsState()
                    val inferenceTime by viewModel.inferenceTime.collectAsState()
                    val jointAngles by viewModel.jointAngles.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraPreview(
                            isFrontCamera = isFrontCamera,
                            switchCamera = { isFrontCamera = !isFrontCamera },
                            detector = detector,
                            executor = cameraExecutor
                        )
                        InferenceTimeView(inferenceTime = inferenceTime)
                        OverlayViewComposable(results = results, jointAngles = jointAngles)
                    }

                }
            }
        }
        cameraExecutor.execute {
            detector = Detector(baseContext, Constants.MODELS[0], this)
            detector?.setup()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            viewModel.clearResults()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            viewModel.updateResults(boundingBoxes, inferenceTime)
        }
    }
}

@Composable
fun CameraPreview(
    isFrontCamera: Boolean,
    switchCamera: () -> Unit,
    detector: Detector?,
    executor: ExecutorService,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.aspectRatio(3f/4f).fillMaxSize()) {
            CameraView(
                isFrontCamera = isFrontCamera,
                lifecycleOwner = lifecycleOwner,
                detector = detector,
                executor = executor,
            )
            IconButton(
                onClick = switchCamera,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Switch Camera"
                )
            }
        }
    } else {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasCameraPermission = granted
        }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun CameraView(
    isFrontCamera: Boolean,
    lifecycleOwner: LifecycleOwner,
    detector: Detector?,
    executor: ExecutorService,
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(isFrontCamera) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer.setAnalyzer(executor) { imageProxy ->
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
            )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
            )

            detector?.detect(rotatedBitmap)
            imageProxy.close()
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e("Camera", "Use case binding failed", exc)
        }
    }

    AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
fun InferenceTimeView(inferenceTime: Long) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "$inferenceTime ms",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}


