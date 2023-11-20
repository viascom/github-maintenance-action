package io.viascom.github.action.maintenance.model

data class RequestData(
    var actor: String? = null,
    var branch: String? = null,
    var event: String? = null,
    var status: String? = null,
)