package com.example.aifitnesstrainer.uilayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.SpeechResponseFormat
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.aifitnesstrainer.datalayer.ml.Detector
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import com.example.aifitnesstrainer.datalayer.models.Constants
import com.example.aifitnesstrainer.ui.theme.AIFitnessTrainerTheme
import com.example.aifitnesstrainer.uilayer.viewmodels.MainViewModel
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
    private val token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        textToSpeech = TextToSpeech(this, this)

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
        thread{
            viewModel.clearResults()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        thread{
            viewModel.updateResults(boundingBoxes, inferenceTime)
        }
    }

    private suspend fun speak(text: String) {
        val isConnected = checkForInternet(this)

        if(!isConnected || token.isEmpty()){
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }

        var rawAudio = openai.speech(
            request = SpeechRequest(
                model = ModelId("tts-1"),
                input = text,
                voice = com.aallam.openai.api.audio.Voice.Nova,
                responseFormat = SpeechResponseFormat("wav")
            )
        )

        rawAudio = rawAudio.slice(44..<rawAudio.size).toByteArray()

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

        CoroutineScope(Dispatchers.Default).launch {
            audioTrack.write(rawAudio, 0, rawAudio.size)
            audioTrack.play()

            while (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                delay(100)
            }

            audioTrack.release()
        }
    }

    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false

        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}









