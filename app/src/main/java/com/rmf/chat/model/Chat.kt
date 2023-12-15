package com.rmf.chat.model

data class Chat(
    val username: String,
    val messageContent: String,
    val roomName: String,
    var messageType: MessageType
)