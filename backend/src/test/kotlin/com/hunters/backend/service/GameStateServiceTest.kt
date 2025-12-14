package com.hunters.backend.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameStateServiceTest {

    @Test
    fun `tick advances time and decays monster presence`() {
        // given
        val service = GameStateService()
        service.init() // manually trigger @PostConstruct logic

        val initialTime = service.getTime().minute
        val initialPresence = service.getPresences().first().presence

        // when
        service.tick(10)

        // then
        val newTime = service.getTime().minute
        val newPresence = service.getPresences().first().presence

        assertEquals(initialTime + 10, newTime)
        assertTrue(
            newPresence < initialPresence,
            "Expected presence to decrease after ticking time"
        )
    }
}