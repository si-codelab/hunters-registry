package com.hunters.backend.service

import com.hunters.backend.api.GameStateResponse
import com.hunters.backend.api.GameTimeResponse
import com.hunters.backend.domain.*
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Duration
import java.util.UUID

@Service
class GameStateService {

    private val emitters = mutableSetOf<SseEmitter>()
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

    fun getHunters(): List<Hunter> = synchronized(lock) { hunters.toList() }
    fun getMonsters(): List<Monster> = synchronized(lock) { monsters.toList() }
    fun getPresences(): List<MonsterPresence> = synchronized(lock) { presences.toList() }
    fun getMissions(): List<Mission> = synchronized(lock) { missions.toList() }

    private fun seedHunters() {
        hunters += Hunter(
            id = "hunter-1", name = "Edric the Grey", skill = 3, status = HunterStatus.IDLE
        )

        hunters += Hunter(
            id = "hunter-2", name = "Mara Blackthorn", skill = 4, status = HunterStatus.IDLE
        )
    }

    private fun seedMonsters() {
        val monster = Monster(
            id = "monster-1", type = MonsterType.WRAITH, threat = 3
        )

        monsters += monster

        presences += MonsterPresence(
            monsterId = monster.id, presence = 0.8
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

    fun tick(minutes: Long = 1) {
        synchronized(lock) {
            gameMinute += minutes

            val decayPerMinute = 0.01
            for (i in presences.indices) {
                val p = presences[i]
                val raw = (p.presence - decayPerMinute * minutes).coerceAtLeast(0.0)
                val newPresence = kotlin.math.round(raw * 100) / 100
                presences[i] = p.copy(presence = newPresence)
            }

            val expiredIds = presences.filter { it.presence <= 0.0 }.map { it.monsterId }.toSet()
            if (expiredIds.isNotEmpty()) {
                presences.removeIf { it.monsterId in expiredIds }
                monsters.removeIf { it.id in expiredIds }
            }
        }

        broadcastState()
    }


    fun registerStateStream(): SseEmitter {
        val emitter = SseEmitter(Duration.ofMinutes(30).toMillis())

        synchronized(lock) {
            emitters += emitter
        }

        emitter.onCompletion { synchronized(lock) { emitters.remove(emitter) } }
        emitter.onTimeout { synchronized(lock) { emitters.remove(emitter) } }
        emitter.onError { synchronized(lock) { emitters.remove(emitter) } }

        val initial = snapshot()
        try {
            emitter.send(SseEmitter.event().name("state").data(initial))
        }
         catch (ex: IOException) {
            synchronized(lock) { emitters.remove(emitter) }
        }

        return emitter
    }

    fun snapshot(): GameStateResponse = synchronized(lock) {
        GameStateResponse(
            time = GameTimeResponse(
                minute = gameMinute, day = (gameMinute / 1440) + 1, hour = ((gameMinute % 1440) / 60).toInt()
            ),
            hunters = hunters.toList(),
            monsters = monsters.toList(),
            presences = presences.toList(),
            missions = missions.toList()
        )
    }

    private fun broadcastState() {
        val state: GameStateResponse
        val currentEmitters: List<SseEmitter>

        synchronized(lock) {
            state = GameStateResponse(
                time = GameTimeResponse(
                    minute = gameMinute,
                    day = (gameMinute / 1440) + 1,
                    hour = ((gameMinute % 1440) / 60).toInt()
                ),
                hunters = hunters.toList(),
                monsters = monsters.toList(),
                presences = presences.toList(),
                missions = missions.toList()
            )
            currentEmitters = emitters.toList()
        }

        val dead = mutableListOf<SseEmitter>()
        for (emitter in currentEmitters) {
            try {
                emitter.send(SseEmitter.event().name("state").data(state))
            } catch (ex: IOException) {
                dead += emitter
            }
        }

        if (dead.isNotEmpty()) {
            synchronized(lock) {
                emitters.removeAll(dead)
            }
        }
    }


}
