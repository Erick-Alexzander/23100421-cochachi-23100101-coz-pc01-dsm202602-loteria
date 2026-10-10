package dev.erick.pc1.presentation.sorteo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.erick.pc1.data.model.BolillaModel
import dev.erick.pc1.data.model.JugadaModel
import dev.erick.pc1.data.model.SorteoModel
import dev.erick.pc1.data.remote.FirebaseSorteoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class SorteoUiState {
    object Cargando : SorteoUiState()
    data class Error(val mensaje: String) : SorteoUiState()
    data class Exito(
        val sorteo: SorteoModel,
        val bolillas: List<BolillaModel>,
        val aciertos: Int,
        val resultadoDefinitivo: String? // "GANADOR", "SIGUE INTENTANDO", "ESPERANDO RESULTADOS", o null si sigue en curso
    ) : SorteoUiState()
}

class SorteoViewModel(
    private val sorteoManager: FirebaseSorteoManager = FirebaseSorteoManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SorteoUiState>(SorteoUiState.Cargando)
    val uiState: StateFlow<SorteoUiState> = _uiState.asStateFlow()

    private var currentSorteo: SorteoModel? = null
    private var currentBolillas: List<BolillaModel> = emptyList()
    private var currentJugada: JugadaModel? = null

    fun initSorteo(sorteoId: String, jugada: JugadaModel) {
        if (currentJugada == jugada && currentSorteo?.id == sorteoId) return
        
        _uiState.value = SorteoUiState.Cargando
        currentJugada = jugada
        observarSorteo(sorteoId)
        observarBolillas(sorteoId)
    }

    private fun observarSorteo(sorteoId: String) {
        viewModelScope.launch {
            sorteoManager.observarSorteo(sorteoId)
                .catch { _uiState.value = SorteoUiState.Error(it.message ?: "Error al cargar sorteo") }
                .collect { sorteo ->
                    if (sorteo != null) {
                        currentSorteo = sorteo
                        actualizarEstado()
                    } else {
                        _uiState.value = SorteoUiState.Error("Sorteo no encontrado")
                    }
                }
        }
    }

    private fun observarBolillas(sorteoId: String) {
        viewModelScope.launch {
            sorteoManager.observarBolillas(sorteoId)
                .catch { _uiState.value = SorteoUiState.Error(it.message ?: "Error al cargar bolillas") }
                .collect { bolillas ->
                    currentBolillas = bolillas
                    actualizarEstado()
                }
        }
    }

    private fun actualizarEstado() {
        val sorteo = currentSorteo ?: return
        val jugada = currentJugada ?: return
        
        val aciertos = calcularAciertos(jugada, currentBolillas)
        
        var resultadoDefinitivo: String? = null
        if (sorteo.estado == "finalizado") {
            if (currentBolillas.size == 6) {
                resultadoDefinitivo = if (aciertos == 6) "GANADOR" else "SIGUE INTENTANDO"
            } else {
                resultadoDefinitivo = "ESPERANDO RESULTADOS"
            }
        }

        _uiState.value = SorteoUiState.Exito(
            sorteo = sorteo,
            bolillas = currentBolillas,
            aciertos = aciertos,
            resultadoDefinitivo = resultadoDefinitivo
        )
    }

    private fun calcularAciertos(jugada: JugadaModel, bolillas: List<BolillaModel>): Int {
        val numerosJugados = jugada.numeros.toSet()
        val numerosSorteados = bolillas.map { it.numero }.toSet()
        return numerosJugados.intersect(numerosSorteados).size
    }
}