package com.hunters.backend.api

data class GameTimeResponse(
    val version: Long,
    val minute: Int,
    val day: Long,
    val hour: Int
)