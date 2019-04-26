on_npc_option(npc = Npcs.FARID_MORRISANE_ORES_AND_BARS, option = "talk-to") {
    player.queue { chat(this) }
}

on_npc_option(npc = Npcs.FARID_MORRISANE_ORES_AND_BARS, option = "prices") {
    player.queue { price_check(this) }
}

suspend fun chat(it: QueueTask) {
    it.chatPlayer("Hello, little boy.", animation = 568)
    it.chatNpc("I would prefer it if you didn't speak to me in such a manner. I'll have you know I'm an accomplished merchant.", animation = 568)
    when (it.options("Calm down, junior.", "Can you show me the prices of ores and bars?", "I best go and speak with someone more my height.")) {
        1 -> calm_down(it)
        2 -> price_check(it)
        3 -> good_bye(it)
    }
}

suspend fun calm_down(it: QueueTask) {
    it.chatPlayer("Calm down, junior.", animation = 565)
    it.chatNpc("Don't tell me to calm down! And don't call me 'junior'.", animation = 568)
    it.chatNpc("I'lkl have you know I am Farid Morrisane, son of Ali Morrisane, the world's greatest merchant!", animation = 568)
    it.chatPlayer("Then why are you here and not him?", animation = 565)
    it.chatNpc("My dad has given me the responsibility of expanding our business here.", animation = 565)
    it.chatPlayer("And you're up to the task? What a grown up boy you are! Mummy and daddy must be very pleased!", animation = 565)
    it.chatNpc("Look, mate- I may be young, I may be short, but I'm a respected merchant around here and don't have time to deal with simpletons like you.", animation = 568)
    when (it.options("Can you show me the prices of ores and bars?", "I best go and speak with someone more my height.")) {
        1 -> price_check(it)
        2 -> good_bye(it)
    }
}

suspend fun good_bye(it: QueueTask) {
    it.chatPlayer("I best go and speak with someone more my height.", animation = 565)
    it.chatNpc("Then I shall not stop you mister. I've too much work to do.", animation = 568)
}

suspend fun price_check(it: QueueTask) {
    //TODO: Add price check
}
