package com.example.mapa.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Classe agregadora para buscar o Chat e sua última mensagem correspondente.
 */
data class ChatWithLastMsg(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "lastMsgId",
        entityColumn = "id"
    )
    val lastMsg: MsgEntity?
)