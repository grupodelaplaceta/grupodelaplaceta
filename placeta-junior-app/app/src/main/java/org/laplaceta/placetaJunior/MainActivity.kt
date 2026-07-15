package org.laplaceta.placetajunior

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.laplaceta.placetajunior.data.SessionManager
import org.laplaceta.placetajunior.network.ApiClient
import org.laplaceta.placetajunior.ui.screens.*
import org.laplaceta.placetajunior.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link from placeta-junior://auth
        val deepLinkDip = intent?.data?.getQueryParameter("dip")
        val deepLinkAuthorized = intent?.data?.getQueryParameter("authorized")

        // Init session persistence
        SessionManager.init(this)
        val savedSession = SessionManager.getSession()

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = PJPurple,
                    onPrimary = PJWhite,
                    primaryContainer = PJPurpleLight,
                    secondary = PJBlue,
                    onSecondary = PJWhite,
                    secondaryContainer = PJBlueLight,
                    tertiary = PJOrange,
                    background = PJWhite,
                    surface = PJWhite,
                    error = PJRed,
                    onError = PJWhite,
                    onBackground = PJBlack,
                    onSurface = PJBlack
                ),
                typography = Typography
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = PJWhite
                ) {
                    AppNavigation(
                        initialDip = deepLinkDip,
                        initialAuthorized = deepLinkAuthorized == "true",
                        savedSession = savedSession
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun AppNavigation(initialDip: String? = null, initialAuthorized: Boolean = false, savedSession: SessionManager.SessionData? = null) {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    var currentDip by remember { mutableStateOf(initialDip ?: savedSession?.dip ?: "") }
    var currentNombre by remember { mutableStateOf(savedSession?.nombre ?: "") }
    var currentApellidos by remember { mutableStateOf(savedSession?.apellidos ?: "") }
    var currentSaldo by remember { mutableIntStateOf(savedSession?.saldo ?: 0) }

    // Auto-login if session exists
    val hasSavedSession = savedSession != null

    var tutorData by remember { mutableStateOf(TutorData()) }
    var minorData by remember { mutableStateOf(MinorData()) }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            if (showSplash) {
                SplashScreen(
                    onFinished = {
                        showSplash = false
                        if (initialDip != null && initialAuthorized) {
                            currentDip = initialDip
                            navController.navigate("dashboard/$initialDip") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        composable("home") {
            HomeScreen(
                onLogin = { navController.navigate("login") },
                onRegister = {
                    tutorData = TutorData()
                    minorData = MinorData()
                    navController.navigate("register_tutor")
                },
                onTutorLogin = { }
            )
        }

        composable("login") {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = { dip, nombre, apellidos, saldo ->
                    currentDip = dip
                    currentNombre = nombre
                    currentApellidos = apellidos
                    currentSaldo = saldo
                    navController.navigate("dashboard/$dip") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onGoToDashboard = { dip ->
                    navController.navigate("dashboard/$dip") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("register_tutor") {
            RegisterTutorScreen(
                initialData = tutorData,
                onBack = { navController.popBackStack() },
                onContinue = { data ->
                    tutorData = data
                    navController.navigate("register_minor")
                }
            )
        }

        composable("register_minor") {
            RegisterMinorScreen(
                initialData = minorData,
                onBack = { navController.popBackStack() },
                onContinue = { data ->
                    minorData = data
                    navController.navigate("register_review")
                }
            )
        }

        composable("register_review") {
            var isLoading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            var success by remember { mutableStateOf(false) }
            var createdDip by remember { mutableStateOf("") }
            var createdCodigo by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            if (!success) {
                RegisterReviewScreen(
                    tutorData = tutorData,
                    minorData = minorData,
                    onBack = { navController.popBackStack() },
                    onConfirm = {
                        scope.launch {
                            isLoading = true
                            error = null

                            val request = org.laplaceta.placetajunior.data.RegisterRequest(
                                nombre = minorData.nombre,
                                apellidos = minorData.apellidos,
                                fecha_nacimiento = minorData.fechaNacimiento,
                                nombre_tutor = tutorData.nombre,
                                apellidos_tutor = tutorData.apellidos,
                                dni_tutor = tutorData.dni,
                                email = tutorData.email,
                                fecha_nacimiento_tutor = tutorData.fechaNacimiento,
                                tutor_ya_existe = tutorData.yaTienePlacetaID
                            )

                            val response = ApiClient.register(request)
                            isLoading = false

                            if (response.success) {
                                createdDip = response.dip ?: ""
                                createdCodigo = response.placetaid_codigo
                                success = true
                            } else {
                                error = response.message ?: "Error al registrar"
                            }
                        }
                    },
                    isLoading = isLoading,
                    error = error
                )
            } else {
                // After registration, go to legal docs signing
                RegisterSuccessScreen(
                    dip = createdDip,
                    placetaidCodigo = createdCodigo,
                    onIrAlLogin = {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("legal_docs") {
            LegalDocsScreen(
                tutorDip = tutorData.dni,
                tutorNombre = "${tutorData.nombre} ${tutorData.apellidos}",
                minorNombre = "${minorData.nombre} ${minorData.apellidos}",
                minorDip = "",
                onBack = { navController.popBackStack() },
                onAllSigned = {
                    navController.navigate("register_success") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("register_success") {
            RegisterSuccessScreen(
                dip = "",
                placetaidCodigo = null,
                onIrAlLogin = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "dashboard/{dip}",
            arguments = listOf(navArgument("dip") { type = NavType.StringType })
        ) { backStackEntry ->
            val dip = backStackEntry.arguments?.getString("dip") ?: ""
            DashboardScreen(
                dip = dip,
                saldo = currentSaldo,
                nombre = currentNombre,
                onLogout = {
                    currentDip = ""
                    currentSaldo = 0
                    SessionManager.logout()
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAcademia = { navController.navigate("academy/$dip") },
                onMonedero = { navController.navigate("monedero/$dip") },
                onProgreso = { navController.navigate("progreso/$dip") },
                onAmigos = { navController.navigate("friends/$dip") }
            )
        }

        composable(
            route = "academy/{dip}",
            arguments = listOf(navArgument("dip") { type = NavType.StringType })
        ) { backStackEntry ->
            val dip = backStackEntry.arguments?.getString("dip") ?: ""
            AcademyScreen(
                dip = dip,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "monedero/{dip}",
            arguments = listOf(navArgument("dip") { type = NavType.StringType })
        ) { backStackEntry ->
            val dip = backStackEntry.arguments?.getString("dip") ?: ""
            MonederoScreen(
                dip = dip,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "progreso/{dip}",
            arguments = listOf(navArgument("dip") { type = NavType.StringType })
        ) { backStackEntry ->
            val dip = backStackEntry.arguments?.getString("dip") ?: ""
            ProgresoScreen(
                dip = dip,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "friends/{dip}",
            arguments = listOf(navArgument("dip") { type = NavType.StringType })
        ) { backStackEntry ->
            val dip = backStackEntry.arguments?.getString("dip") ?: ""
            FriendsScreen(
                dip = dip,
                saldo = currentSaldo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
