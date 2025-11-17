package io.viascom.github.action.maintenance.util

inline fun <reified T : Enum<T>> String.fromCommaSeparatedValues(): List<T> {
    if (this.isBlank()) return emptyList()
    val enumConstants = enumValues<T>().associateBy { it.name }

    return this.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { value ->
            enumConstants[value.uppercase()]
        }
}

fun String?.splitCommaList(): List<String> =
    this?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
