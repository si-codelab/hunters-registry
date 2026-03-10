package com.hunters.backend.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `idle hunters recover energy and mission hunters lose energy`() {
        val service = GameStateService()
        service.init()

        service.startMission(
            com.hunters.backend.api.StartMissionRequest(
                type = com.hunters.backend.domain.MissionType.SCOUT,
                hunterId = "hunter-1",
                targetCell = com.hunters.backend.domain.Cell(x = 2, y = 2)
            )
        )

        service.tick(10)

        val hunters = service.getHunters().associateBy { it.id }
        assertEquals(70, hunters.getValue("hunter-1").energy)
        assertEquals(100, hunters.getValue("hunter-2").energy)
    }

    @Test
    fun `hunter energy is clamped between zero and one hundred`() {
        val service = GameStateService()
        service.init()

        service.startMission(
            com.hunters.backend.api.StartMissionRequest(
                type = com.hunters.backend.domain.MissionType.SCOUT,
                hunterId = "hunter-1",
                targetCell = com.hunters.backend.domain.Cell(x = 2, y = 2)
            )
        )

        service.tick(40)

        val drainedHunter = service.getHunters().first { it.id == "hunter-1" }
        val restedHunter = service.getHunters().first { it.id == "hunter-2" }

        assertEquals(0, drainedHunter.energy)
        assertEquals(100, restedHunter.energy)
    }

    @Test
    fun `scouting is a persistent assignment and does not create a mission`() {
        val service = GameStateService()
        service.init()

        service.startMission(
            com.hunters.backend.api.StartMissionRequest(
                type = com.hunters.backend.domain.MissionType.SCOUT,
                hunterId = "hunter-1",
                targetCell = com.hunters.backend.domain.Cell(x = 2, y = 2)
            )
        )

        val hunter = service.getHunters().first { it.id == "hunter-1" }

        assertEquals(com.hunters.backend.domain.HunterStatus.IDLE, hunter.status)
        assertEquals(com.hunters.backend.domain.HunterActivity.SCOUTING, hunter.activity)
        assertTrue(service.getMissions().isEmpty())
        assertFalse(service.snapshot().presences.isEmpty())
    }

    @Test
    fun `observe can be started by the same hunter that is scouting`() {
        val service = GameStateService()
        service.init()

        service.startMission(
            com.hunters.backend.api.StartMissionRequest(
                type = com.hunters.backend.domain.MissionType.SCOUT,
                hunterId = "hunter-1",
                targetCell = com.hunters.backend.domain.Cell(x = 2, y = 2)
            )
        )

        service.startMission(
            com.hunters.backend.api.StartMissionRequest(
                type = com.hunters.backend.domain.MissionType.OBSERVE,
                hunterId = "hunter-1",
                monsterId = "monster-1"
            )
        )

        val hunter = service.getHunters().first { it.id == "hunter-1" }
        val presenceCell = service.getPresences().first { it.monsterId == "monster-1" }.cell

        assertEquals(com.hunters.backend.domain.HunterStatus.ON_MISSION, hunter.status)
        assertEquals(com.hunters.backend.domain.HunterActivity.IDLE, hunter.activity)
        assertEquals(presenceCell, hunter.cell)
        assertEquals(1, service.getMissions().size)
    }

    @Test
    fun `scouting hunter can be recalled to idle`() {
        val service = GameStateService()
        service.init()

        service.startMission(
            com.hunters.backend.api.StartMissionRequest(
                type = com.hunters.backend.domain.MissionType.SCOUT,
                hunterId = "hunter-1",
                targetCell = com.hunters.backend.domain.Cell(x = 2, y = 2)
            )
        )

        service.stopScouting("hunter-1")

        val hunter = service.getHunters().first { it.id == "hunter-1" }
        assertEquals(com.hunters.backend.domain.HunterStatus.IDLE, hunter.status)
        assertEquals(com.hunters.backend.domain.HunterActivity.IDLE, hunter.activity)
        assertTrue(service.snapshot().presences.isEmpty())
    }
}
