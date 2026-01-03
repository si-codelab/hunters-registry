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
import kotlin.math.round

@Service
class GameStateService {

    private val emitters = mutableSetOf<SseEmitter>()
    private val lock = Any()

    private var gameMinute: Long = 0
    private val map = GameMap(width = 6, height = 6)
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

    fun getHunters(): List<Hunter> = withLock { hunters.toList() }
    fun getMonsters(): List<Monster> = withLock { monsters.toList() }
    fun getPresences(): List<MonsterPresence> = withLock { presences.toList() }
    fun getMissions(): List<Mission> = withLock { missions.toList() }

    fun getTime(): GameTimeResponse = withLock { buildTimeResponse() }

    fun snapshot(): GameStateResponse = withLock { buildStateResponse() }

    fun tick(minutes: Long = 1) {
        val stateToBroadcast = withLock {
            advanceTime(minutes)
            decayPresences(minutes)
            removeExpiredMonsters()
            buildStateResponse()
        }

        broadcastState(stateToBroadcast)
    }

    fun registerStateStream(): SseEmitter {
        val emitter = SseEmitter(Duration.ofMinutes(30).toMillis())

        withLock { emitters += emitter }

        emitter.onCompletion { withLock { emitters.remove(emitter) } }
        emitter.onTimeout { withLock { emitters.remove(emitter) } }
        emitter.onError { withLock { emitters.remove(emitter) } }

        val initialState = snapshot()
        if (!sendToEmitter(emitter, initialState)) {
            withLock { emitters.remove(emitter) }
        }

        return emitter
    }

    private fun broadcastState(state: GameStateResponse) {
        val currentEmitters = withLock { emitters.toList() }

        val dead = mutableListOf<SseEmitter>()
        for (emitter in currentEmitters) {
            if (!sendToEmitter(emitter, state)) {
                dead += emitter
            }
        }

        if (dead.isNotEmpty()) {
            withLock { emitters.removeAll(dead) }
        }
    }

    private fun sendToEmitter(emitter: SseEmitter, state: GameStateResponse): Boolean {
        return try {
            emitter.send(SseEmitter.event().name("state").data(state))
            true
        } catch (_: IOException) {
            try { emitter.complete() } catch (_: Exception) {}
            false
        } catch (_: IllegalStateException) {
            // Can happen if emitter is already completed
            try { emitter.complete() } catch (_: Exception) {}
            false
        } catch (_: Exception) {
            try { emitter.complete() } catch (_: Exception) {}
            false
        }
    }

    private fun advanceTime(minutes: Long) {
        gameMinute += minutes
    }

    private fun decayPresences(minutes: Long) {
        val decayPerMinute = 0.01
        for (i in presences.indices) {
            val p = presences[i]
            val raw = (p.presence - decayPerMinute * minutes).coerceAtLeast(0.0)
            presences[i] = p.copy(presence = round2(raw))
        }
    }

    private fun removeExpiredMonsters() {
        val expiredMonsterIds = presences
            .asSequence()
            .filter { it.presence <= 0.0 }
            .map { it.monsterId }
            .toSet()

        if (expiredMonsterIds.isEmpty()) return

        presences.removeIf { it.monsterId in expiredMonsterIds }
        monsters.removeIf { it.id in expiredMonsterIds }
    }

    private fun buildStateResponse(): GameStateResponse {
        return GameStateResponse(
            time = buildTimeResponse(),
            map = map,
            hunters = hunters.toList(),
            monsters = monsters.toList(),
            presences = presences.toList(),
            missions = missions.toList()
        )
    }

    private fun buildTimeResponse(): GameTimeResponse {
        val day = (gameMinute / 1440) + 1
        val hour = ((gameMinute % 1440) / 60).toInt()
        val minute = (gameMinute % 60).toInt()
        return GameTimeResponse(
            version = gameMinute,
            day = day,
            hour = hour,
            minute = minute
        )
    }

    private fun round2(value: Double): Double = round(value * 100) / 100

    private fun <T> withLock(block: () -> T): T = synchronized(lock, block)

    private fun seedHunters() {
        hunters += Hunter(
            id = "hunter-1",
            name = "Edric the Grey",
            skill = 3,
            status = HunterStatus.IDLE,
            cell = Cell(x = 1, y = 1)
        )

        hunters += Hunter(
            id = "hunter-2",
            name = "Mara Blackthorn",
            skill = 4,
            status = HunterStatus.IDLE,
            cell = Cell(x = 4, y = 2)
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
            presence = 0.8,
            cell = Cell(x = 2, y = 4)
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
