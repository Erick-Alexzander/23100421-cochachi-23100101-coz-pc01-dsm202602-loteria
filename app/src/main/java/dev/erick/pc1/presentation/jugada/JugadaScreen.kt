package dev.erick.pc1.presentation.jugada

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.erick.pc1.data.remote.FirebaseAuthManager
import dev.erick.pc1.data.remote.FirebaseJugadaManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JugadaScreen(
    onBack: () -> Unit
) {
    val authManager = remember { FirebaseAuthManager() }
    val jugadaManager = remember { FirebaseJugadaManager() }
    val coroutineScope = rememberCoroutineScope()

    val uid = authManager.getCurrentUserId()

    var activeSorteoId by remember { mutableStateOf<String?>(null) }
    var sorteoEstado by remember { mutableStateOf("abierto") }
    var existingJugada by remember { mutableStateOf<List<Int>?>(null) }
    val selectedNumbers = remember { mutableStateListOf<Int>() }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            if (uid == null) {
                message = "Usuario no autenticado."
                isLoading = false
                return@launch
            }

            val sorteoResult = jugadaManager.getActiveSorteo()
            if (sorteoResult.isSuccess) {
                val pair = sorteoResult.getOrNull()
                if (pair != null) {
                    activeSorteoId = pair.first
                    sorteoEstado = pair.second["estado"] as? String ?: "abierto"

                    val jugadaResult = jugadaManager.getJugada(activeSorteoId!!, uid)
                    if (jugadaResult.isSuccess) {
                        val jugada = jugadaResult.getOrNull()
                        if (jugada != null) {
                            existingJugada = jugada
                        }
                    }
                } else {
                    message = "No hay sorteos activos disponibles."
                }
            } else {
                message = "Error al cargar el sorteo activo."
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selección de Jugada (1 al 45)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (existingJugada != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¡Ya tienes una jugada registrada!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Tus 6 números seleccionados:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = existingJugada!!.sorted().joinToString(", "),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Volver al Inicio")
                    }
                }
            } else {
                Text(
                    text = "Seleccionados: ${selectedNumbers.size}/6",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedNumbers.size == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedNumbers.isNotEmpty()) {
                    Text(
                        text = "Números: ${selectedNumbers.sorted().joinToString(", ")}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..45).toList()) { number ->
                        val isSelected = selectedNumbers.contains(number)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedNumbers.remove(number)
                                } else {
                                    if (selectedNumbers.size < 6) {
                                        selectedNumbers.add(number)
                                    }
                                }
                            },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = number.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (message != null) {
                    Text(
                        text = message!!,
                        color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        if (activeSorteoId == null || uid == null) {
                            message = "Error: Sorteo o usuario no válido."
                            return@Button
                        }
                        if (selectedNumbers.size != 6) {
                            message = "Debes seleccionar exactamente 6 números."
                            return@Button
                        }

                        isSaving = true
                        message = null
                        coroutineScope.launch {
                            val result = jugadaManager.saveJugada(activeSorteoId!!, uid, selectedNumbers.toList())
                            isSaving = false
                            if (result.isSuccess) {
                                isSuccess = true
                                message = "¡Jugada guardada con éxito!"
                                existingJugada = selectedNumbers.toList()
                            } else {
                                message = result.exceptionOrNull()?.localizedMessage ?: "Error al guardar jugada."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedNumbers.size == 6 && !isSaving && activeSorteoId != null
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar Jugada")
                    }
                }
            }
        }
    }
}
