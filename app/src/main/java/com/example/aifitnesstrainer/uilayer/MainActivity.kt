package com.example.aifitnesstrainer.uilayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.SpeechResponseFormat
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity(), Detector.DetectorListener, TextToSpeech.OnInitListener {
    private var isFrontCamera by mutableStateOf(true)
    private var detector: Detector? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: MainViewModel by viewModels()
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var openai: OpenAI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        textToSpeech = TextToSpeech(this, this)

        val token = ""

        openai = OpenAI(
            token = token,
            timeout = Timeout(socket = 5.seconds),
        )

        viewModel.registerSpeakCallback { text ->
            lifecycleScope.launch {
                run {
                    speak(text)
                }
            }
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
                    val feedback by viewModel.feedback.collectAsState()

                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            CameraPreview(
                                isFrontCamera = isFrontCamera,
                                switchCamera = { isFrontCamera = !isFrontCamera },
                                detector = detector,
                                executor = cameraExecutor
                            )
                            InferenceTimeView(inferenceTime = inferenceTime)
                            OverlayViewComposable(results = results, jointAngles = jointAngles)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            MovementProgressBar(progress = movementProgress)
                            FeedbackView(feedback = feedback)
                        }
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
        thread {
            viewModel.clearResults()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        thread {
            viewModel.updateResults(boundingBoxes, inferenceTime)
        }
    }

    private suspend fun speak(text: String) {
        try {
            val rawAudio = openai.speech(
                request = SpeechRequest(
                    model = ModelId("tts-1"),
                    input = text,
                    voice = com.aallam.openai.api.audio.Voice.Nova,
                    responseFormat = SpeechResponseFormat("wav")
                )
            )

            val audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(22050)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                rawAudio.size,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack.write(rawAudio, 0, rawAudio.size)
            audioTrack.play()

        } catch (e: Exception) {
            Log.e("LIFECYCLE OPENAI", e.toString())
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}









