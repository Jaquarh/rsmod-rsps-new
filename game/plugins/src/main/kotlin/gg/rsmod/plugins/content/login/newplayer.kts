import gg.rsmod.game.model.attr.NEW_ACCOUNT_ATTR

/**
 * @author Kye <okaydots@gmail.com>
 * @description Allows a newly registered user to take a tour of the RSPS, also allows them to receive starter items if enabled.
 **/

val npcId = Npcs.QUEST_GUIDE
val startItems = true

val starterItems= listOf(
        Item(Items.COINS, 200000),
        Item(Items.COOKED_CHICKEN, 5),
        Item(Items.BRONZE_PLATEBODY, 1),
        Item(Items.BRONZE_PLATELEGS, 1),
        Item(Items.BRONZE_FULL_HELM, 1),
        Item(Items.BRONZE_SWORD, 1),
        Item(Items.AIR_RUNE, 30),
        Item(Items.MIND_RUNE, 30)
)

on_login {
    if(player.attr.getOrDefault(NEW_ACCOUNT_ATTR, false))
        player.queue { dialog(this) }
}

suspend fun dialog(it: QueueTask) {
    it.player.lock()
    it.chatNpc("Good day! Welcome to ${world.gameContext.name}. I'm your guide for today.", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")
    it.chatNpc("Would you like to take a tour?", animation = 554, npc = npcId, title = "${world.gameContext.name} Guide")

    when (it.options("Yes. I can spare 2 minutes of time.", "No. I am an experienced player.")) {
        1 -> {
            tour(it)
            if(startItems)
                startItems(it)
            it.chatNpc("If you need any help whilst playing feel free to come and chat to me again!<br>Good luck on your travels, stranger!", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")
        }
        2 -> {
            if(startItems)
                startItems(it)
            it.chatNpc("If you need any help whilst playing feel free to come and chat to me again!<br>Good luck on your travels, stranger!", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")
        }
    }
}

suspend fun tour(it: QueueTask) {
    it.chatNpc("${world.gameContext.name} is a PvE and PvP dedicated server.", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")
    it.chatNpc("Making money is simple. You can raid bosses, or, choose to raid players.", animation = 569, npc = npcId, title = "${world.gameContext.name} Guide")
    it.chatPlayer("And where can I start?", animation = 554)

    /** Varrock **/
    it.player.moveTo(3210, 3424, 0)
    it.chatNpc("Ah, Varrock. The land of the Grand Exchange.<br>Here, you can sell and buy items between players.", animation = 569, npc = npcId, title = "${world.gameContext.name} Guide")
    it.chatPlayer("What items are sold here?", animation = 554)
    it.chatNpc("All types of tradable items can be found at the GE. PvE/PvP will get you items to sell.", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")

    /** Lumbridge **/
    it.player.moveTo(3222, 3218, 0)
    it.chatNpc("Next, Lumbridge. The land of the Wise, cows and creepy Goblins.", animation = 569, npc = npcId, title = "${world.gameContext.name} Guide")
    it.chatPlayer("Is this a good area to train?", animation = 554)
    it.chatNpc("Yes! You can find all of the trainers here: Goblins and Cows are also Lvl 2.", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")

    /** Duel Arena **/
    it.player.moveTo(3360 ,3213 , 0)
    it.chatNpc("Finally, Mini-Games. Welcome to the Duel Arena!", animation = 569, npc = npcId, title = "${world.gameContext.name} Guide")
    it.chatPlayer("Do I keep my items on death?", animation = 554)
    it.chatNpc("You can stake your items on a player duel which will be lost if you die.<br>All unstaked items will be kept on death.", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")

    /** Move Back Home **/
    it.player.moveTo(3087, 3497, 0)
}

suspend fun startItems(it: QueueTask) {
    it.chatNpc("Thanks for choosing ${world.gameContext.name}, please accept these starter items!", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")

    starterItems.forEach {
        item ->
            it.player.inventory.add(item)
            it.chatNpc("I have rewarded you ${item.amount} ${item.getName(world.definitions)}.", animation = 568, npc = npcId, title = "${world.gameContext.name} Guide")

    }
    it.player.unlock()
}