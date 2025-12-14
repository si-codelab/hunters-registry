package com.hunters.backend.api

import com.hunters.backend.service.GameStateService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class GameStateController(
    private val gameStateService: GameStateService
) {

    @GetMapping("/state")
    fun getState(): GameStateResponse =
        GameStateResponse(
            time = gameStateService.getTime(),
            hunters = gameStateService.getHunters(),
            monsters = gameStateService.getMonsters(),
            presences = gameStateService.getPresences(),
            missions = gameStateService.getMissions()
        )
}
