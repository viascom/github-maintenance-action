package io.viascom.github.action.maintenance.util

class Util {}

inline fun <reified T : Enum<T>> String.fromCommaSeparatedValues(): List<T> {
    if (this.isBlank()) return arrayListOf()

    return this.split(",")
        .map { it.trim() }
        .mapNotNull { value -> enumValues<T>().firstOrNull { it.name == value.uppercase() } }
}