on_npc_option(npc = Npcs.MURKY_MATT_RUNES, option = "talk-to") {
    player.queue { chat(this) }
}

on_npc_option(npc = Npcs.MURKY_MATT_RUNES, option = "prices") {
    player.queue { price_check(this) }
}

on_npc_option(npc = Npcs.MURKY_MATT_RUNES, option = "decant") {
    player.queue { decant_jewelry(this) }
}

suspend fun chat(it: QueueTask) {
    it.chatNpc("Arrr, what is it that ye be wantin? I can tell ye all about the prices of runes.", animation = 568)
    when (it.options("What's a pirate doing here?", "Tell me about the prices of runes.", "I hear you deal in rings of forging?", "I got to go, erm, swab some deck! Yarr!")) {
        1 -> whats_a_pirate_doing_here(it)
        2 -> price_check(it)
        3 -> decant_jewelry(it)
        4 -> i_got_to_go(it)
    }
}

suspend fun whats_a_pirate_doing_here(it: QueueTask) {
    it.chatPlayer("What's a pirate doing here?", animation = 569)
    it.chatNpc("By my sea-blistered skin, I could ask the same of you!", animation = 568)
    it.chatPlayer("But... I'm not a pirate?", animation = 569)
    it.chatNpc("No? Then what's that smell? The smell o' someone spent too long at sea without a bath!", animation = 568)
    it.chatPlayer("I think that's probably you.", animation = 565)
    it.chatNpc("Har har har! We've got a stern landlubber 'ere Well, let me tell ye, I'm there for the Grand Exchange! Gonna cash in me loot!", animation = 569)
    it.chatPlayer("Don't you just want to sell it in a shop or trade it to someone specific?", animation = 562)
    it.chatNpc("By my wave-battered bones! Not when I can sell to the whole world from this very spot!", animation = 569)
    it.chatPlayer("You pirates are nothing but trouble! Why, once I travelled to Lunar Isle with a bunch of your types, and spent days sailing around in circles!", animation = 566)
    it.chatNpc("Then ye must know me brother! Murky Pat!", animation = 569)
    it.chatPlayer("Hmmm. Not so sure I remember him.", animation = 569)
    it.chatNpc("Well, 'e be on that ship for sure. And I remember 'im tellin' me about some guy like ye, getting all mixed up with curses and cabin boys.", animation = 568)
    it.chatPlayer("Yes! That was me!", animation = 568)
    it.chatNpc("Ye sure be a different character.", animation = 568)
    when (it.options("Tell me about the prices of runes.?.","I got to go, erm, swab some deck! Yarr!")) {
        1 -> price_check(it)
        2 -> i_got_to_go(it)
    }
}

suspend fun i_got_to_go(it: QueueTask) {
    it.chatPlayer("I got to go, erm, swab some deck! Yarr!", animation = 569)
    it.chatNpc("Defer your speech right there! Quit this derogatory and somewhat narrow-minded allusion that all folks of sea voyage are only concerned with washing the decks, looking after parrots and drinking rum. I'll have ye", animation = 568)
    it.chatNpc("know there is much more to a pirate then meets the eye.", animation = 568)
    it.chatPlayer("Aye-aye, captain!", animation = 565)
    it.chatNpc("...", animation = 561)
    it.chatPlayer("Oh, come on! Lighten up!", animation = 572)
}

suspend fun price_check(it: QueueTask) {
    //TODO: Add price check
}

suspend fun decant_jewelry(it: QueueTask) {
    //TODO: Add jewelry decant
}
