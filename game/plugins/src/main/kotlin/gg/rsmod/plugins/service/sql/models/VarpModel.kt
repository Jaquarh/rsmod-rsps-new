package gg.rsmod.plugins.service.sql.models

import org.jetbrains.exposed.sql.*

object VarpModel : Table("Varps") {
    val id = integer("id").autoIncrement().primaryKey()
    val playerId = (integer("player_id") references PlayerModel.id).nullable()
    val varpId = integer("varp_id")
    val state = integer("state")
}