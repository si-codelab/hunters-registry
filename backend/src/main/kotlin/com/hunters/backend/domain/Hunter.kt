package com.hunters.backend.domain

data class Hunter(
    val id: String,
    val name: String,
    val skill: Int,
    val energy: Int,
    val status: HunterStatus,
    val activity: HunterActivity,
    val cell: Cell
)
