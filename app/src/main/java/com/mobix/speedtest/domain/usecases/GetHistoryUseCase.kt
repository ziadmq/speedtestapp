package com.mobix.speedtest.domain.usecases

import com.mobix.speedtest.domain.repository.SpeedTestRepository

class GetHistoryUseCase(private val repository: SpeedTestRepository) {
    operator fun invoke() = repository.getHistory()
}