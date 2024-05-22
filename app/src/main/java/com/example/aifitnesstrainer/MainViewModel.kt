package com.example.aifitnesstrainer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<BoundingBox>>(emptyList())
    val results: StateFlow<List<BoundingBox>> = _results

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime

    fun updateResults(newResults: List<BoundingBox>, inferenceTime: Long) {
        _results.value = newResults
        _inferenceTime.value = inferenceTime
    }

    fun clearResults() {
        _results.value = emptyList()
    }
}
