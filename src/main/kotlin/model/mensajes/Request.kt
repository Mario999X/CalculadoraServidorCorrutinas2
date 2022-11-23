package model.mensajes

import kotlinx.serialization.Serializable

@Serializable
data class Request<T>(
    val content: T?,
    val type: Type

) {
    enum class Type {
        SUMA, RESTA, MULTIPLICAR, DIVIDIR, DESCONOCIDO // etc..
    }
}