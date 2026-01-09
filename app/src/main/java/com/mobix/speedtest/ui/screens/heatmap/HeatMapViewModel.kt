package com.mobix.speedtest.ui.screens.heatmap

import android.content.Context
import androidx.lifecycle.ViewModel
import com.mobix.speedtest.domain.models.HeatMapPoint
import com.mobix.speedtest.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HeatMapViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _points = MutableStateFlow<List<HeatMapPoint>>(emptyList())
    val points = _points.asStateFlow()

    fun addPoint(x: Float, y: Float, z: Float) {
        val dbm = NetworkUtils.getCurrentSignalDbm(context)
        val newPoint = HeatMapPoint(x, y, z, dbm, NetworkUtils.getColorForSignal(dbm))
        _points.value = _points.value + newPoint
    }
}