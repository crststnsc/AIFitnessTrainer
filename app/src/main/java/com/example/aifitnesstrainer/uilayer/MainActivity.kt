package com.example.aifitnesstrainer.uilayer

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.datalayer.models.Constants
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel
import com.example.aifitnesstrainer.datalayer.ml.Detector
import com.example.aifitnesstrainer.ui.theme.AIFitnessTrainerTheme
import com.example.aifitnesstrainer.uilayer.views.composable.CameraPreview
import com.example.aifitnesstrainer.uilayer.views.composable.FeedbackView
import com.example.aifitnesstrainer.uilayer.views.composable.InferenceTimeView
import com.example.aifitnesstrainer.uilayer.views.composable.MovementProgressBar
import com.example.aifitnesstrainer.uilayer.views.composable.MovementSwitcher
import com.example.aifitnesstrainer.uilayer.views.composable.OverlayViewComposable
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), Detector.DetectorListener, TextToSpeech.OnInitListener {
    private var isFrontCamera by mutableStateOf(true)
    private var detector: Detector? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: MainViewModel by viewModels()
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        textToSpeech = TextToSpeech(this, this)

        viewModel.registerSpeakCallback { text ->
            speak(text)
        }

        setContent {
            AIFitnessTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val results by viewModel.results.collectAsState()
                    val inferenceTime by viewModel.inferenceTime.collectAsState()
                    val jointAngles by viewModel.jointAngles.collectAsState()
                    val movementProgress by viewModel.movementProgress.collectAsState()

                    Column(modifier=Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            CameraPreview(
                                isFrontCamera = isFrontCamera,
                                switchCamera = { isFrontCamera = !isFrontCamera },
                                detector = detector,
                                executor = cameraExecutor
                            )
                            InferenceTimeView(inferenceTime = inferenceTime)
                            OverlayViewComposable(results = results, jointAngles = jointAngles)
                            FeedbackView(viewModel = viewModel)
                        }
                        MovementProgressBar(progress = movementProgress)
                        MovementSwitcher(viewModel)
                    }
                }
            }
        }
        cameraExecutor.execute {
            detector = Detector(baseContext, Constants.MODELS[0], this)
            detector?.setup()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
        textToSpeech.stop()
        textToSpeech.shutdown()
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

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}








