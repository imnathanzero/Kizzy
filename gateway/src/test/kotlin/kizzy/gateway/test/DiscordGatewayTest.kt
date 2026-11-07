/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * DiscordGatewayTest.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package kizzy.gateway.test

import kizzy.gateway.entities.OutgoingPayload
import kizzy.gateway.entities.op.OpCode
import kizzy.gateway.entities.presence.Activity
import kizzy.gateway.entities.presence.Assets
import kizzy.gateway.entities.presence.Presence
import kizzy.gateway.entities.presence.Timestamps
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class DiscordGatewayTest {
    @Test
    fun `Check Presence Serialization`() {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
        val presence = Presence(
            activities = listOf(
                Activity(
                    name = "Persona 5 Royal",
                    state = null,
                    details = "Playing",
                    party = null,
                    type = 0,
                    timestamps = Timestamps(
                        start = 1731000000000L,
                        end = null
                    ),
                    assets = Assets(
                        largeImage = "mp:external/BSuXceWHRZF8lsujRGgQIBv8zzhR7mSeUteRjc69AOM/https/assets1.ignimgs.com/2020/02/14/persona-5-royal---button-fin-1581716582492.jpg",
                        smallImage = "mp:attachments/1009061802593763398/1049675792952598599/1670332610733.png",
                        largeText = null,
                        smallText = "Xbox"
                    ),
                    buttons = null,
                    metadata = null,
                    applicationId = "962990036020756480",
                    url = null
                )
            ),
            afk = true,
            since = 0L,
            status = "dnd"
        )
        val payload = json.encodeToString(OutgoingPayload(op = OpCode.PRESENCE_UPDATE, d = presence))
        assert(payload.contains("\"since\":0"))
        assert(payload.contains("\"status\":\"dnd\""))
        assert(payload.contains("\"afk\":true"))
        assert(!payload.contains("\"buttons\":null"))
        assert(!payload.contains("\"party\":null"))
        println("SERIALIZED PRESENCE: $payload")
    }
}