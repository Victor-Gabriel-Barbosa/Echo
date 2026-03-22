package com.example.mapa.data.mapper

import com.example.mapa.data.local.entity.ChatEntity
import com.example.mapa.data.local.entity.ChatLastMsg
import com.example.mapa.data.remote.dto.ChatDTO

fun ChatDTO.toEntity(): ChatEntity {
    return ChatEntity(
        id = this.id,
        lastTimestamp = this.lastTimestamp,
        participants = "|" + this.participants.joinToString(separator = "|") + "|",
        visibleTo = "|" + this.visibleTo.joinToString(separator = "|") + "|",
        lastMsgId = this.lastMsg?.id,
        locationId = this.locationId
    )
}

fun ChatLastMsg.toDTO(): ChatDTO {
    return ChatDTO(
        id = this.chat.id,
        lastTimestamp = this.chat.lastTimestamp,
        participants = this.chat.participants.split("|").filter { it.isNotBlank() },
        visibleTo = this.chat.visibleTo.split("|").filter { it.isNotBlank() },
        lastMsg = this.lastMsg?.toDTO(),
        locationId = this.chat.locationId
    )
}