package com.hunters.backend.api

import com.hunters.backend.domain.*

data class GameStateResponse(
    val time: GameTimeResponse,
    val map: GameMap,
    val hunters: List<Hunter>,
    val monsters: List<Monster>,
    val presences: List<MonsterPresence>,
    val missions: List<Mission>,
    val capturedMonsters: List<CapturedMonster>
)
