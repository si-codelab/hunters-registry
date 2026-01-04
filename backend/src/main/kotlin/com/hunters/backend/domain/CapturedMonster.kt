package com.hunters.backend.domain

data class CapturedMonster(
    val id: String,
    val type: MonsterType,
    val threat: Int,
    val capturedAtVersion: Long
)
