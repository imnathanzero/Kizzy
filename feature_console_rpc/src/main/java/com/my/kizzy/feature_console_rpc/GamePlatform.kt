package com.my.kizzy.feature_console_rpc

import com.my.kizzy.data.rpc.Constants
import com.my.kizzy.domain.model.Game
import com.my.kizzy.domain.model.rpc.RpcConfig

enum class GamePlatform(
    val title: String,
    val consoleName: String?,
    val smallIcon: String?,
    val smallText: String?
) {
    NONE(
        title = "None",
        consoleName = null,
        smallIcon = null,
        smallText = null
    ),
    PS5(
        title = "PlayStation 5",
        consoleName = "PlayStation®5",
        smallIcon = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/playstation.png",
        smallText = "PlayStation 5"
    ),
    PS4(
        title = "PlayStation 4",
        consoleName = "PlayStation®4",
        smallIcon = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/playstation.png",
        smallText = "PlayStation 4"
    ),
    SWITCH(
        title = "Switch",
        consoleName = "Nintendo Switch",
        smallIcon = Constants.NINTENDO_LINK,
        smallText = "Nintendo Switch"
    ),
    XBOX(
        title = "Xbox",
        consoleName = "Xbox",
        smallIcon = Constants.XBOX_LINK,
        smallText = "Xbox"
    ),
    PC(
        title = "PC / Steam",
        consoleName = "PC",
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
    val effectiveName = platform.consoleName ?: game.game_title
    val effectiveDetails = if (platform.consoleName != null) game.game_title else ""
    return RpcConfig(
        name = effectiveName,
        details = effectiveDetails,
        timestampsStart = System.currentTimeMillis().toString(),
        status = "dnd",
        largeImg = game.large_image ?: "",
        smallImg = platform.smallIcon ?: "",
        smallText = platform.smallText ?: "",
        type = "0",
        platform = ""
    )
}
