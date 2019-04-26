on_npc_option(npc = Npcs.BRUGSEN_BURSEN, option = "talk-to") {
    player.queue { chat(this) }
}

on_npc_option(npc = Npcs.BRUGSEN_BURSEN, option = "prices") {
    player.queue { price_check(this) }
}

suspend fun chat(it: QueueTask) {
    it.chatNpc("Hello and welcome to the Grand Exchange!<br>How can I help you?", animation = 568)
    when (it.options("What is this place?", "Can you tell me the price of an item?.", "I'm fine thanks.")) {
        1 -> what_is_this_place(it)
        2 -> price_check(it)
        3 -> im_fine_thanks(it)
    }
}

suspend fun what_is_this_place(it: QueueTask) {
    it.chatPlayer("What is this place?", animation = 554)
    it.chatNpc("this is the Grand Exchange, the best trading spot in the land! People come from all across the country to buy and sell every item under the sun.", animation = 568)
    it.chatNpc("When you want to buy or sell an item, talk to the clerks in the middle. They'll tell you how to set up a trade offer.", animation = 566)
    it.chatNpc("Now is there something I can do for you?", animation = 566)
    when (it.options("Can you tell me the price of an item?.", "I'm fine thanks.")) {
        1 -> price_check(it)
        2 -> im_fine_thanks(it)
    }
}

suspend fun price_check(it: QueueTask) {
    it.chatPlayer("Can you tell me the price of an item?", animation = 569)
    it.chatNpc("What had you in mind?", animation = 566)
    //TODO: Add price check
}

suspend fun im_fine_thanks(it: QueueTask) {
    it.chatPlayer("I'm fine, thanks.", animation = 569)
}