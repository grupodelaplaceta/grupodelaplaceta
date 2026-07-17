package org.laplaceta.placetaid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.laplaceta.placetaid.ui.screens.*
import org.laplaceta.placetaid.ui.theme.*
import org.laplaceta.placetaid.viewmodel.MainViewModel
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // System splash - must be installed before super.onCreate()
            installSplashScreen()
            super.onCreate(savedInstanceState)

            // Request notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Store reference for biometric prompts
            PlacetaIDApp.instance?.currentActivity = this

            setContent {
                PlacetaIDTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = PlacetaidBackground
                    ) {
                        AppContent(viewModel = viewModel)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "onCreate crashed", e)
            // Show error and finish
            android.widget.Toast.makeText(this, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (PlacetaIDApp.instance?.currentActivity == this) {
            PlacetaIDApp.instance?.currentActivity = null
        }
    }
}

@Composable
private fun AppContent(viewModel: MainViewModel = viewModel()) {
    var showSplash by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf("placetaids") }
    var showAddScreen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val placetaids by viewModel.placetaids.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val history by viewModel.history.collectAsState()
    val selectedDip by viewModel.selectedDip.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isDeviceRegistered by viewModel.isDeviceRegistered.collectAsState()
    val navAction by viewModel.navAction.collectAsState()

    val scope = rememberCoroutineScope()

    // ── Fetch linked minors for ALL registered identities ──────────
    var menores by remember { mutableStateOf<List<org.laplaceta.placetaid.data.models.JuniorInfo>>(emptyList()) }

    LaunchedEffect(placetaids) {
        if (placetaids.isEmpty()) {
            menores = emptyList()
            return@LaunchedEffect
        }
        val dips = placetaids.map { it.dip }.filter { it.length >= 8 }
        val todosLosMenores = mutableListOf<org.laplaceta.placetaid.data.models.JuniorInfo>()
        try {
            for (dip in dips) {
                try {
                    val json = withContext(Dispatchers.IO) {
                        java.net.URL("${org.laplaceta.placetaid.data.api.ApiClient.getAdminUrl()}/api/junior/menores/$dip").readText()
                    }
                    val arr = org.json.JSONArray(json)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        todosLosMenores.add(org.laplaceta.placetaid.data.models.JuniorInfo(
                            id = obj.optString("id", null),
                            dip = obj.optString("dip", ""),
                            alias = obj.optString("alias", ""),
                            tutorDip = obj.optString("tutor_dip", null),
                            cuentaBanco = obj.optString("cuenta_banco", null),
                            nombre = obj.optString("nombre", null),
                            apellidos = obj.optString("apellidos", null),
                            creadoEn = obj.optString("creado_en", null)
                        ))
                    }
                } catch (_: Exception) { /* skip this DIP */ }
            }
            menores = todosLosMenores.distinctBy { it.dip }
        } catch (_: Exception) {
            menores = emptyList()
        }
    }

    // Show error snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Show auth result
    LaunchedEffect(navAction) {
        when (navAction) {
            "authorized" -> {
                snackbarHostState.showSnackbar(
                    message = "✓ Solicitud autorizada correctamente",
                    duration = SnackbarDuration.Short
                )
            }
            "denied" -> {
                snackbarHostState.showSnackbar(
                    message = "✗ Solicitud denegada",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    if (showSplash) {
        SplashScreen(onSplashComplete = { showSplash = false })
    } else if (showAddScreen) {
        AddPlacetaIDScreen(
            onBack = { showAddScreen = false },
            onPlacetaIDAdded = { info ->
                viewModel.addPlacetaID(info)
                showAddScreen = false
            },
            onError = { msg ->
                scope.launch {
                    snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
                }
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { scaffoldPadding ->
            MainScreen(
                currentTab = currentTab,
                onTabChange = { currentTab = it },
                onAddPlacetaID = { showAddScreen = true },
                placetaidsContent = {
                    PlacetaIDsScreen(
                        placetaids = placetaids,
                        menores = menores,
                        selectedDip = selectedDip,
                        onSelect = { viewModel.selectDip(it) },
                        onRemove = { dip ->
                            viewModel.removePlacetaID(dip)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "PlacetaID eliminado",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onAdd = { showAddScreen = true }
                    )
                },
                requestsContent = {
                    RequestsScreen(
                        requests = pendingRequests,
                        isLoading = isLoading,
                        onAuthorize = { request ->
                            // Trigger biometric prompt before authorizing
                            performBiometricAuth(
                                title = "Autorizar acceso",
                                subtitle = "${request.servicio} · ${request.codigo}",
                                onSuccess = {
                                    viewModel.authorizeRequest(request.id, request.dip, true)
                                },
                                onError = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = msg,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        },
                        onDeny = { request ->
                            viewModel.authorizeRequest(request.id, request.dip, false)
                        },
                        onRefresh = { viewModel.loadPendingRequests() }
                    )
                },
                historyContent = {
                    HistoryScreen(
                        placetaids = placetaids,
                        selectedDip = selectedDip,
                        history = history,
                        onSelectDip = { viewModel.selectDip(it) }
                    )
                },
                documentsContent = {
                    val docDips = placetaids.map { it.dip }.filter { it.length >= 8 }
                    var docs by remember { mutableStateOf<List<org.laplaceta.placetaid.data.models.MultiDocumentoResponse>>(emptyList()) }
                    var docsLoading by remember { mutableStateOf(false) }

                    fun loadDocs() {
                        scope.launch {
                            docsLoading = true
                            try {
                                val api = org.laplaceta.placetaid.data.api.ApiClient.getApi()
                                val resp = withContext(Dispatchers.IO) {
                                    api.getMultiDocumentos(mapOf("dips" to docDips))
                                }
                                docs = if (resp.isSuccessful) resp.body() ?: emptyList() else emptyList()
                            } catch (_: Exception) { docs = emptyList() }
                            docsLoading = false
                        }
                    }

                    LaunchedEffect(currentTab) {
                        if (currentTab == "documents") loadDocs()
                    }

                    DocumentsScreen(
                        documentos = docs,
                        dips = docDips,
                        isLoading = docsLoading,
                        onRefresh = { loadDocs() },
                        onNavigateToMulti = { currentTab = "placetaids" }
                    )
                },
                settingsContent = {
                    SettingsScreen(
                        isDeviceRegistered = isDeviceRegistered,
                        deviceName = PlacetaIDApp.instance?.store?.deviceName,
                        registeredDip = PlacetaIDApp.instance?.store?.registeredDip,
                        onUnregister = {
                            viewModel.removePlacetaID(selectedDip ?: return@SettingsScreen)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Dispositivo desvinculado",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onClearAll = {
                            PlacetaIDApp.instance?.store?.clearAll()
                            viewModel.loadPlacetaIDs()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Datos locales eliminados",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            )
        }

        // Load pending requests when tab changes to requests
        LaunchedEffect(currentTab) {
            if (currentTab == "requests") {
                viewModel.loadPendingRequests()
            }
        }

        // Auto-load history when selected dip changes
        LaunchedEffect(selectedDip) {
            if (currentTab == "history" && selectedDip != null) {
                viewModel.loadHistory(selectedDip!!)
            }
        }
    }
}

/**
 * Triggers a biometric authentication prompt.
 */
private fun performBiometricAuth(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val activity = PlacetaIDApp.instance?.currentActivity ?: run {
        onError("Actividad no disponible")
        return
    }

    val executor = Executors.newSingleThreadExecutor()
    try {
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autenticación fallida. Intenta de nuevo.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Usa tu huella dactilar o reconocimiento facial")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    } catch (e: SecurityException) {
        onError("Biometría no disponible en este dispositivo")
    } catch (e: Exception) {
        onError("Error al iniciar autenticación: ${e.message}")
    }
}
