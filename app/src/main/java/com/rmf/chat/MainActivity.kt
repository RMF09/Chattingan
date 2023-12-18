package com.rmf.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rmf.chat.model.Chat
import com.rmf.chat.model.MessageType
import com.rmf.chat.ui.theme.ChattinganTheme
import com.rmf.chat.util.isOneEmoticon

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val viewModel: MainViewModel = viewModel()
            val chats by viewModel.chatList.collectAsState(initial = emptyList())
            LaunchedEffect(key1 = Unit) {
                viewModel.initialize()
            }

            ChattinganTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        chats = chats,
                        message = viewModel.message,
                        userIsTyping = viewModel.userIsTyping,
                        onMessageChange = {
                            viewModel.message = it
                        },
                        onClickSend = viewModel::sendMessage,
                        onTyping = viewModel::sendStartTyping,
                        onStopTyping = viewModel::stopTyping
                    )
                }
            }
        }
    }
}


@Composable
fun MainContent(
    chats: List<Chat>,
    message: String,
    userIsTyping: String,
    onMessageChange: (String) -> Unit,
    onClickSend: () -> Unit,
    onTyping: () -> Unit,
    onStopTyping: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), contentAlignment = Alignment.BottomCenter
    ) {
        ListChat(
            chats = chats, modifier = Modifier
                .fillMaxSize()
        )
        Column(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
        ) {
            if (userIsTyping.isNotBlank())
                Text(
                    text = stringResource(id = R.string.user_is_typing, userIsTyping),
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(4.dp)
                )

            MessageSendSection(
                message = message,
                onMessageChange = onMessageChange,
                onClickSend = onClickSend,
                onTyping = onTyping,
                onStopTyping = onStopTyping
            )
        }

    }
}


@Composable
fun ListChat(modifier: Modifier = Modifier, chats: List<Chat>) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(chats) { chat ->
            MessageContent(chat = chat)
        }
        item {
            Spacer(modifier = Modifier.height(54.dp))
        }
    }
}

@Composable
fun MessageContent(chat: Chat) {
    Box(modifier = Modifier.fillMaxWidth()) {

        val fontSize = if (chat.messageContent.isOneEmoticon()) 32.sp else 16.sp

        val align = when (chat.messageType) {
            MessageType.CHAT_MINE -> Alignment.TopEnd
            MessageType.CHAT_PARTNER -> Alignment.TopStart
            else -> Alignment.Center
        }

        val color =
            if (chat.messageType == MessageType.CHAT_MINE) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary

        Box(
            modifier = Modifier
                .background(
                    color = color.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .align(align)
                .defaultMinSize(minOf(30.dp))
        ) {
            Text(text = chat.messageContent, fontSize = fontSize, modifier = Modifier.padding(8.dp))

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSendSection(
    modifier: Modifier = Modifier,
    message: String,
    onMessageChange: (String) -> Unit,
    onClickSend: () -> Unit,
    onTyping: () -> Unit,
    onStopTyping: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val handler = rememberUpdatedState(newValue = Handler(Looper.myLooper()!!))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = message,
                onValueChange = {
                    onMessageChange(it)
                    onTyping()

                    handler.value.removeCallbacksAndMessages(null)
                    handler.value.postDelayed({
                        onStopTyping()
                    }, 4000)
                },
                placeholder = {
                    Text(text = stringResource(id = R.string.placeholder_message))
                },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        handler.value.removeCallbacksAndMessages(null)
                        focusManager.clearFocus()
                        onClickSend()
                    })
            )
            IconButton(onClick = {
                handler.value.removeCallbacksAndMessages(null)
                focusManager.clearFocus()
                onClickSend()
            }) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null)
            }
        }
    }
}
