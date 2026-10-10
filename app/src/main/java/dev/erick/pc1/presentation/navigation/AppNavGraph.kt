package dev.erick.pc1.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import dev.erick.pc1.presentation.auth.LoginScreen
import dev.erick.pc1.presentation.auth.RegisterScreen
import dev.erick.pc1.presentation.home.HomeScreen
import dev.erick.pc1.presentation.jugada.JugadaScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToJugada = {
                    navController.navigate("jugada")
                },
                onNavigateToSorteo = {
                    navController.navigate("sorteo")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("jugada") {
            JugadaScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Ruta de Sorteo (Integración con el módulo/pantalla de Erick: SorteoScreen)
        composable("sorteo") {
            SorteoPlaceholderScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Placeholder temporal para la pantalla de Sorteo (SorteoScreen.kt a cargo de Erick).
 * Una vez que Erick implemente SorteoScreen, se puede enlazar aquí directamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SorteoPlaceholderScreen(onBack: () -> Unit) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pantalla de Sorteo en desarrollo",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Esta sección corresponde a Erick (FirebaseSorteoManager y SorteoScreen). Estará integrada tras el merge.",
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Volver")
            }
        }
    }
}
