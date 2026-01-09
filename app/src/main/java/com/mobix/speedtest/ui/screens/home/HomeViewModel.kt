package com.mobix.speedtest.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobix.speedtest.domain.models.SpeedResult
import com.mobix.speedtest.domain.repository.SpeedTestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SpeedTestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SpeedResult?>(null)
    val uiState = _uiState.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    fun startTest() {
        viewModelScope.launch {
            _isTesting.value = true
            repository.startSpeedTest().collect { result ->
                _uiState.value = result
            }
            _isTesting.value = false
            // تم حذف حفظ النتيجة هنا لأنك لا تريد السجل
        }
    }
}