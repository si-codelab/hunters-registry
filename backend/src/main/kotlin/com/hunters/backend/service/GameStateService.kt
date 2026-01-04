package com.hunters.backend.service

import com.hunters.backend.api.GameStateResponse
import com.hunters.backend.api.GameTimeResponse
import com.hunters.backend.api.StartMissionRequest
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

    private val SCOUT_DURATION_MINUTES = 30L
    private val OBSERVE_DURATION_MINUTES = 5L
    private val CAPTURE_DURATION_MINUTES = 5L
    private val SCOUT_RADIUS = 1

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
            updateMissions()
            removeExpiredMonsters()
            buildStateResponse()
        }

        broadcastState(stateToBroadcast)
    }

    private fun updateMissions() {
        for (i in missions.indices) {
            val m = missions[i]
            if (m.status != MissionStatus.IN_PROGRESS) continue

            val elapsed = gameMinute - m.startedAtMinute
            val duration = durationFor(m.type)

            if (elapsed < duration) continue

            when (m.type) {
                MissionType.SCOUT -> {
                    missions[i] = m.copy(status = MissionStatus.SUCCEEDED)
                    setHunterIdle(m.hunterId)
                }

                MissionType.OBSERVE -> {
                    missions[i] = m.copy(status = MissionStatus.SUCCEEDED)
                    setHunterIdle(m.hunterId)
                }

                MissionType.CAPTURE -> {
                    val success = resolveCapture(m)
                    missions[i] = m.copy(status = if (success) MissionStatus.SUCCEEDED else MissionStatus.FAILED)
                    setHunterIdle(m.hunterId)
                }
            }
        }
    }


    private fun setHunterIdle(hunterId: String) {
        val idx = hunters.indexOfFirst { it.id == hunterId }
        if (idx == -1) return
        val h = hunters[idx]
        hunters[idx] = h.copy(status = HunterStatus.IDLE)
    }


    private fun isWithinScoutRadius(hunterCell: Cell, presenceCell: Cell): Boolean {
        val dx = kotlin.math.abs(hunterCell.x - presenceCell.x)
        val dy = kotlin.math.abs(hunterCell.y - presenceCell.y)
        return dx <= SCOUT_RADIUS && dy <= SCOUT_RADIUS
    }

    private fun visiblePresences(): List<MonsterPresence> {
        val huntersById = hunters.associateBy { it.id }

        val scoutHunters = missions
            .asSequence()
            .filter { it.status == MissionStatus.IN_PROGRESS && it.type == MissionType.SCOUT }
            .mapNotNull { huntersById[it.hunterId] }
            .toList()

        if (scoutHunters.isEmpty()) return emptyList()

        return presences.filter { presence ->
            scoutHunters.any { hunter -> isWithinScoutRadius(hunter.cell, presence.cell) }
        }
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
        val visible = visiblePresences()

        return GameStateResponse(
            time = buildTimeResponse(),
            map = map,
            hunters = hunters.toList(),
            monsters = monsters.toList(),
            presences = visible,
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
            cell = Cell(x = 2, y = 2)
        )
    }

    private fun seedMissions() {
        missions += Mission(
            id = UUID.randomUUID().toString(),
            type = MissionType.SCOUT,
            hunterId = "hunter-1",
            monsterId = "monster-1", // unused for SCOUT right now, but keep it for now
            status = MissionStatus.IN_PROGRESS,
            startedAtMinute = gameMinute
        )
    }

    fun startMission(req: StartMissionRequest): Mission {
        val (created, stateToBroadcast) = withLock {
            val hunterIndex = hunters.indexOfFirst { it.id == req.hunterId }
            if (hunterIndex == -1) throw IllegalArgumentException("Hunter not found: ${req.hunterId}")

            val hunter = hunters[hunterIndex]
            if (hunter.status != HunterStatus.IDLE) {
                throw IllegalStateException("Hunter is not idle: ${req.hunterId}")
            }

            when (req.type) {
                MissionType.SCOUT -> {
                    val cell = req.targetCell ?: throw IllegalArgumentException("SCOUT requires targetCell")
                    requireCellInBounds(cell)

                    // For now: move the hunter instantly to the target cell
                    hunters[hunterIndex] = hunter.copy(
                        status = HunterStatus.ON_MISSION,
                        cell = cell
                    )
                }

                MissionType.OBSERVE, MissionType.CAPTURE -> {
                    val monsterId = req.monsterId ?: throw IllegalArgumentException("${req.type} requires monsterId")
                    requireMonsterExists(monsterId)

                    // Require it to currently be revealed
                    if (!isMonsterRevealed(monsterId)) {
                        throw IllegalStateException("Monster is not revealed: $monsterId")
                    }

                    hunters[hunterIndex] = hunter.copy(status = HunterStatus.ON_MISSION)
                }
            }

            val mission = Mission(
                id = UUID.randomUUID().toString(),
                type = req.type,
                hunterId = req.hunterId,
                monsterId = req.monsterId ?: "",
                status = MissionStatus.IN_PROGRESS,
                startedAtMinute = gameMinute
            )

            missions += mission

            mission to buildStateResponse()
        }

        broadcastState(stateToBroadcast)
        return created
    }


    private fun requireCellInBounds(cell: Cell) {
        if (cell.x !in 0 until map.width || cell.y !in 0 until map.height) {
            throw IllegalArgumentException("Cell out of bounds: ${cell.x},${cell.y}")
        }
    }

    private fun requireMonsterExists(monsterId: String) {
        if (monsters.none { it.id == monsterId }) {
            throw IllegalArgumentException("Monster not found: $monsterId")
        }
    }

    private fun isMonsterRevealed(monsterId: String): Boolean {
        // presence is the real "instance" that is revealed
        return visiblePresences().any { it.monsterId == monsterId }
    }

    private fun resolveCapture(mission: Mission): Boolean {
        val hunter = hunters.firstOrNull { it.id == mission.hunterId } ?: return false
        val monster = monsters.firstOrNull { it.id == mission.monsterId } ?: return false
        val presence = presences.firstOrNull { it.monsterId == mission.monsterId } ?: return false

        // Base chance uses skill and threat. Presence can also influence chance.
        // Keep this simple, tune later.
        val base = 0.45
        val skillBonus = 0.10 * hunter.skill
        val threatPenalty = 0.08 * monster.threat
        val presenceBonus = 0.20 * presence.presence

        val chance = (base + skillBonus + presenceBonus - threatPenalty).coerceIn(0.10, 0.90)

        val roll = kotlin.random.Random.nextDouble()
        val success = roll < chance

        if (success) {
            // Successful capture removes presence and monster from the world
            presences.removeIf { it.monsterId == monster.id }
            monsters.removeIf { it.id == monster.id }
        } else {
            // Optional small consequence so failure matters
            // Example: presence drops a bit (monster becomes harder to find later)
            val idx = presences.indexOfFirst { it.monsterId == monster.id }
            if (idx != -1) {
                val p = presences[idx]
                presences[idx] = p.copy(presence = round2((p.presence - 0.10).coerceAtLeast(0.0)))
            }
        }

        return success
    }

    private fun durationFor(type: MissionType): Long = when (type) {
        MissionType.SCOUT -> SCOUT_DURATION_MINUTES
        MissionType.OBSERVE -> OBSERVE_DURATION_MINUTES
        MissionType.CAPTURE -> CAPTURE_DURATION_MINUTES
    }


}
