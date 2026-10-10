package dev.erick.pc1.data.model

data class SorteoModel(
    val id: String = "",
    val nombre: String = "",
    val fecha: String = "",
    val estado: String = "" // "abierto", "en curso", "finalizado"
)