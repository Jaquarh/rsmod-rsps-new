package gg.rsmod.plugins.service.sql.models

import org.jetbrains.exposed.sql.*

object TimerModel : Table("Timers") {
    val id = integer("id").autoIncrement().primaryKey()
    val playerId = (integer("player_id") references PlayerModel.id).nullable()
    val currentMs = long("current_ms")
    val identifier = varchar("identified", 1024)
    val tickOffline = bool("tick_offline").default(false)
    val timeLeft = integer("time_left")
}