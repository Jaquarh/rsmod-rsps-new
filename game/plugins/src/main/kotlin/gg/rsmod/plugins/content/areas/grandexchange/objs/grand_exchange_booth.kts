import com.google.common.collect.ImmutableSet
import gg.rsmod.plugins.content.inter.bank.openBank

val BANK_BOOTHS = ImmutableSet.of(Objs.GRAND_EXCHANGE_BOOTH, Objs.GRAND_EXCHANGE_BOOTH_30390)
val EXCHANGE_BOOTHS = ImmutableSet.of(Objs.GRAND_EXCHANGE_BOOTH_10061)

BANK_BOOTHS.forEach { booth ->
    on_obj_option(obj = booth, option = "bank") {
        player.openBank()
    }

    on_obj_option(obj = booth, option = "collect") {
        open_collect(player)
    }
}

EXCHANGE_BOOTHS.forEach { booth ->
    on_obj_option(obj = booth, option = "exchange") {
        open_exchange(player)
    }

    on_obj_option(obj = booth, option = "collect") {
        open_collect(player)
    }
}

fun open_collect(p: Player) {
    p.setInterfaceUnderlay(color = -1, transparency = -1)
    p.openInterface(interfaceId = 402, dest = InterfaceDestination.MAIN_SCREEN)
}

fun open_exchange(p: Player) {
    p.setInterfaceUnderlay(color = -1, transparency = -1)
    p.openInterface(interfaceId = 465, dest = InterfaceDestination.MAIN_SCREEN)
    //p.openInterface(interfaceId = 467, dest = InterfaceDestination.INVENTORY)
}