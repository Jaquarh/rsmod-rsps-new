package gg.rsmod.plugins.service.sql.controllers

import gg.rsmod.game.model.entity.Client
import gg.rsmod.game.service.serializer.json.JsonPlayerSerializer

abstract class Controller {
    protected fun getContainers(client: Client): List<JsonPlayerSerializer.PersistentContainer> {
        val containers = mutableListOf<JsonPlayerSerializer.PersistentContainer>()

        client.containers.forEach { key, container ->
            if (!container.isEmpty) {
                containers.add(JsonPlayerSerializer.PersistentContainer(key.name, container.toMap()))
            }
        }

        return containers
    }
}