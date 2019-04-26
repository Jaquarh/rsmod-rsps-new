package gg.rsmod.plugins.service.restapi.data

enum class AuthResponseData {
    OK,
    INCORRECT_SCOPE,
    INCORRECT_CREDENTIALS,
    NO_REFERER,
    MISSING_CREDENTIALS,
    ACCOUNT_NOT_EXIST
}