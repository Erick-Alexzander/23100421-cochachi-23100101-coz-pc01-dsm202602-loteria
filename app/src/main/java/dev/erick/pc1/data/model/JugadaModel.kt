package dev.erick.pc1.data.model

data class JugadaModel(
    val uidUsuario: String = "",
    val sorteoId: String = "",
    val numeros: List<Int> = emptyList()
)