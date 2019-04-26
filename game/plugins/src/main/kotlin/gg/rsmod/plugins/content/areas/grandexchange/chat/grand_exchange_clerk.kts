arrayOf(Npcs.GRAND_EXCHANGE_CLERK, Npcs.GRAND_EXCHANGE_CLERK_2149, Npcs.GRAND_EXCHANGE_CLERK_2150, Npcs.GRAND_EXCHANGE_CLERK_2151).forEach { clerk ->
    on_npc_option(npc = clerk, option = "talk-to", lineOfSightDistance = 2) {
        player.queue { dialog(this) }
    }
    on_npc_option(npc = clerk, option = "exchange", lineOfSightDistance = 2) {
        open_exchange(player)
    }
    on_npc_option(npc = clerk, option = "history", lineOfSightDistance = 2) {
        open_exchange_history(player)
    }
    on_npc_option(npc = clerk, option = "sets", lineOfSightDistance = 2) {
        open_exchange_sets(player)
    }
}

suspend fun dialog(it: QueueTask) {
    it.chatNpc("Welcome to the Grand Exchange.<br>Would you like to trade now, or exchange item sets?")
    when (it.options("How do I use the Grand Exchange?", "I'd like to set up trade offers please.", "Can you help me with item sets?", "I don't want Exchange collection reminders on login.", "I'm fine, thanks.")) {
        1 -> how_do_i_use_the_grand_exchange(it)
        2 -> id_like_to_set_up_trade_offers_please(it)
        3 -> can_you_help_me_with_item_sets(it)
        4 -> id_like_exhange_collection_reminders_on_login_please(it)
        5 -> im_fine_thanks(it)
    }
}

suspend fun how_do_i_use_the_grand_exchange(it: QueueTask) {
    it.chatPlayer("How do I use the Grand Exchange?", animation = 554)
    it.chatNpc("My colleague and I can let you set up trade offers.<br>You can offer the Sell items or Buy items.", animation = 568)
    it.chatNpc("When you want to sell something, you give us the items and tell us how much money you want for them.", animation = 569)
    it.chatNpc("We'll look for someone who wants to buy those items at your price, and we'll perform the trade. You can then collect the cash here, or at any bank.", animation = 567)
    it.chatNpc("When you want to buy something, you tell us what you want, and give us the cash you're willing to spend on it.", animation = 569)
    it.chatNpc("We'll look for someone who's selling those items at your price, and we'll perform the trade. You can then collect the items here, or at any bank, along with any left-over cash.", animation = 569)
    it.chatNpc("Sometimes it takes a while to find a matching trade offer. If you change your mind, we'll let you cancel your trade offer, and we'll return your unused items and cash.", animation = 566)
    it.chatNpc("That's all the essential information you need to get started. Would you like to trade now, or exchange item sets?", animation = 566)
    when (it.options("I'd like to set up trade offers please.", "Can you help me with item sets?", "I don't want Exchange collection reminders on login.", "I'm fine, thanks.")) {
        1 -> id_like_to_set_up_trade_offers_please(it)
        2 -> can_you_help_me_with_item_sets(it)
        3 -> id_like_exhange_collection_reminders_on_login_please(it)
        4 -> im_fine_thanks(it)
    }
}

suspend fun id_like_to_set_up_trade_offers_please(it: QueueTask) {
    it.chatPlayer("I'd like to set up trade offers, please.", animation = 554)
    open_exchange(it.player)
}

suspend fun can_you_help_me_with_item_sets(it: QueueTask) {
    it.chatPlayer("can you help me with item sets?", animation = 554)
    open_exchange_sets(it.player)
}

suspend fun im_fine_thanks(it: QueueTask) {
    it.chatPlayer("I'm fine, thanks.", animation = 554)
}

suspend fun id_like_exhange_collection_reminders_on_login_please(it: QueueTask) {
    it.chatPlayer("I'd like to Exchange collection reminders on login, please.", animation = 554)
    it.chatNpc("Okay, when you log in, we'lkl send you a message about any items that are waiting for you to collect them.", animation = 568)
    when (it.options("Thank you.", "I don't want Exchange Collection reminders on login.")) {
        1 -> thank_you(it)
        2 -> i_dont_want_exchange_collection_reminders_on_login(it)
    }
}

suspend fun thank_you(it: QueueTask) {
    it.chatPlayer("Thank you.", animation = 554)
}

suspend fun i_dont_want_exchange_collection_reminders_on_login(it: QueueTask) {
    it.chatPlayer("I don't want Exchange Collection reminders on login.", animation = 554)
    it.chatNpc("Okay, when you log in, you will no longer be reminded about items waiting to be collected, unless the items only arrived after you logged out.", animation = 568)
    when (it.options("Thank you.", "I like Exchange Collection reminders on login, please.")) {
        1 -> thank_you(it)
        2 -> id_like_exhange_collection_reminders_on_login_please(it)
    }
}

fun open_exchange(p: Player) {
    p.setInterfaceUnderlay(color = -1, transparency = -1)
    p.openInterface(interfaceId = 465, dest = InterfaceDestination.MAIN_SCREEN)
}

fun open_exchange_history(p: Player) {
    p.setInterfaceUnderlay(color = -1, transparency = -1)
    p.openInterface(interfaceId = 383, dest = InterfaceDestination.MAIN_SCREEN)
}

fun open_exchange_sets(p: Player) {
    p.setInterfaceUnderlay(color = -1, transparency = -1)
    p.openInterface(interfaceId = 451, dest = InterfaceDestination.MAIN_SCREEN)
}