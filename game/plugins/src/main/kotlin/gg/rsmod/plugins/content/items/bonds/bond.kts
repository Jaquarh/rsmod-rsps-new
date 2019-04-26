val bonds = listOf(Items.OLD_SCHOOL_BOND, Items.OLD_SCHOOL_BOND_13191, Items.OLD_SCHOOL_BOND_UNTRADEABLE)
val attrKey = AttributeKey<Any>("OLD_SCHOOL_BONDS")

bonds.forEach { item ->
    on_item_option(item = item, option = "redeem") {
        player.inventory.remove(item = item, amount = 1)
        player.addXp((1..player.getSkills().maxSkills).shuffled().first(), 500.0)
    }

    on_item_option(item = item, option = "deposit") {
        player.inventory.remove(item = item, amount = 1)
        player.attr[attrKey] = Integer.parseInt(player.attr.getOrDefault(attrKey, 0).toString()) +1
        player.message("You successfully deposited a bond.", ChatMessageType.GAME_MESSAGE)
    }
}