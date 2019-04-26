package gg.rsmod.plugins.service.restapi.data

data class AuthCreateData(val clientId: Int, val clientSecret: String?, val response: AuthResponseData)