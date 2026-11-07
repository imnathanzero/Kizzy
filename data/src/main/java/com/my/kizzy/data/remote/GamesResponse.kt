/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * GamesResponse.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.data.remote


import com.my.kizzy.data.rpc.Constants
import com.my.kizzy.domain.model.Game
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GamesResponse(
    @SerialName("label")
    val label: String,
    @SerialName("link")
    val link: String? = null,
    @SerialName("title")
    val title: String
)

fun GamesResponse.toGame() : Game {
    val platformName = when (label.lowercase()) {
        "nintendo" -> Constants.NINTENDO
        "wii" -> Constants.WII_U
        "nintendo-3ds" -> Constants.NINTENDO_3DS
        "xbox" -> Constants.XBOX
        else -> ""
    }
    val smallImg = when (label.lowercase()) {
        "nintendo" -> Constants.NINTENDO_LINK
        "wii" -> Constants.WII_U_LINK
        "nintendo-3ds" -> Constants.N3DS_LINK
        "xbox" -> Constants.XBOX_LINK
        else -> ""
    }
    return Game(
        platform = platformName,
        small_image = smallImg,
        large_image = link,
        game_title = title
    )
}