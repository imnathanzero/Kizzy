package com.my.kizzy.feature_console_rpc

import com.my.kizzy.data.rpc.Constants
import com.my.kizzy.domain.model.Game
import com.my.kizzy.domain.model.rpc.RpcConfig

enum class GamePlatform(
    val title: String,
    val smallIcon: String?,
    val smallText: String?
) {
    NONE(
        title = "None",
        smallIcon = null,
        smallText = null
    ),
    PLAYSTATION(
        title = "PlayStation",
        smallIcon = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/playstation.png",
        smallText = "PlayStation"
    ),
    SWITCH(
        title = "Switch",
        smallIcon = Constants.NINTENDO_LINK,
        smallText = "Nintendo Switch"
    ),
    XBOX(
        title = "Xbox",
        smallIcon = Constants.XBOX_LINK,
        smallText = "Xbox"
    ),
    PC(
        title = "PC / Steam",
        smallIcon = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/steam.png",
        smallText = "PC / Steam"
    );

    companion object {
        fun fromSavedName(name: String): GamePlatform {
            return entries.find { it.name == name } ?: NONE
        }
    }
}

fun buildGameRpcConfig(game: Game, platform: GamePlatform): RpcConfig {
    return RpcConfig(
        name = game.game_title,
        details = "",
        timestampsStart = System.currentTimeMillis().toString(),
        status = "dnd",
        largeImg = game.large_image ?: "",
        smallImg = platform.smallIcon ?: "",
        smallText = platform.smallText ?: "",
        type = "0",
        platform = ""
    )
}
