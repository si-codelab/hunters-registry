package com.hunters.backend.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class GameClock(
    private val gameStateService: GameStateService
) {

    @Scheduled(fixedRate = 1000)
    fun advanceTime() {
        gameStateService.tick(1)
    }
}