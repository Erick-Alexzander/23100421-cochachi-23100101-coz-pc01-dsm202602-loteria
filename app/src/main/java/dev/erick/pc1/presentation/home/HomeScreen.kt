package dev.erick.pc1.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import dev.erick.pc1.data.remote.FirebaseAuthManager
import dev.erick.pc1.data.remote.FirebaseJugadaManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToJugada: () -> Unit,
    onNavigateToSorteo: () -> Unit,
    onLogout: () -> Unit
) {
    val authManager = remember { FirebaseAuthManager() }
    val jugadaManager = remember { FirebaseJugadaManager() }
    val coroutineScope = rememberCoroutineScope()

    val userEmail = authManager.getCurrentUserEmail() ?: "Usuario"
    
    var sorteoNombre by remember { mutableStateOf("Cargando sorteo...") }
    var sorteoFecha by remember { mutableStateOf("--/--/----") }
    var sorteoEstado by remember { mutableStateOf("Desconocido") }
    var isLoadingSorteo by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val result = jugadaManager.getActiveSorteo()
            isLoadingSorteo = false
            if (result.isSuccess) {
                val pair = result.getOrNull()
                if (pair != null) {
                    val data = pair.second
                    sorteoNombre = data["nombre"] as? String ?: "Sorteo Principal"
                    val fechaVal = data["fecha"]
                    sorteoFecha = when (fechaVal) {
                        is Timestamp -> fechaVal.toDate().toString()
                        is String -> fechaVal
                        else -> "Fecha por definir"
                    }
                    sorteoEstado = data["estado"] as? String ?: "abierto"
                } else {
                    sorteoNombre = "No hay sorteos activos"
                    sorteoFecha = "-"
                    sorteoEstado = "Cerrado"
                }
            } else {
                sorteoNombre = "Error al cargar sorteo"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LOTERÍA - Inicio") },
                actions = {
                    IconButton(onClick = {
                        authManager.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Hola!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = userEmail,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjeta de Información del Sorteo
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Información del Sorteo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoadingSorteo) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = "Nombre: $sorteoNombre")
                        Text(text = "Fecha: $sorteoFecha")
                        Text(text = "Estado: $sorteoEstado", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Registrar/Consultar Jugada
            Button(
                onClick = onNavigateToJugada,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar / Consultar mi Jugada")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Seguimiento Sorteo (Erick's screen)
            OutlinedButton(
                onClick = onNavigateToSorteo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seguimiento del Sorteo")
            }
        }
    }
}
