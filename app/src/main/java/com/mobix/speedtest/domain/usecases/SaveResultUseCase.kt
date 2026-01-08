package com.mobix.speedtest.domain.usecases

import com.mobix.speedtest.domain.models.SpeedResult
import com.mobix.speedtest.domain.repository.SpeedTestRepository

class SaveResultUseCase(private val repository: SpeedTestRepository) {
    suspend operator fun invoke(result: SpeedResult) {
        repository.saveResult(result)
    }
}