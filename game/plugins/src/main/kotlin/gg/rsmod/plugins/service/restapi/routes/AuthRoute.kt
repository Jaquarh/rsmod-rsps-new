package gg.rsmod.plugins.service.restapi.routes

import gg.rsmod.plugins.service.restapi.data.AuthCreateData
import gg.rsmod.plugins.service.restapi.data.AuthData
import gg.rsmod.plugins.service.restapi.data.AuthResponseData
import gg.rsmod.plugins.service.restapi.data.AuthScopes
import org.mindrot.jbcrypt.BCrypt

class AuthRoute {
    companion object {
        private val auths:MutableList<AuthData> = mutableListOf()
        private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        private fun generateSecret(): String = (1..64).map { kotlin.random.Random.nextInt(0, charPool.size) }.map(charPool::get).joinToString("")

        fun validate(request: AuthData, scopes: MutableList<AuthScopes>): AuthResponseData {
            val auth = auths.firstOrNull { it.clientId == request.clientId } ?: return AuthResponseData.ACCOUNT_NOT_EXIST

            if(BCrypt.checkpw(auth.clientSecret, request.clientSecret)) {
                scopes.firstOrNull {
                    it == auth.clientRequest
                } ?: return AuthResponseData.INCORRECT_SCOPE

                return AuthResponseData.OK
            }
            return AuthResponseData.INCORRECT_CREDENTIALS
        }

        fun create(scope: AuthScopes): AuthCreateData {
            val gen: String = generateSecret()
            val secret: String = BCrypt.hashpw(gen, BCrypt.gensalt(16))
            val clientId: Int = auths.size +1
            auths.add(AuthData(clientId, secret, scope))
            return AuthCreateData(clientId, gen, AuthResponseData.OK)
        }
    }
}