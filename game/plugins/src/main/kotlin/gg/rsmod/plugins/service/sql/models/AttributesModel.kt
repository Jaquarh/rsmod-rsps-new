package gg.rsmod.plugins.service.sql.models

import org.jetbrains.exposed.sql.*

object AttributesModel : Table("Attributes") {
    val id = integer("id").autoIncrement().primaryKey()
    val playerId = (integer("player_id") references PlayerModel.id).nullable()
    val key = varchar("key", 60)
    val value = varchar("value", 120)
}