import gg.rsmod.game.model.attr.NEW_ACCOUNT_ATTR

val NPC_ID = Npcs.QUEST_GUIDE

val starterItems = listOf<Item>(
        Item(995, 1000000)
)

on_login {
    if(player.attr.getOrDefault(NEW_ACCOUNT_ATTR, false)) {
        player.queue { dialog(this) }
    }
}

suspend fun dialog(it: QueueTask) {
    it.chatNpc("Good day! Welcome to ${world.gameContext.name}.", animation = 568, npc = NPC_ID, title = "${world.gameContext.name} Guide")
    it.chatNpc("Would you like to take a tour?", animation = 554, npc = NPC_ID, title = "${world.gameContext.name} Guide")
    when (it.options("Yes. A tour would be good.", "No. I am an experienced player.")) {
        1 -> {
            tour(it)
            startItems(it)
        }
        2 -> {
            startItems(it)
        }
    }
}

suspend fun tour(it: QueueTask) {
    it.chatNpc("${world.gameContext.name} is a PvM and PvP dedicated server.", animation = 568, npc = NPC_ID, title = "${world.gameContext.name} Guide")
    it.chatNpc("Making money is simple. You can raid bosses, or, choose to raid players.", animation = 569, npc = NPC_ID, title = "${world.gameContext.name} Guide")
    it.chatPlayer("And where can I start?", animation = 554)
    it.chatNpc("There are plenty of ways you can skill and gain XP.", animation = 569, npc = NPC_ID, title = "${world.gameContext.name} Guide")
}

suspend fun startItems(it: QueueTask) {
    it.chatNpc("As a token of gratitude, please accept these starter items!", animation = 568, npc = NPC_ID, title = "${world.gameContext.name} Guide")
    starterItems.forEach {
        item ->
            it.player.inventory.add(item)
            it.chatNpc("I have rewarded you ${item.amount} of ${item.getName(world.definitions)}.", animation = 568, npc = NPC_ID, title = "${world.gameContext.name} Guide")
    }
    it.chatNpc("On a final note, if you need any help whilst playing<br>Feel free to come and chat to me again!<br>Good luck, stranger!", animation = 568, npc = NPC_ID, title = "${world.gameContext.name} Guide")
}