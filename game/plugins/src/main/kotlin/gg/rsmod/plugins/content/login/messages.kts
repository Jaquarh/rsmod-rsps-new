on_login {
    val loginMessages = listOf(
        "We are currently in Beta. Please report any bugs to a member of staff.",
        "We are recruiting staff! Head over to our website to apply for a position."
    )

    loginMessages.forEach { msg -> player.message(msg, ChatMessageType.GAME_MESSAGE) }
}