package com.rmf.chat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.rmf.chat.model.Chat
import com.rmf.chat.model.MessageType
import com.rmf.chat.model.User
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private lateinit var socket: Socket
    private val gson: Gson = Gson()


    private val username = "User-${(0..200).random()}"
    private val roomName = "R-01"

    private val _chatList = MutableStateFlow<List<Chat>>(emptyList())
    val chatList = _chatList.asStateFlow()

    var message by mutableStateOf("")
    var userIsTyping by mutableStateOf("")

    private lateinit var jobTyping: Job

    fun initialize() {

        try {
            socket = IO.socket("http://192.168.18.18:4000")
            Log.e("TAG", "initialize: ${socket.id()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.e("TAG", "initialize: kadie")

        socket.connect()
        socket.on(Socket.EVENT_CONNECT, onConnect)
        socket
            .on("newUserToChatRoom", onNewUser) // To know if the new user entered the room.
            .on("updateChat", onUpdateChat) // To update if someone send a message to chatroom
            .on("userLeftChatRoom", onUserLeft) // To know if the user left the chatroom.
            .on("userTyping", onUserTyping)
            .on("userStopTyping", onUserStopTyping)
    }

    private fun updateChatList(chat: Chat) {
        val currentList = _chatList.value.toMutableList()
        currentList.add(chat)
        _chatList.value = currentList
    }

    fun sendMessage() {
        val chat = Chat(
            username = username,
            messageContent = message,
            roomName = roomName,
            messageType = MessageType.CHAT_MINE
        )
        val jsonData = gson.toJson(chat)

        updateChatList(chat)
        sendStopTyping()
        socket.emit("newMessage", jsonData)
        message = ""
    }

    fun sendStartTyping() {

        if (::jobTyping.isInitialized && !jobTyping.isCompleted)
            return

        Log.e("TAG", "sendStartTyping: ")
        jobTyping = viewModelScope.launch {
            val data = User(username, roomName)
            val jsonData = gson.toJson(data)
            socket.emit("typing", jsonData)
            delay(2000)
        }
    }

    fun stopTyping() {
        if (::jobTyping.isInitialized && !jobTyping.isCompleted)
            return

        jobTyping = viewModelScope.launch {
            delay(2000)
            sendStopTyping()
        }
    }

    private fun sendStopTyping() {
        val data = User(username, roomName)
        val jsonData = gson.toJson(data)
        socket.emit("stopTyping", jsonData)
    }


    private var onUserTyping = Emitter.Listener { result ->
        val user: User = gson.fromJson(result[0].toString(), User::class.java)
        Log.e("TAG", "onUserTyping: $user ")
        userIsTyping = user.username
    }

    private var onUserStopTyping = Emitter.Listener { result ->
        val user: User = gson.fromJson(result[0].toString(), User::class.java)
        Log.e("TAG", "onUserStopTyping: $user")
        userIsTyping = ""
    }


    private var onConnect = Emitter.Listener {
        Log.e("TAG", "connect: ")
        //After getting a Socket.EVENT_CONNECT which indicate socket has been connected to server,
        //send userName and roomName so that they can join the room.
        val data = User(username, roomName)
        val jsonData = gson.toJson(data)
        socket.emit("subscribe", jsonData)
    }

    private var onNewUser = Emitter.Listener { result ->
        val name = result[0] as String
        val chat = Chat(
            username = name,
            roomName = roomName,
            messageContent = "$name joining the party",
            messageType = MessageType.USER_JOIN
        )
        Log.e("TAG", "onNewUser: $chat ")

        updateChatList(chat)
    }

    private var onUpdateChat = Emitter.Listener { result ->
        var chat: Chat = gson.fromJson(result[0].toString(), Chat::class.java)
        chat = chat.copy(messageType = MessageType.CHAT_PARTNER)
        updateChatList(chat)
    }

    private var onUserLeft = Emitter.Listener { result ->
        val leftUsername = result[0] as String
        val chat = Chat(
            username = leftUsername,
            messageContent = "$leftUsername left the party",
            roomName = roomName,
            MessageType.USER_LEAVE
        )
        updateChatList(chat)
    }
}