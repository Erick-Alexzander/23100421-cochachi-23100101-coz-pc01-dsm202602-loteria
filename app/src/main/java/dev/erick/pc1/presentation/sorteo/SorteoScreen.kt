package dev.erick.pc1.presentation.sorteo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erick.pc1.data.model.BolillaModel
import dev.erick.pc1.data.model.JugadaModel
/**
 * Pantalla de seguimiento del sorteo.
 *
 * @param sorteoId El ID del sorteo a observar (proveniente de la selección anterior).
 * @param jugada La jugada del usuario (proporcionada por FirebaseJugadaManager, responsabilidad de Braulio).
 * @param viewModel ViewModel encargado de la lógica de aciertos y conexión a Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SorteoScreen(
    sorteoId: String,
    jugada: JugadaModel,
    viewModel: SorteoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sorteoId, jugada) {
        viewModel.initSorteo(sorteoId, jugada)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento del Sorteo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is SorteoUiState.Cargando -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SorteoUiState.Error -> {
                    Text(
                        text = state.mensaje,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SorteoUiState.Exito -> {
                    SorteoContent(
                        state = state,
                        jugada = jugada
                    )
                }
            }
        }
    }
}

@Composable
fun SorteoContent(
    state: SorteoUiState.Exito,
    jugada: JugadaModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera del sorteo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.sorteo.nombre.ifEmpty { "Sorteo Sin Nombre" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fecha: ${state.sorteo.fecha}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Badge(
                    containerColor = when (state.sorteo.estado.lowercase()) {
                        "abierto" -> Color(0xFF4CAF50)
                        "en curso" -> Color(0xFFFF9800)
                        "finalizado" -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.secondary
                    }
                ) {
                    Text(
                        text = state.sorteo.estado.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Bolillas sorteadas (6 espacios)
        Text(
            text = "Bolillas Sorteados (${state.bolillas.size}/6)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 1..6) {
                val bolilla = state.bolillas.find { it.orden == i }
                BolillaItem(bolilla = bolilla, orden = i)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Jugada del usuario
        Text(
            text = "Tu Jugada",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val numerosSorteados = state.bolillas.map { it.numero }.toSet()

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(jugada.numeros) { numero ->
                val esAcierto = numerosSorteados.contains(numero)
                JugadaNumberItem(numero = numero, esAcierto = esAcierto)
            }
        }

        Text(
            text = "Aciertos actuales: ${state.aciertos}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // Resultado definitivo o mensaje de espera
        if (state.resultadoDefinitivo != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.resultadoDefinitivo == "GANADOR")
                        Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.resultadoDefinitivo,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.resultadoDefinitivo == "GANADOR")
                        Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun BolillaItem(bolilla: BolillaModel?, orden: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (bolilla != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bolilla?.numero?.toString() ?: "?",
                color = if (bolilla != null) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "#$orden",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun JugadaNumberItem(numero: Int, esAcierto: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (esAcierto) Color(0xFFFFD700) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.aspectRatio(2f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = numero.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (esAcierto) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}