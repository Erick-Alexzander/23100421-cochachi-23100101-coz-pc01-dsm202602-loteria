package dev.erick.pc1.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import dev.erick.pc1.data.model.JugadaModel
import dev.erick.pc1.data.remote.FirebaseJugadaManager
import dev.erick.pc1.presentation.auth.LoginScreen
import dev.erick.pc1.presentation.auth.RegisterScreen
import dev.erick.pc1.presentation.home.HomeScreen
import dev.erick.pc1.presentation.jugada.JugadaScreen
import dev.erick.pc1.presentation.sorteo.SorteoScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_HOME = "home"
private const val ROUTE_JUGADA = "jugada"
private const val ROUTE_SORTEO = "sorteo"

private const val SORTEO_ID = "sorteos_001"

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val currentUser = FirebaseAuth.getInstance().currentUser

    val startDestination =
        if (currentUser != null) ROUTE_HOME else ROUTE_LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(ROUTE_REGISTER)
                }
            )
        }

        composable(ROUTE_REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(ROUTE_HOME) {
            HomeScreen(
                onNavigateToJugada = {
                    navController.navigate(ROUTE_JUGADA)
                },
                onNavigateToSorteo = {
                    navController.navigate(ROUTE_SORTEO)
                },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_HOME) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(ROUTE_JUGADA) {
            JugadaScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(ROUTE_SORTEO) {
            SorteoRoute(
                navController = navController
            )
        }
    }
}

@Composable
private fun SorteoRoute(
    navController: NavHostController
) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate(ROUTE_LOGIN) {
                popUpTo(ROUTE_HOME) {
                    inclusive = true
                }
            }
        }
        return
    }

    var jugada by remember(currentUser.uid) {
        mutableStateOf<JugadaModel?>(null)
    }

    var cargando by remember(currentUser.uid) {
        mutableStateOf(true)
    }

    var error by remember(currentUser.uid) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(currentUser.uid) {
        cargando = true
        error = null

        try {
            val resultado = FirebaseJugadaManager()
                .getJugada(SORTEO_ID, currentUser.uid)

            resultado.fold(
                onSuccess = { numeros ->
                    jugada = numeros?.let {
                        JugadaModel(
                            uidUsuario = currentUser.uid,
                            sorteoId = SORTEO_ID,
                            numeros = it
                        )
                    }

                    if (jugada == null) {
                        error = "Todavía no tienes una jugada registrada para este sorteo."
                    }
                },
                onFailure = { exception ->
                    error = exception.localizedMessage
                        ?: "No se pudo recuperar tu jugada de Firebase."
                }
            )
        } catch (exception: Exception) {
            error = exception.localizedMessage
                ?: "Ocurrió un error al consultar tu jugada."
        } finally {
            cargando = false
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                cargando -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando tu jugada...")
                }

                jugada != null -> {
                    SorteoScreen(
                        sorteoId = SORTEO_ID,
                        jugada = jugada!!
                    )
                }

                else -> {
                    Text(
                        text = error ?: "No se encontró tu jugada.",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}