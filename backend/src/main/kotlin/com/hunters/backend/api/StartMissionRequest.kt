package com.hunters.backend.api

import com.hunters.backend.domain.Cell
import com.hunters.backend.domain.MissionType

data class StartMissionRequest(
    val type: MissionType,
    val hunterId: String,
    val targetCell: Cell? = null,
    val monsterId: String? = null
)
