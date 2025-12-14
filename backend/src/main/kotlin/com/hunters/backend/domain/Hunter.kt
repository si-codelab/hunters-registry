package com.hunters.backend.domain

data class Hunter(val id: String,
                  val name: String,
                  val skill: Int,
                  val status: HunterStatus)