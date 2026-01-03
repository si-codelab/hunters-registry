package com.hunters.backend.domain

data class MonsterPresence(
    val monsterId: String,
    val presence: Double,
    val cell: Cell
)
