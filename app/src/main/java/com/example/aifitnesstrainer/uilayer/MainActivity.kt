package com.example.aifitnesstrainer.uilayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.audio.SpeechRequest
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity(), Detector.DetectorListener, TextToSpeech.OnInitListener {
    private var isFrontCamera by mutableStateOf(true)
    private var detector: Detector? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: MainViewModel by viewModels()
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var openai: OpenAI
    private var useOpenAI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        textToSpeech = TextToSpeech(this, this)

        val token = System.getenv("OPENAI_API_KEY")

        if (token != null) {
            openai = OpenAI(
                token = token,
                timeout = Timeout(socket = 5.seconds),
            )
        }
        else{
            Toast.makeText(this, "API Key expired on non-existent", Toast.LENGTH_LONG).show()
            useOpenAI = false
        }

        viewModel.registerSpeakCallback { text ->
            lifecycleScope.launch {
                try {
                    speak(text)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("LIFECYCLE OPENAI", e.toString())
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
                            FeedbackView(feedback = feedback)
                            MovementStatusView(viewModel = viewModel)
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
                    voice = com.aallam.openai.api.audio.Voice.Alloy,
                )
            )

            val audioTrack = AudioTrack.Builder()
                .setBufferSizeInBytes(rawAudio.size)
                .build()

            audioTrack.write(rawAudio, 0, rawAudio.size)

            audioTrack.play()
        } catch (e: Exception) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

}

@Composable
fun MovementStatusView(viewModel: MainViewModel) {
    val movementStatus by viewModel.movementStatus.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = movementStatus,
            color = Color.Black,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.8f))
                .padding(8.dp)
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
                .shadow(4.dp, shape = RoundedCornerShape(8.dp))
        )
    }
}









