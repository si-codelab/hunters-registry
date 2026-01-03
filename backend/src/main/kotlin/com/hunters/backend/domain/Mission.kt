package com.hunters.backend.domain

data class Mission(
    val id: String,
    val type: MissionType,
    val hunterId: String,
    val monsterId: String,
    val status: MissionStatus,
    val startedAtMinute: Long
)
