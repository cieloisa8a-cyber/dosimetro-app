package com.example.dosimetrov2

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DosimeterViewModel : ViewModel() {
    private val _history = MutableStateFlow<List<Float>>(emptyList())
    val history: StateFlow<List<Float>> = _history.asStateFlow()

    private val _measurementName = MutableStateFlow("")
    val measurementName: StateFlow<String> = _measurementName.asStateFlow()

    fun addMeasurement(db: Float) {
        _history.update { current ->
            val updated = current.toMutableList()
            updated.add(db)
            if (updated.size > 400) updated.removeAt(0)
            updated
        }
    }

    fun setMeasurementName(name: String) {
        _measurementName.value = name
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
