package gg.rsmod.plugins.service.sql.models

import org.jetbrains.exposed.sql.*

object ItemAttributeModel : Table("ItemAttributes") {
    val id = integer("id").autoIncrement().primaryKey()
    val itemId = integer("itemId")
    val key = varchar("key", 1024)
    val value = varchar("value", 1024)
    val playerId = (integer("player_id") references PlayerModel.id).nullable()
}