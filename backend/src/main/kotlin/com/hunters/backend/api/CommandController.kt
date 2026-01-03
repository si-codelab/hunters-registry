package com.hunters.backend.api

import com.hunters.backend.service.GameStateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/commands")
class CommandController(
    private val gameStateService: GameStateService
) {

    @PostMapping("/missions")
    fun startMission(@RequestBody req: StartMissionRequest): ResponseEntity<Any> {
        return try {
            val created = gameStateService.startMission(req)
            ResponseEntity.ok(created)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Bad request")))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(409).body(mapOf("error" to (e.message ?: "Conflict")))
        }
    }
}
