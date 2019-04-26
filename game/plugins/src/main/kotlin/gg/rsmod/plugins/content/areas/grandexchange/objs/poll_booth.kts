import com.google.common.collect.ImmutableSet

val BOOTHS = ImmutableSet.of(Objs.POLL_BOOTH, Objs.POLL_BOOTH_26796, Objs.POLL_BOOTH_33482,
        Objs.POLL_BOOTH_26797, Objs.POLL_BOOTH_26798, Objs.POLL_BOOTH_26799, Objs.POLL_BOOTH_26800,
        Objs.POLL_BOOTH_26801, Objs.POLL_BOOTH_26802, Objs.POLL_BOOTH_26803, Objs.POLL_BOOTH_26804,
        Objs.POLL_BOOTH_26805, Objs.POLL_BOOTH_26806, Objs.POLL_BOOTH_26807, Objs.POLL_BOOTH_26808,
        Objs.POLL_BOOTH_26809, Objs.POLL_BOOTH_26810, Objs.POLL_BOOTH_26811, Objs.POLL_BOOTH_26812,
        Objs.POLL_BOOTH_32546, Objs.POLL_BOOTH_32547, Objs.POLL_BOOTH_33481)

BOOTHS.forEach { booth ->
    on_obj_option(obj = booth, option = "use") {
        open_poll_history(player)
    }
}

fun open_poll_history(p: Player) {
    p.setInterfaceUnderlay(color = -1, transparency = -1)
    p.openInterface(interfaceId = 310, dest = InterfaceDestination.MAIN_SCREEN)
}