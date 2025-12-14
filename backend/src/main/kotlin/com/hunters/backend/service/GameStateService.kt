package com.hunters.backend.service

import com.hunters.backend.api.GameTimeResponse
import com.hunters.backend.domain.*
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GameStateService {

    private val lock = Any()
    private var gameMinute: Long = 0
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

    fun getTime(): GameTimeResponse = synchronized(lock) {
        val day = (gameMinute / 1440) + 1
        val hour = ((gameMinute % 1440) / 60).toInt()
        GameTimeResponse(minute = gameMinute, day = day, hour = hour)
    }

    fun tick(minutes: Long = 1) = synchronized(lock) {
        gameMinute += minutes

        val decayPerMinute = 0.01
        for (i in presences.indices) {
            val p = presences[i]
            val newPresence = (p.presence - decayPerMinute * minutes).coerceAtLeast(0.0)
            presences[i] = p.copy(presence = newPresence)
        }

        val expiredIds = presences.filter { it.presence <= 0.0 }.map { it.monsterId }.toSet()
        if (expiredIds.isNotEmpty()) {
            presences.removeIf { it.monsterId in expiredIds }
            monsters.removeIf { it.id in expiredIds }
        }
    }
}
