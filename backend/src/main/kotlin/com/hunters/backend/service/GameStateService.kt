package com.hunters.backend.service

import com.hunters.backend.domain.*
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GameStateService {

    private val hunters = mutableListOf<Hunter>()
    private val monsters = mutableListOf<Monster>()
    private val presences = mutableListOf<MonsterPresence>()
    private val missions = mutableListOf<Mission>()

    @PostConstruct
    fun init() {
        seedHunters()
        seedMonsters()
        seedMissions()
    }
    fun getHunters(): List<Hunter> = hunters.toList()
    fun getMonsters(): List<Monster> = monsters.toList()
    fun getPresences(): List<MonsterPresence> = presences.toList()
    fun getMissions(): List<Mission> = missions.toList()

    private fun seedHunters() {
        hunters += Hunter(
            id = "hunter-1",
            name = "Edric the Grey",
            skill = 3,
            status = HunterStatus.IDLE
        )

        hunters += Hunter(
            id = "hunter-2",
            name = "Mara Blackthorn",
            skill = 4,
            status = HunterStatus.IDLE
        )
    }

    private fun seedMonsters() {
        val monster = Monster(
            id = "monster-1",
            type = MonsterType.WRAITH,
            threat = 3
        )

        monsters += monster

        presences += MonsterPresence(
            monsterId = monster.id,
            presence = 0.8
        )
    }

    private fun seedMissions() {
        missions += Mission(
            id = UUID.randomUUID().toString(),
            type = MissionType.OBSERVE,
            hunterId = "hunter-1",
            monsterId = "monster-1",
            status = MissionStatus.IN_PROGRESS
        )
    }
}
