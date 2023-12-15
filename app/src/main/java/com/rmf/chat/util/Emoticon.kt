package com.rmf.chat.util

private fun containsEmoticon(text: String): Boolean {
    // Define your regex pattern for emoticons
    val emoticonRegex = Regex("""[\p{So}\p{Sk}\p{Sm}\p{Sc}\p{S}]+""")
    // Check if the text contains any emoticon character
    return emoticonRegex.containsMatchIn(text)
}

fun String.isOneEmoticon(): Boolean {
    return containsEmoticon(this) && this.length == 2
}