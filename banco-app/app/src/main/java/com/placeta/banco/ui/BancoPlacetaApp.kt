package com.placeta.banco.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.Tag
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import java.io.File
import android.widget.RemoteViews
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AssuredWorkload
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.placeta.banco.BuildConfig
import com.placeta.banco.R
import com.placeta.banco.PlacezumWidgetProvider
import com.placeta.banco.WidgetDataManager
import com.placeta.banco.core.AGLDP_ID
import com.placeta.banco.core.Account
import com.placeta.banco.core.AccountHolder
import com.placeta.banco.core.AccountKind
import com.placeta.banco.core.AccountType
import com.placeta.banco.core.ComplianceFlag
import com.placeta.banco.core.DigitalCard
import com.placeta.banco.core.DigitalDocument
import com.placeta.banco.core.DocumentKind
import com.placeta.banco.core.DonationReward
import com.placeta.banco.core.DonationRewardDestination
import com.placeta.banco.core.DonationRewardStatus
import com.placeta.banco.core.EconomyEngine
import com.placeta.banco.core.EconomyResult
import com.placeta.banco.core.FOUNDATION_RBU_ID
import com.placeta.banco.core.IbanGdlp
import com.placeta.banco.core.InvestmentHolding
import com.placeta.banco.core.InvestmentOperation
import com.placeta.banco.core.LedgerTransaction
import com.placeta.banco.core.MemberTier
import com.placeta.banco.core.PayrollContract
import com.placeta.banco.core.PayrollContractStatus
import com.placeta.banco.core.PayrollFrequency
import com.placeta.banco.core.PayrollPeriod
import com.placeta.banco.core.PayrollPeriodStatus
import com.placeta.banco.core.PayrollSalaryChange
import com.placeta.banco.core.PlacezumCode
import com.placeta.banco.core.PromoAction
import com.placeta.banco.core.PromoSlide
import com.placeta.banco.core.Role
import com.placeta.banco.core.SavedContact
import com.placeta.banco.core.SignedProductContract
import com.placeta.banco.core.TGLP_ID
import com.placeta.banco.core.TransactionKind
import com.placeta.banco.core.TransactionStatus
import com.placeta.banco.core.TreasuryConfig
import com.placeta.banco.core.UserProfile
import com.placeta.banco.core.UserModulePreferences
import com.placeta.banco.core.TrainingModule
import com.placeta.banco.core.TrainingModuleId
import com.placeta.banco.core.TrainingQuestion
import com.placeta.banco.core.TrainingProgress
import com.placeta.banco.data.BankRepository
import com.placeta.banco.data.ConnectionState

import com.placeta.banco.data.UpdateCheckResult
import com.placeta.banco.data.checkBackendVersion
import com.placeta.banco.data.seedRegularization
import com.placeta.banco.documents.PdfExporter
import com.placeta.banco.nfc.NfcPlacezumCard
import com.placeta.banco.nfc.PlacezumNfcProtocol
import com.placeta.banco.nfc.PlacezumNfcSession
import com.placeta.banco.notifications.AppNotificationState
import com.placeta.banco.notifications.PlacetaNotifications
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

private val PremiumPurple = Color(0xFF3F00D8)
private val TributosPurple = Color(0xFF3F00D8)
private val DeepPurple = Color(0xFF000000)
private val SurfacePurple = Color(0xFFFFFFFF)
private val LightBackground = Color(0xFFFFFFFF)
private val LightSurface = Color(0xFFFFFFFF)
private val Ink = Color(0xFF000000)
private val SoftPurple = Color(0xFF3F00D8)
private val Gold = Color(0xFFFFB300)
private val Mint = Color(0xFF00A884)
private val ErrorRed = Color(0xFFE00022)
private val IncomeGreen = Color(0xFF00A651)
private val HistoryDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm")
private const val MODULE_SOCIETIES = "societies"
private const val MODULE_STATE_TOOLS = "stateTools"
private const val MODULE_TRAINING = "training"
private val OptionalModuleIds = setOf(MODULE_STATE_TOOLS, MODULE_TRAINING)
private const val PLACETA_ID_PREFS = "placeta_id_auth"
private const val KEY_PLACETA_ID_STATE = "pending_state"
private const val KEY_PLACETA_ID_CODE_VERIFIER = "pending_code_verifier"
private const val MAX_VIRTUAL_CARDS_PER_ACCOUNT = 3

private data class InvestmentReveal(
    val userWins: Boolean,
    val assetName: String,
    val amountPz: Long,
    val movementPercent: Int
)

private data class InvestmentResultRow(
    val sell: LedgerTransaction,
    val assetName: String,
    val principalPz: Long,
    val returnedPz: Long,
    val netResultPz: Long,
    val movementPercent: Int,
    val won: Boolean
)

@Composable
fun BancoPlacetaApp(
    repository: BankRepository,
    initialTab: String = "",
    placetaIdCallbackUri: String? = null,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val outfit = FontFamily(
        Font(R.font.outfit_light, FontWeight.Light),
        Font(R.font.outfit_regular, FontWeight.Normal),
        Font(R.font.outfit_bold, FontWeight.Bold),
        Font(R.font.outfit_black, FontWeight.Black),
        Font(R.font.outfit_black, FontWeight.ExtraBold)
    )
    val base = MaterialTheme.typography
    val typography = base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = outfit, fontWeight = FontWeight.Black),
        displayMedium = base.displayMedium.copy(fontFamily = outfit, fontWeight = FontWeight.Black),
        displaySmall = base.displaySmall.copy(fontFamily = outfit, fontWeight = FontWeight.Black),
        headlineLarge = base.headlineLarge.copy(fontFamily = outfit, fontWeight = FontWeight.Black),
        headlineMedium = base.headlineMedium.copy(fontFamily = outfit, fontWeight = FontWeight.Black),
        headlineSmall = base.headlineSmall.copy(fontFamily = outfit, fontWeight = FontWeight.Black),
        titleLarge = base.titleLarge.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = outfit),
        bodyMedium = base.bodyMedium.copy(fontFamily = outfit),
        bodySmall = base.bodySmall.copy(fontFamily = outfit),
        labelLarge = base.labelLarge.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontFamily = outfit, fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(fontFamily = outfit, fontWeight = FontWeight.Bold)
    )

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PremiumPurple,
            onPrimary = Color.White,
            primaryContainer = SurfacePurple,
            onPrimaryContainer = DeepPurple,
            secondary = Mint,
            onSecondary = DeepPurple,
            secondaryContainer = Mint.copy(alpha = 0.18f),
            tertiary = Gold,
            onTertiary = DeepPurple,
            background = LightBackground,
            onBackground = Ink,
            surface = LightSurface,
            onSurface = Ink,
            surfaceVariant = SurfacePurple,
            onSurfaceVariant = TributosPurple,
            outline = PremiumPurple.copy(alpha = 0.24f),
            outlineVariant = PremiumPurple.copy(alpha = 0.12f),
            error = ErrorRed
        ),
        typography = typography
    ) {
        ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
        Surface(modifier = Modifier.fillMaxSize(), color = LightBackground) {
            val connection by repository.connection
            val connectionMessage by repository.connectionMessage
            val activeUser by repository.activeUser
            val promoSlides by repository.promoSlides
            val context = LocalContext.current
            var networkOnline by remember { mutableStateOf(context.hasNetworkConnection()) }
            var showOfflineOverlay by remember { mutableStateOf(false) }
            var updateCheck by remember { mutableStateOf<UpdateCheckResult>(UpdateCheckResult.Checking) }
            val initialLoading = updateCheck is UpdateCheckResult.Checking || (connection == ConnectionState.Loading && activeUser == null)
            var showResumeLoading by remember { mutableStateOf(false) }
            val offlineSignal = !networkOnline || connection == ConnectionState.Offline
            val appBlocked = updateCheck !is UpdateCheckResult.Allowed

            LaunchedEffect(Unit) {
                updateCheck = checkBackendVersion()
                while (true) {
                    networkOnline = context.hasNetworkConnection()
                    delay(1_000)
                }
            }
            LaunchedEffect(connection) {
                if (connection == ConnectionState.Loading && activeUser != null && !initialLoading) {
                    showResumeLoading = true
                } else if (connection == ConnectionState.Online && showResumeLoading) {
                    delay(400)
                    showResumeLoading = false
                }
            }
            LaunchedEffect(networkOnline) {
                if (networkOnline) repository.refresh()
            }
            LaunchedEffect(offlineSignal) {
                if (offlineSignal) {
                    delay(1_000)
                    showOfflineOverlay = true
                } else {
                    delay(1_600)
                    showOfflineOverlay = false
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (initialLoading || showResumeLoading) {
                    LoadingGifScreen()
                } else if (updateCheck is UpdateCheckResult.Maintenance) {
                    val maintenance = updateCheck as UpdateCheckResult.Maintenance
                    StatusGifScreen(
                        asset = "serverfail.gif",
                        title = "Banco en mantenimiento",
                        message = maintenance.message,
                        code = maintenance.code
                    )
                } else if (appBlocked) {
                    ForceUpdateScreen()
                } else if (activeUser?.banned == true) {
                    StatusGifScreen(
                        asset = "banned.gif",
                        title = "Acceso bloqueado",
                        message = "Tu usuario no puede usar ahora los servicios del Grupo de La Placeta.",
                        code = "PLID-BAN-001"
                    )
                } else if (activeUser != null) {
                    val needsTributos = activeUser?.tributosCensusDate == null
                    if (needsTributos) {
                        TributosCensoPopup(
                            accounts = accounts.values.toList(),
                            activeUser = activeUser!!,
                            onRegister = { eip -> repository.registerTributosContribuyente(eip) }
                        )
                    } else {
                        BancoHome(repository, activeUser!!, initialTab, authenticateTransfer)
                    }
                } else {
                    LoginScreen(
                        connection = connection,
                        promos = promoSlides,
                        placetaIdCallbackUri = placetaIdCallbackUri,
                        onPlacetaIdLogin = repository::loginWithPlacetaId,
                        onRetry = repository::refresh,
                    )
                }
                AnimatedVisibility(
                    visible = showOfflineOverlay && !appBlocked,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NoInternetOverlay(message = connectionMessage, onRetry = repository::refresh)
                }
            }
        }
        }
    }
}

@Composable
private fun LoadingGifScreen() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#3f00d8"))
                settings.javaScriptEnabled = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }.also { it.loadGifAsset("loading2.gif", "#3f00d8") }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Carga un GIF desde assets mediante base64 embebido en HTML.
 * Compatible con Android 10+ (scoped storage) y todas las versiones.
 */
private fun WebView.loadGifAsset(assetName: String, bgColor: String) {
    try {
        setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        val inputStream = context.assets.open(assetName)
        val bytes = inputStream.readBytes()
        inputStream.close()
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val mime = if (assetName.endsWith(".gif", true)) "image/gif" else "image/png"
        val html = """
            <html><body style='margin:0;background:$bgColor;display:flex;align-items:center;justify-content:center;height:100vh'>
            <img src="data:$mime;base64,$base64" style='max-width:100%;max-height:100%;object-fit:contain' />
            </body></html>
        """.trimIndent()
        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Log.e("GifWebView", "Error cargando GIF $assetName", e)
        loadDataWithBaseURL(null, "<html><body style='margin:0;background:$bgColor;display:flex;align-items:center;justify-content:center;height:100vh'><p style='color:white;font-size:18px;font-family:sans-serif'>⚡ Banco de La Placeta</p></body></html>", "text/html", "UTF-8", null)
    }
}

@Composable
private fun ForceUpdateScreen() {
    val context = LocalContext.current
    StatusGifScreen(
        asset = "update.gif",
        title = "Actualización necesaria",
        message = "Hay una versión nueva de Banco de La Placeta. Actualiza la app para seguir usando el servicio.",
        code = "BPL-UPD-001",
        actionLabel = "Abrir Play Store",
        onAction = {
            val packageName = context.packageName
            val marketUri = Uri.parse("market://details?id=$packageName")
            val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, marketUri)) }
                .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, webUri)) }
        }
    )
}

@Composable
private fun StatusGifScreen(
    asset: String,
    title: String,
    message: String,
    code: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                }.also { it.loadGifAsset(asset, "#000000") }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Rectángulo redondeado inferior con el error
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .background(Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(message, color = Color(0xFF4C4057), style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple)
                ) { Text(actionLabel, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LoginScreen(
    connection: ConnectionState,
    promos: List<PromoSlide>,
    placetaIdCallbackUri: String?,
    onPlacetaIdLogin: (String, String) -> String?,
    onRetry: () -> Unit,
) {
    val fallbackSlides = remember {
        listOf(
            PromoSlide("promo-1", "BANCO DE LA PLACETA", "Tu centro financiero seguro, claro y siempre a mano.", PromoAction.Login, "bank"),
            PromoSlide("promo-2", "PLACETAID", "Identidad segura para acceder al banco.", PromoAction.Login, "placezum"),
            PromoSlide("promo-3", "MERCADO GDLP", "Invierte, revisa movimientos y descarga tus documentos fiscales.", PromoAction.Login, "market")
        )
    }
    val loginHaptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val promoFallbackAssets = remember(context) { context.localPromoFallbackAssets() }
    val shuffledPromoFallbackAssets = remember(promoFallbackAssets) {
        promoFallbackAssets.shuffled(Random(System.currentTimeMillis()))
    }
    val slides = remember(promos, shuffledPromoFallbackAssets) {
        promos.ifEmpty { fallbackSlides }.withPromoAssetFallbacks(shuffledPromoFallbackAssets)
    }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val offline = connection == ConnectionState.Offline
    var handledCallback by remember { mutableStateOf<String?>(null) }

    fun startPlacetaId() {
        val state = UUID.randomUUID().toString()
        val codeVerifier = generatePkceVerifier()
        context.getSharedPreferences(PLACETA_ID_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PLACETA_ID_STATE, state)
            .putString(KEY_PLACETA_ID_CODE_VERIFIER, codeVerifier)
            .apply()
        val url = Uri.parse(BuildConfig.PLACETA_ID_BASE_URL).buildUpon()
            .appendQueryParameter("client_id", BuildConfig.PLACETA_ID_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.PLACETA_ID_CALLBACK_URL)
            .appendQueryParameter("platform", "android")
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", pkceChallenge(codeVerifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }

    LaunchedEffect(placetaIdCallbackUri, connection) {
        val raw = placetaIdCallbackUri ?: return@LaunchedEffect
        if (handledCallback == raw) return@LaunchedEffect
        handledCallback = raw
        val uri = Uri.parse(raw)
        val params = uri.callbackParams()
        val error = params["error"].orEmpty()
        if (error.isNotBlank()) {
            snackbar.showSnackbar("PlacetaID: $error")
            return@LaunchedEffect
        }
        val prefs = context.getSharedPreferences(PLACETA_ID_PREFS, Context.MODE_PRIVATE)
        val returnedState = params["state"].orEmpty()
        val expectedState = prefs.getString(KEY_PLACETA_ID_STATE, null).orEmpty()
        if (returnedState.isNotBlank() && expectedState.isNotBlank() && returnedState != expectedState) {
            snackbar.showSnackbar("PlacetaID rechazado: state inválido")
            return@LaunchedEffect
        }
        val directToken = params.placetaToken()
        val exchanged = if (directToken.isBlank()) {
            val code = params["code"].orEmpty()
            val verifier = prefs.getString(KEY_PLACETA_ID_CODE_VERIFIER, null).orEmpty()
            if (code.isNotBlank()) {
                withContext(Dispatchers.IO) { exchangePlacetaIdCode(code, verifier) }
            } else {
                null
            }
        } else {
            null
        }
        prefs.edit()
            .remove(KEY_PLACETA_ID_STATE)
            .remove(KEY_PLACETA_ID_CODE_VERIFIER)
            .apply()
        val token = directToken.ifBlank { exchanged?.first.orEmpty() }
        val user = params.placetaUserJson()
            .ifBlank { exchanged?.second.orEmpty() }
            .ifBlank { jwtPayloadJson(token).orEmpty() }
        if (token.isBlank()) {
            snackbar.showSnackbar("PlacetaID no devolvió token de sesión")
            return@LaunchedEffect
        }
        if (user.isBlank()) {
            snackbar.showSnackbar("PlacetaID no devolvió datos de usuario")
            return@LaunchedEffect
        }
        val loginError = withContext(Dispatchers.IO) { onPlacetaIdLogin(token, user) }
        if (loginError != null) {
            snackbar.showSnackbar(loginError)
        } else {
            snackbar.showSnackbar("Sesión verificada para ${placetaDisplayName(user)}")
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        loginHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(slides.map { "${it.id}:${it.imageKey}:${it.imageUrl}:${it.assetPath}" }, offline) {
        if (!offline) {
            withContext(Dispatchers.IO) {
                PromoImagePreloader.preload(context, slides)
            }
        }
    }

    LaunchedEffect(slides.map { "${it.id}:${it.title}:${it.imageKey}:${it.imageUrl}:${it.assetPath}" }, offline) {
        if (slides.size > 1 && !offline) {
            while (true) {
                delay(5_000)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % slides.size)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val slide = slides[page]
                key(slide.id, slide.imageKey, slide.imageUrl, slide.assetPath) {
                    PromoBackgroundImage(
                        slide = slide,
                        offline = offline,
                        contentDescription = if (offline) "Sin conexión" else "Promo Banco de La Placeta",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPageIndicator(pageCount = slides.size, currentPage = pagerState.currentPage)
                if (offline) {
                    Text("Sin conexión. Revisa internet y vuelve a intentarlo.", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { startPlacetaId() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("Continuar con PlacetaID", fontWeight = FontWeight.Black)
                        Text("DIP + 2FA · sesión privada", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE8D9FF))
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp))
    }
}

@Composable
private fun HorizontalPageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (selected) 28.dp else 12.dp, height = 6.dp)
                    .background(if (selected) PremiumPurple else PremiumPurple.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun PromoBackgroundImage(
    slide: PromoSlide,
    offline: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(slide.id, slide.imageKey, slide.imageUrl, slide.assetPath, offline) {
        mutableStateOf<ImageBitmap?>(PromoImagePreloader.get(slide)?.asImageBitmap())
    }
    LaunchedEffect(slide.id, slide.imageKey, slide.imageUrl, slide.assetPath, offline) {
        bitmap = PromoImagePreloader.get(slide)?.asImageBitmap()
        if (!offline) {
            bitmap = withContext(Dispatchers.IO) {
                PromoImagePreloader.getOrLoad(context, slide)?.asImageBitmap()
            }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(slide.imageResource()),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun PromoSlide.imageResource(): Int = when (imageKey) {
    "placezum" -> R.drawable.login_slide_2
    "market" -> R.drawable.login_slide_3
    else -> R.drawable.login_slide_1
}

private fun Context.localPromoFallbackAssets(): List<String> =
    assets.list("promos")
        ?.filter { Regex("^promo\\d+\\.(png|jpg|jpeg|webp)$", RegexOption.IGNORE_CASE).matches(it) }
        ?.sortedWith(compareBy({ it.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }, { it }))
        ?.map { "promos/$it" }
        ?.ifEmpty { listOf("promos/promo1.png", "promos/promo2.png") }
        ?: listOf("promos/promo1.png", "promos/promo2.png")

private fun List<PromoSlide>.withPromoAssetFallbacks(fallbackAssets: List<String>): List<PromoSlide> {
    val assets = fallbackAssets.ifEmpty { listOf("promos/promo1.png", "promos/promo2.png", "promos/promo3.png") }
    return mapIndexed { index, slide ->
        slide.copy(assetPath = assets[index % assets.size])
    }
}

private fun PromoAction.label(): String = when (this) {
    PromoAction.Login -> "ACCEDER"
    PromoAction.Register -> "DARME DE ALTA"
    PromoAction.Demo -> "DEMO"
}

private fun PromoAction.formTitle(): String = when (this) {
    PromoAction.Login -> "Acceder"
    PromoAction.Register -> "Crear acceso"
    PromoAction.Demo -> "Demo"
}

private fun PromoAction.submitLabel(): String = when (this) {
    PromoAction.Login -> "LOGIN"
    PromoAction.Register -> "CREAR CUENTA"
    PromoAction.Demo -> "DEMO"
}

@Composable
private fun NoInternetOverlay(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#3f00d8"))
                    settings.javaScriptEnabled = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                }.also { it.loadGifAsset("nowifi.gif", "#3f00d8") }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Rectángulo redondeado inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .background(Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Sin conexión", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(message.ifBlank { "Revisa tu conexión a internet y vuelve a intentarlo." }, color = Color(0xFF4C4057), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                Spacer(Modifier.size(8.dp))
                Text("REINTENTAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BottomSlideUpOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f))
                        .clickable(onClick = onDismiss)
                )
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 260f),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(200))
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun BancoHome(
    repository: BankRepository,
    activeUser: UserProfile,
    initialTab: String,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val config by repository.treasuryConfig
    val engine = remember(config) { EconomyEngine(config) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by remember(initialTab) { mutableStateOf(AppTab.fromKey(initialTab)) }
    val users by repository.users
    val accounts by repository.accounts
    val transactions by repository.transactions
    val holdings by repository.investmentHoldings
    val investmentOperations by repository.investmentOperations
    val digitalCards by repository.digitalCards
    val savedContacts by repository.savedContacts
    val payrollContracts by repository.payrollContracts
    val payrollPeriods by repository.payrollPeriods
    val donationRewards by repository.donationRewards
    val promoSlides by repository.promoSlides
    val flags by repository.complianceFlags
    val userModulePreferences by repository.userModulePreferences
    val accountHolders by repository.accountHolders
    val foundation = accounts[FOUNDATION_RBU_ID] ?: accounts[AGLDP_ID]
    val context = LocalContext.current
    val compactScreen = LocalConfiguration.current.screenWidthDp < 380
    val ownedAccounts = accounts.values
        .filter { account ->
            account.isActiveAccount() && (
                account.isOwnedBy(activeUser) ||
                    accountHolders.any { holder ->
                        holder.accountId == account.id &&
                            holder.placetaId.normalizedOwnerId() == activeUser.placetaId.normalizedOwnerId()
                    }
            )
        }
        .sortedBy { it.type.ordinal }
    val citizenAccounts = ownedAccounts
        .filter { it.role == Role.Citizen }
        .sortedBy { it.type.ordinal }
    val businessAccounts = ownedAccounts.filter { it.type == AccountType.Business }
    val stateAccounts = ownedAccounts.filter { it.type == AccountType.State || it.role == Role.Tributos }
    val modulePreferences = userModulePreferences.firstOrNull {
        it.placetaId.normalizedOwnerId() == activeUser.placetaId.normalizedOwnerId()
    }
    val hiddenModules = modulePreferences?.hiddenModules?.toSet() ?: OptionalModuleIds
    fun moduleEnabled(id: String): Boolean = id !in hiddenModules
    fun setModuleEnabled(id: String, enabled: Boolean) {
        val currentHidden = (modulePreferences?.hiddenModules?.toSet() ?: OptionalModuleIds).toMutableSet()
        if (enabled) currentHidden.remove(id) else currentHidden.add(id)
        repository.upsertUserModulePreferences(
            UserModulePreferences(
                placetaId = activeUser.placetaId,
                hiddenModules = currentHidden.toList().sorted()
            )
        )
    }
    var selectedAccountId by remember(activeUser.primaryAccountId) { mutableStateOf(activeUser.primaryAccountId) }
    var moreOpen by remember { mutableStateOf(false) }
    var moreProductOpen by remember { mutableStateOf(false) }
    var moreDonationAmount by remember { mutableStateOf("25") }
    var storeDonationAmount by remember { mutableStateOf("25") }
    if (citizenAccounts.isNotEmpty() && citizenAccounts.none { it.id == selectedAccountId }) {
        selectedAccountId = citizenAccounts.firstOrNull { it.id == activeUser.primaryAccountId }?.id ?: citizenAccounts.first().id
    }
    val availableTabs = remember(activeUser.primaryAccountId, accounts, hiddenModules, businessAccounts.size, stateAccounts.size) {
        if (accounts[activeUser.primaryAccountId]?.role == Role.Administracion) {
            AppTab.entries
        } else {
            buildList {
                add(AppTab.Home)
                add(AppTab.Placezum)
                if (ownedAccounts.any { it.type == AccountType.Investment || it.type == AccountType.Business }) add(AppTab.Inversiones)
                if (moduleEnabled(MODULE_SOCIETIES)) add(AppTab.Sociedades)
                if (moduleEnabled(MODULE_STATE_TOOLS) && stateAccounts.isNotEmpty()) add(AppTab.Estatal)
                add(AppTab.Hub)
            }
        }
    }
    if (tab !in availableTabs) tab = AppTab.Home
    LaunchedEffect(tab) {
        AppNotificationState.currentTab = tab.name
    }

    DisposableEffect(accounts, transactions) {
        val primary = ownedAccounts.firstOrNull()
        val lastTx = transactions
            .filter { it.fromAccountId == primary?.id || it.toAccountId == primary?.id }
            .maxByOrNull { it.createdAt }
        WidgetDataManager.updateFromState(context, primary, lastTx)
        PlacezumWidgetProvider.notifyDataChanged(context)
        onDispose { }
    }

    fun show(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun apply(result: EconomyResult<*>) {
        repository.run(result)?.let(::show)
    }

    Scaffold(
        containerColor = LightBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFEFCFF),
                tonalElevation = 8.dp
            ) {
                availableTabs.forEach { item ->
                    val selected = item != AppTab.Hub && tab == item
                    val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, label = "nav-scale")
                    val isBusinessSelected = accounts[selectedAccountId]?.type == AccountType.Business
                    val isInvestmentSelected = accounts[selectedAccountId]?.type == AccountType.Investment
                    val tabEnabled = when (item) {
                        AppTab.Sociedades -> isBusinessSelected && businessAccounts.isNotEmpty()
                        AppTab.Inversiones -> isInvestmentSelected || isBusinessSelected
                        AppTab.Placezum -> !isBusinessSelected
                        else -> true
                    }
                    NavigationBarItem(
                        selected = selected,
                        enabled = tabEnabled,
                        onClick = {
                            if (item == AppTab.Hub) moreOpen = true
                            else if (!tabEnabled) {
                                when (item) {
                                    AppTab.Placezum -> show("PlaceZum no disponible para cuentas de empresa. Usa Sociedades.")
                                    AppTab.Sociedades -> show("Sociedades no disponible para cuentas personales. Selecciona una cuenta de empresa.")
                                    AppTab.Inversiones -> show("Selecciona una cuenta de inversión o empresa para acceder a Inversiones.")
                                    else -> show("Módulo no disponible")
                                }
                            } else tab = item
                        },
                        icon = {
                            Box {
                                Icon(item.icon, contentDescription = item.title, modifier = Modifier.scale(iconScale))
                                if (!tabEnabled) {
                                    Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = Color(0xFF756781).copy(alpha = 0.5f), modifier = Modifier.size(12.dp).align(Alignment.BottomEnd))
                                }
                            }
                        },
                        label = { Text(item.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PremiumPurple,
                            indicatorColor = PremiumPurple,
                            unselectedIconColor = Color(0xFF756781),
                            unselectedTextColor = Color(0xFF756781)
                        )
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (compactScreen) 8.dp else 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header(accounts, activeUser, repository::logout) }
            item {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val spec = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
                        (slideInVertically(spec) { it / 4 } + fadeIn(tween(250, easing = FastOutSlowInEasing)))
                            .togetherWith(slideOutVertically(spec) { -it / 4 } + fadeOut(tween(180, easing = FastOutSlowInEasing)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "tab-animated-content"
                ) { currentTab ->
                    when (currentTab) {
                        AppTab.Home -> CitizenDashboard(activeUser, accounts, selectedAccountId, { if (citizenAccounts.any { account -> account.id == it }) selectedAccountId = it }, transactions, digitalCards, config, engine, repository::upsertDigitalCard, users, savedContacts, payrollContracts, payrollPeriods, repository::upsertPayrollContract, repository::upsertPayrollPeriod, authenticateTransfer, accountHolders, ::show, ::apply)
                        AppTab.Placezum -> PlacezumScreen(activeUser, accounts, selectedAccountId, { if (citizenAccounts.any { account -> account.id == it }) selectedAccountId = it }, transactions, savedContacts, config, engine, repository::upsertSavedContact, authenticateTransfer, ::show, ::apply)
                        AppTab.Inversiones -> InversionesScreen(
                            activeUser = activeUser,
                            accounts = accounts,
                            ownedAccounts = ownedAccounts,
                            businessAccounts = businessAccounts,
                            selectedAccountId = selectedAccountId,
                            onSelectedAccount = { id -> if (accounts.values.any { it.isActiveAccount() && it.isOwnedBy(activeUser) && it.id == id }) selectedAccountId = id },
                            transactions = transactions,
                            holdings = holdings,
                            operations = investmentOperations,
                            config = config,
                            engine = engine,
                            show = ::show,
                            apply = ::apply
                        )
                        AppTab.Sociedades -> SociedadesScreen(
                            activeUser = activeUser,
                            accounts = accounts,
                            selectedAccountId = businessAccounts.firstOrNull { it.id == selectedAccountId }?.id ?: businessAccounts.firstOrNull()?.id ?: selectedAccountId,
                            onSelectedAccount = { id -> if (businessAccounts.any { account -> account.id == id }) selectedAccountId = id },
                            transactions = transactions,
                            config = config,
                            engine = engine,
                            users = users,
                            savedContacts = savedContacts,
                            payrollContracts = payrollContracts,
                            payrollPeriods = payrollPeriods,
                            upsertPayrollContract = repository::upsertPayrollContract,
                            upsertPayrollPeriod = repository::upsertPayrollPeriod,
                            authenticateTransfer = authenticateTransfer,
                            show = ::show,
                            apply = ::apply
                        )
                        AppTab.Estatal -> EstatalScreen(
                            activeUser = activeUser,
                            accounts = accounts,
                            selectedAccountId = stateAccounts.firstOrNull { it.id == selectedAccountId }?.id ?: stateAccounts.firstOrNull()?.id ?: selectedAccountId,
                            onSelectedAccount = { id -> if (stateAccounts.any { account -> account.id == id }) selectedAccountId = id },
                            transactions = transactions,
                            flags = flags,
                            config = config,
                            engine = engine,
                            show = ::show,
                            apply = ::apply
                        )
                        AppTab.Hub -> HubScreen(
                            activeUser = activeUser,
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            onSelectedAccount = { id ->
                                if (accounts.values.any { account -> account.isActiveAccount() && account.isOwnedBy(activeUser) && account.id == id }) {
                                    selectedAccountId = id
                                }
                            },
                            transactions = transactions,
                            config = config,
                            engine = engine,
                            upsertDigitalCard = repository::upsertDigitalCard,
                            users = users,
                            savedContacts = savedContacts,
                            payrollContracts = payrollContracts,
                            payrollPeriods = payrollPeriods,
                            upsertPayrollContract = repository::upsertPayrollContract,
                            upsertPayrollPeriod = repository::upsertPayrollPeriod,
                            show = ::show,
                            apply = ::apply,
                            onCreatePaymentLink = repository::createPaymentLink,
                            accountHolders = accountHolders,
                            onAddAccountHolder = repository::upsertAccountHolder,
                            onRemoveAccountHolder = repository::removeAccountHolder
                        )
                        AppTab.Escuela -> TrainingSchoolScreen(activeUser, users, ::show, ::apply)
                        AppTab.Tributos -> TributosScreen(accounts, transactions, flags, config, engine, ::show, ::apply)
                    }
                }
            }
        }
    }
    val templates by repository.productContractTemplates
    val signedContracts by repository.signedProductContracts
    val unsignedProducts = remember(accounts, digitalCards, templates, signedContracts) {
        val signed = signedContracts.filter { it.placetaId == activeUser.placetaId }.map { it.templateId }.toSet()
        val needed = templates.filterNot { it.id in signed }
        val cardsWithoutContract = digitalCards.filter { card ->
            card.accountId == activeUser.primaryAccountId && needed.any { it.productType == "card" }
        }
        val accountsWithoutContract = ownedAccounts.filter { account ->
            needed.any { it.productType == "account" }
        }
        needed.isNotEmpty() && (cardsWithoutContract.isNotEmpty() || accountsWithoutContract.isNotEmpty())
    }
    var showContractPopup by remember { mutableStateOf(false) }
    var contractProductName by remember { mutableStateOf("") }
    var contractDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(unsignedProducts, templates) {
        if (unsignedProducts && templates.isNotEmpty() && !contractDismissed) {
            contractProductName = templates.first().productType.replaceFirstChar { it.uppercase() }
            showContractPopup = true
        }
    }
    ContractRequiredPopup(
        show = showContractPopup,
        productName = contractProductName,
        onDismiss = {
            showContractPopup = false
            contractDismissed = true
        },
        onAccept = {
            val template = templates.firstOrNull()
            if (template != null) {
                val contract = SignedProductContract(
                    id = "signed-${activeUser.placetaId}-${template.id}",
                    accountId = activeUser.primaryAccountId,
                    placetaId = activeUser.placetaId,
                    templateId = template.id,
                    templateVersion = template.version,
                    signedAt = Instant.now().toString(),
                    documentId = "doc-${template.id}-${activeUser.placetaId}"
                )
                repository.upsertSignedContract(contract)
            }
            showContractPopup = false
            contractDismissed = true
            show("Contrato firmado. Producto activado.")
        },
        onViewContract = {
            val template = templates.firstOrNull()
            if (template != null) {
                show("Contrato v${template.version}: ${template.productType}. ID: ${template.id}. Cláusulas: ${template.clausesHash}")
            } else {
                show("No hay plantilla de contrato disponible. Contacta con soporte.")
            }
        }
    )

    val moreAccounts = accounts.values.filter { it.isActiveAccount() && it.isOwnedBy(activeUser) }.sortedBy { it.type.ordinal }
    val moreAccount = moreAccounts.firstOrNull { it.id == selectedAccountId }
        ?: moreAccounts.firstOrNull { it.id == activeUser.primaryAccountId }
        ?: moreAccounts.firstOrNull()
    moreAccount?.let { account ->
        BottomSlideUpOverlay(visible = moreOpen, onDismiss = { moreOpen = false }) {
            val moreDocs = contextualDocuments(
                account = account,
                transactions = transactionsForAccount(transactions, account.id),
                contracts = payrollContracts.filter { it.companyAccountId == account.id || it.employeeAccountId == account.id },
                periods = payrollPeriods.filter { it.companyAccountId == account.id || it.employeeAccountId == account.id }
            )
            MoreServicesSlideUp(
                account = account,
                documents = moreDocs,
                paymentTransactions = transactionsForAccount(transactions, account.id),
                accounts = moreAccounts,
                allAccounts = accounts,
                users = users,
                investmentTransactions = transactionsForAccount(transactions, account.id).filter { it.kind == TransactionKind.InvestmentBuy || it.kind == TransactionKind.InvestmentSell },
                digitalCards = digitalCards.filter { it.accountId in moreAccounts.map(Account::id) },
                config = config,
                engine = engine,
                payrollTargets = accounts.values.filter { it.isActiveAccount() && it.type == AccountType.Current && it.id != account.id },
                savedContacts = savedContacts,
                payrollContracts = payrollContracts.filter { it.companyAccountId == account.id },
                payrollPeriods = payrollPeriods.filter { it.companyAccountId == account.id },
                donationAmount = moreDonationAmount,
                activeUser = activeUser,
                donationRewards = donationRewards.filter { it.dip == activeUser.dip },
                moduleHidden = hiddenModules,
                hasBusinessAccess = businessAccounts.isNotEmpty(),
                hasStateAccess = stateAccounts.isNotEmpty(),
                onModuleEnabledChange = ::setModuleEnabled,
                accountHolders = accountHolders,
                onAddAccountHolder = repository::upsertAccountHolder,
                onRemoveAccountHolder = repository::removeAccountHolder,
                onDonationAmount = { moreDonationAmount = sanitizeMoneyInput(it) },
                onClose = { moreOpen = false },
                onNewProduct = {
                    moreOpen = false
                    moreProductOpen = true
                },
                onSupportTicket = { ticketInfo ->
                    val parts = ticketInfo.split(" · ")
                    val ticketId = parts.getOrElse(0) { "SUP-000000" }
                    scope.launch {
                        try {
                            val json = org.json.JSONObject().apply {
                                put("id", ticketId)
                                put("category", parts.getOrElse(1) { "General" })
                                put("priority", parts.getOrElse(2) { "Media" })
                                put("subject", parts.getOrElse(3) { "" })
                                put("message", ticketInfo)
                                put("dip", activeUser.dip)
                                put("name", activeUser.displayName)
                            }
                            val url = URL("http://127.0.0.1:18731/api/support-tickets")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.doOutput = true
                            conn.setRequestProperty("Content-Type", "application/json")
                            conn.outputStream.write(json.toString().toByteArray())
                            val code = conn.responseCode
                            conn.disconnect()
                            if (code == 200) show("✅ Ticket enviado: $ticketId")
                            else show("✅ Ticket creado localmente: $ticketId")
                        } catch (_: Exception) {
                            show("✅ Ticket creado: $ticketId (admin offline)")
                        }
                    }
                },
                onRegisterPayroll = { target, amount ->
                    apply(engine.transferPayrollOrLoan(accounts, account.id, target.id, amount, "Nómina registrada ${account.displayName} -> ${target.displayName}"))
                    show("Nómina registrada para ${target.displayName}")
                },
                onPayrollContract = repository::upsertPayrollContract,
                onPayrollPeriod = repository::upsertPayrollPeriod,
                onPayPayrollPeriod = { period ->
                    val result = engine.transferPayrollOrLoan(
                        accounts,
                        period.companyAccountId,
                        period.employeeAccountId,
                        period.grossSalaryPz,
                        "Nómina ${period.label} · DIP ${period.employeeDip}"
                    )
                    when (result) {
                        is EconomyResult.Failure -> show(result.message)
                        is EconomyResult.Success -> {
                            repository.run(result)?.let(::show)
                            repository.upsertPayrollPeriod(period.copy(status = PayrollPeriodStatus.Paid, paidAt = Instant.now(), transactionId = result.value.id))
                            val employee = accounts[period.employeeAccountId] ?: account
                            val doc = DigitalDocument("payroll-${period.id}", employee.id, "Nómina ${period.label}", DocumentKind.PayrollPayslip)
                            show(PdfExporter.createAndOpenTaxPdf(context, employee, doc, listOf(result.value), config))
                        }
                    }
                },
                onPayrollContractPdf = { contract ->
                    val employee = accounts[contract.employeeAccountId] ?: account
                    val doc = DigitalDocument("alta-${contract.id}", employee.id, "Alta laboral ${contract.employeeName}", DocumentKind.LaborContract)
                    show(PdfExporter.createAndOpenTaxPdf(context, employee, doc, emptyList(), config, contract))
                },
                onDonate = {
                    val destination = foundation
                    if (destination == null) {
                        show("Fundación no disponible")
                    } else {
                        apply(engine.transferByIban(accounts, account.id, destination.iban, parseMoneyInput(moreDonationAmount), TransactionKind.Donation, "Donación a Fundación Banco de La Placeta para RBU"))
                        moreOpen = false
                        show("✅ Donación realizada: ${formatPz(parseMoneyInput(moreDonationAmount))} Pz enviados a la Fundación. Código: BPL-DON-001")
                    }
                },
                onDonationReward = repository::upsertDonationReward,
                onDonatePointsToFoundation = { points ->
                    val destination = foundation
                    if (destination == null) {
                        show("Fundación no disponible")
                    } else {
                        apply(engine.transferByIban(accounts, AGLDP_ID, destination.iban, points, TransactionKind.Donation, "Conversión de puntos de donación Stripe a Placetas para Fundación"))
                    }
                },
                onDocument = { doc ->
                    show(PdfExporter.createAndOpenTaxPdf(context, account, doc, transactionsForAccount(transactions, account.id), config))
                },
                onTransactionReceipt = { transaction ->
                    val doc = DigitalDocument("transfer-${transaction.id}", account.id, "Justificante ${transaction.kind.name} · ${formatPz(transaction.amountPz)} Pz", DocumentKind.PaymentReceipt)
                    show(PdfExporter.createAndOpenTaxPdf(context, account, doc, listOf(transaction), config))
                },
                onCreatePaymentLink = repository::createPaymentLink
            )
        }
        BottomSlideUpOverlay(visible = moreProductOpen, onDismiss = { moreProductOpen = false }) {
            ProductSlideUp(
                account = account,
                accounts = accounts,
                activeUser = activeUser,
                config = config,
                engine = engine,
                onClose = { moreProductOpen = false },
                onCreated = { result, tier ->
                    apply(result)
                    val created = (result as? EconomyResult.Success)?.value as? Account
                    if (created != null) {
                        repository.upsertDigitalCard(DigitalCard("card-${created.id}", created.id, "${created.displayName} Card", tier))
                        moreProductOpen = false
                        show("Producto dado de alta: ${created.iban}")
                    }
                }
            )
        }
    }
}

@Composable
private fun Header(accounts: Map<String, Account>, activeUser: UserProfile, onLogout: () -> Unit) {
    val ownedAccounts = accounts.values.filter { it.isActiveAccount() && it.isOwnedBy(activeUser) }
    val hasTributos = ownedAccounts.any { it.role == Role.Tributos }
    val hasAdmin = ownedAccounts.any { it.role == Role.Administracion }
    val compactScreen = LocalConfiguration.current.screenWidthDp < 380
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        val headerContentModifier = Modifier
            .fillMaxWidth()
            .background(SurfacePurple.copy(alpha = 0.72f))
            .padding(horizontal = if (compactScreen) 10.dp else 12.dp, vertical = 9.dp)
        if (compactScreen) {
            Column(
                headerContentModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Banco de La Placeta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${activeUser.displayName} · ${activeUser.dip}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HeaderUserActions(activeUser, hasTributos, hasAdmin, onLogout)
            }
        } else {
            Row(
                headerContentModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Banco de La Placeta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${activeUser.displayName} · ${activeUser.dip}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HeaderUserActions(activeUser, hasTributos, hasAdmin, onLogout)
            }
        }
    }
}

@Composable
private fun HeaderUserActions(
    activeUser: UserProfile,
    hasTributos: Boolean,
    hasAdmin: Boolean,
    onLogout: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .background(PremiumPurple, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(activeUser.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
            Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (hasTributos) RoleDot(TributosPurple)
                if (hasAdmin) RoleDot(Gold)
            }
        }
        IconButton(onClick = onLogout, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = PremiumPurple, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RoleDot(color: Color) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(color, RoundedCornerShape(5.dp))
            .border(1.dp, Color.White, RoundedCornerShape(5.dp))
    )
}

@Composable
private fun AccountSwitcher(
    accounts: List<Account>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    val selected = accounts.firstOrNull { it.id == selectedId } ?: accounts.firstOrNull()
    var expanded by remember(selectedId, accounts.size) { mutableStateOf(false) }
    val compactScreen = LocalConfiguration.current.screenWidthDp < 380
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        val switcherModifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PremiumPurple.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .background(SurfacePurple.copy(alpha = 0.78f), RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 10.dp, vertical = 7.dp)
        if (compactScreen) {
            Column(switcherModifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PremiumPurple, modifier = Modifier.size(15.dp))
                    Text(selected?.displayName ?: "Cuenta", style = MaterialTheme.typography.labelLarge, color = PremiumPurple, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${selected?.type?.label().orEmpty()} · ${formatPz(selected?.balancePz ?: 0)} Pz", style = MaterialTheme.typography.labelSmall, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Row(
                switcherModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PremiumPurple, modifier = Modifier.size(15.dp))
                    Text(selected?.displayName ?: "Cuenta", style = MaterialTheme.typography.labelLarge, color = PremiumPurple, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${selected?.type?.label().orEmpty()} · ${formatPz(selected?.balancePz ?: 0)} Pz", style = MaterialTheme.typography.labelSmall, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                items(accounts) { account ->
                    FilterChip(
                        selected = account.id == selectedId,
                        onClick = {
                            onSelect(account.id)
                            expanded = false
                        },
                        label = { Text("${account.displayName.take(12)} · ${account.type.label()}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CitizenDashboard(
    activeUser: UserProfile,
    accounts: Map<String, Account>,
    selectedAccountId: String,
    onSelectedAccount: (String) -> Unit,
    transactions: List<LedgerTransaction>,
    digitalCards: List<DigitalCard>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    upsertDigitalCard: (DigitalCard) -> Unit,
    users: List<UserProfile>,
    savedContacts: List<SavedContact>,
    payrollContracts: List<PayrollContract>,
    payrollPeriods: List<PayrollPeriod>,
    upsertPayrollContract: (PayrollContract) -> Unit,
    upsertPayrollPeriod: (PayrollPeriod) -> Unit,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    accountHolders: List<AccountHolder> = emptyList(),
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val citizenAccounts = accounts.values
        .filter { account ->
            account.isActiveAccount() && account.role == Role.Citizen && (
                account.isOwnedBy(activeUser) || accountHolders.any { holder ->
                    holder.accountId == account.id && holder.placetaId.normalizedOwnerId() == activeUser.placetaId.normalizedOwnerId()
                }
            )
        }
        .sortedBy { it.type.ordinal }
    val mainAccount = citizenAccounts.firstOrNull { it.id == selectedAccountId }
        ?: citizenAccounts.firstOrNull { it.id == activeUser.primaryAccountId }
        ?: citizenAccounts.firstOrNull { it.type == AccountType.Current }
        ?: return
    var amount by remember { mutableStateOf("180") }
    var code by remember { mutableStateOf<PlacezumCode?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var hideBalance by remember { mutableStateOf(false) }
    var showTransferPanel by remember { mutableStateOf(false) }
    var showSuccessPopup by remember { mutableStateOf(false) }
    val totalBalance = citizenAccounts.sumOf { it.balancePz }
    var firstBalancePaint by remember { mutableStateOf(true) }
    val animatedBalance by animateFloatAsState(
        targetValue = totalBalance.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(if (firstBalancePaint) 1100 else 420),
        label = "balance-count"
    )

    LaunchedEffect(Unit) {
        firstBalancePaint = false
    }

    fun playTransferSound() {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60).startTone(ToneGenerator.TONE_PROP_ACK, 160)
    }

    fun runWithBiometricIfNeeded(amountPz: Long, block: () -> Unit) {
        if (amountPz > 500_00) authenticateTransfer(block, show) else block()
    }

    val accountLimit = if (mainAccount.type == AccountType.Business) config.institutionalDeclarationThresholdPz else config.personalDeclarationThresholdPz
    val ia = (mainAccount.balancePz.toDouble() / accounts.values.sumOf { it.balancePz.coerceAtLeast(0) }.coerceAtLeast(1)).coerceIn(0.0, 1.0)
    val irmRate = if (ia > config.accumulationIndexThreshold) config.irmPersonalPercent else 0
    val canClaimRbu = mainAccount.lastRbuClaim == null || java.time.LocalDate.now().isAfter(mainAccount.lastRbuClaim.plusDays(6))
    val accountTransactions = remember(transactions, mainAccount.id) {
        transactions
            .filter { it.fromAccountId == mainAccount.id || it.toAccountId == mainAccount.id }
            .sortedByDescending { it.createdAt }
    }
    val weekAgo = remember { Instant.now().minusSeconds(7L * 24L * 60L * 60L) }
    val weeklyIncoming = accountTransactions
        .filter { it.toAccountId == mainAccount.id && it.createdAt.isAfter(weekAgo) }
        .sumOf { it.netAmount.coerceAtLeast(it.amountPz) }
    val weeklyOutgoing = accountTransactions
        .filter { it.fromAccountId == mainAccount.id && it.createdAt.isAfter(weekAgo) }
        .sumOf { it.amountPz + it.taxAmount }
    val lockedSavings = citizenAccounts.filter { it.type == AccountType.Savings || it.type == AccountType.Child }.sumOf { it.balancePz }
    var section by remember { mutableStateOf(WalletSection.Summary) }

    Box {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccountSwitcher(
            accounts = citizenAccounts,
            selectedId = mainAccount.id,
            onSelect = onSelectedAccount
        )
        BalanceHeroCard(
            balance = mainAccount.balancePz,
            hidden = hideBalance,
            limit = accountLimit,
            tier = "${mainAccount.displayName} · ${mainAccount.type.label()}",
            onToggle = {
                hideBalance = !hideBalance
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onSend = { showTransferPanel = true },
            onReceive = {
                code = engine.generatePlacezumCode(mainAccount)
                show("Código de recepción: ${code?.code}")
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction(Icons.Default.Payments, "Enviar", Modifier.weight(1f)) { showTransferPanel = true }
            QuickAction(Icons.Default.AccountBalanceWallet, "Recibir", Modifier.weight(1f)) {
                code = engine.generatePlacezumCode(mainAccount)
                show("Código de recepción: ${code?.code}")
            }
            QuickAction(Icons.Default.Add, "RBU", Modifier.weight(1f)) {
                if (canClaimRbu) {
                    apply(engine.claimRbu(accounts, mainAccount.id, 5_00))
                    showSuccessPopup = true
                } else {
                    show("RBU semanal aún no disponible")
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WalletSection.entries) { item ->
                FilterChip(
                    selected = section == item,
                    onClick = { section = item },
                    label = { Text(item.label) },
                    leadingIcon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        when (section) {
            WalletSection.Summary -> {
                CardBlock {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Vista rápida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("Patrimonio total entre tus cuentas", color = Color(0xFF6C5878))
                        }
                        Text("${formatPz(totalBalance)} Pz", color = PremiumPurple, fontWeight = FontWeight.Black)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ResultMetric("Cuenta", formatPz(mainAccount.balancePz), Modifier.weight(1f), PremiumPurple)
                        ResultMetric("Hucha", formatPz(lockedSavings), Modifier.weight(1f), IncomeGreen)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FiscalMiniCard(
                        title = "Semana",
                        value = "+${formatPz(weeklyIncoming)} / -${formatPz(weeklyOutgoing)}",
                        icon = Icons.Default.Warning,
                        iconColor = if (weeklyIncoming >= weeklyOutgoing) IncomeGreen else ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                    FiscalMiniCard(
                        title = "RBU",
                        value = if (canClaimRbu) "Disponible" else "En espera",
                        icon = Icons.Default.CheckCircle,
                        iconColor = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                CardBlock {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Estado fiscal", fontWeight = FontWeight.Black)
                            Text("IRM $irmRate% · IA ${"%.2f".format(ia)} · límite ${formatPz(accountLimit)} Pz", color = Color(0xFF6C5878))
                        }
                        Icon(
                            if (irmRate > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (irmRate > 0) Gold else IncomeGreen
                        )
                    }
                }
                CardBlock {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Estimación de impuestos", fontWeight = FontWeight.Black)
                            Text("Próximo IRM: ${engine.nextIrmPaymentDate()}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${formatPz(engine.estimateWeeklyTax(mainAccount))} Pz", color = ErrorRed, fontWeight = FontWeight.Black)
                    }
                }
                CardBlock {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Últimos movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        TextButton(onClick = { section = WalletSection.Activity }) {
                            Text("Ver todos", color = PremiumPurple, fontWeight = FontWeight.Black)
                        }
                    }
                    if (accountTransactions.isEmpty()) {
                        Text("Aún no hay actividad en esta cuenta.", color = Color(0xFF6C5878))
                    } else {
                        accountTransactions.take(3).forEach { txn ->
                            CompactTransactionLine(account = mainAccount, transaction = txn, config = config)
                        }
                    }
                }
                code?.let {
                    CardBlock {
                        Text("Código de recepción activo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(it.code, style = MaterialTheme.typography.headlineMedium, color = PremiumPurple, fontWeight = FontWeight.Black)
                        Text(it.iban, color = Color(0xFF6C5878))
                    }
                }
            }
            WalletSection.Accounts -> {
                SectionTitle("Cuentas GDLP")
                CardBlock {
                    Text("Resumen de cuentas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("${citizenAccounts.size} productos · ${formatPz(totalBalance)} Pz totales", color = Color(0xFF6C5878))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultMetric("Operativo", formatPz(citizenAccounts.filter { it.type == AccountType.Current || it.type == AccountType.Business }.sumOf { it.balancePz }), Modifier.weight(1f), PremiumPurple)
                        ResultMetric("Ahorro", formatPz(lockedSavings), Modifier.weight(1f), IncomeGreen)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultMetric("Inversión", formatPz(citizenAccounts.filter { it.type == AccountType.Investment }.sumOf { it.balancePz }), Modifier.weight(1f), Gold)
                        ResultMetric("Bloqueadas", citizenAccounts.count { it.complianceStatus != com.placeta.banco.core.ComplianceStatus.Clear }.toString(), Modifier.weight(1f), ErrorRed)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    citizenAccounts.forEach { account ->
                        AccountSummaryRow(
                            account = account,
                            selected = account.id == mainAccount.id,
                            onSelect = { onSelectedAccount(account.id) }
                        )
                    }
                }
                AccountManagementCard(
                    account = mainAccount,
                    activeUser = activeUser,
                    engine = engine,
                    accounts = accounts,
                    onResult = apply
                )
            }
            WalletSection.Card -> {
                CardWalletPanel(
                    cards = digitalCards.filter { it.accountId == mainAccount.id && !it.released },
                    allCards = digitalCards,
                    account = mainAccount,
                    engine = engine,
                    upsertDigitalCard = upsertDigitalCard,
                    authenticateTransfer = authenticateTransfer,
                    show = show
                )
            }
            WalletSection.Activity -> {
                History(mainAccount, transactions.filter { it.fromAccountId == mainAccount.id || it.toAccountId == mainAccount.id }, config)
            }
        }

        AnimatedVisibility(
            visible = showSuccessPopup,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            InlineSuccessPopup(
                title = "OPERACIÓN GUARDADA",
                message = "Saldo e historial sincronizados.",
                onDismiss = { showSuccessPopup = false }
            )
        }
}
        BottomSlideUpOverlay(visible = showTransferPanel, onDismiss = { showTransferPanel = false }) {
            TransferSlideUp(
                accounts = accounts,
                from = mainAccount,
                engine = engine,
                authenticateTransfer = authenticateTransfer,
                onClose = { showTransferPanel = false },
                onResult = { result ->
                    apply(result)
                    if (result is EconomyResult.Success) {
                        playTransferSound()
                        showSuccessPopup = true
                        showTransferPanel = false
                    }
                },
                onError = show
            )
        }
}
}

@Composable
private fun BalanceHeroCard(
    balance: Long,
    hidden: Boolean,
    limit: Long,
    tier: String,
    onToggle: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit
) {
    val compactScreen = LocalConfiguration.current.screenWidthDp < 380
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PremiumPurple.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactScreen) 16.dp else 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactScreen) 12.dp else 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Saldo disponible", style = MaterialTheme.typography.labelLarge, color = Color(0xFF6C5878))
                IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                    Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Ocultar saldo", tint = PremiumPurple, modifier = Modifier.size(20.dp))
                }
            }
            AnimatedContent(
                targetState = if (hidden) "•••••• Pz" else "${formatMoneyPz(balance)} Pz",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        slideInVertically(animationSpec = tween(220, delayMillis = 90)) { it / 2 })
                        .togetherWith(fadeOut(animationSpec = tween(90)) + slideOutVertically(animationSpec = tween(90)) { -it / 2 })
                },
                label = "balance-animation"
            ) { text ->
                Text(
                    text,
                    style = if (compactScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                    color = PremiumPurple,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("Límite: ${formatPz(limit)} Pz · $tier", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSend,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Enviar", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onReceive,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfacePurple, contentColor = PremiumPurple)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = PremiumPurple, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Recibir", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FiscalMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    action: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.border(1.dp, PremiumPurple.copy(alpha = 0.10f), RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Text(title, color = Color(0xFF6C5878))
            Text(value, fontWeight = FontWeight.Black)
            action?.let {
                TextButton(onClick = it) { Text("COBRAR", color = PremiumPurple, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun AccountSummaryRow(account: Account, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(1.dp, if (selected) PremiumPurple else PremiumPurple.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = if (selected) SurfacePurple else Color.White, contentColor = Ink),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).background(PremiumPurple.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (account.type) {
                        AccountType.Savings, AccountType.Child -> Icons.Default.Lock
                        AccountType.Investment -> Icons.Default.ShowChart
                        AccountType.Business -> Icons.Default.AccountBalance
                        else -> Icons.Default.AccountBalanceWallet
                    },
                    contentDescription = null,
                    tint = PremiumPurple,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(account.displayName, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${account.type.label()} · ${account.iban}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${formatPz(account.balancePz)} Pz", color = PremiumPurple, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (account.huchaLocked) "Hucha" else account.complianceStatus.name, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AccountManagementCard(
    account: Account,
    activeUser: UserProfile,
    engine: EconomyEngine,
    accounts: Map<String, Account>,
    onResult: (EconomyResult<*>) -> Unit
) {
    val availableTypes = account.mutableTypes().filter { it != account.type }
    val canClose = account.id != activeUser.primaryAccountId && account.balancePz == 0L && account.closedAt == null
    CardBlock {
        Text("Gestionar cuenta seleccionada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("${account.displayName} · ${account.type.label()} · ${account.iban}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        if (availableTypes.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableTypes) { type ->
                    FilterChip(
                        selected = false,
                        onClick = { onResult(engine.changeAccountType(accounts, account.id, type)) },
                        label = { Text("Convertir a ${type.label()}") }
                    )
                }
            }
        } else {
            Text("Esta cuenta no permite cambio de tipo desde autoservicio.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = { onResult(engine.closeAccount(accounts, account.id, activeUser.primaryAccountId)) },
            enabled = canClose,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Cerrar cuenta", fontWeight = FontWeight.Bold)
        }
        Text("Para cerrar: no principal, saldo 0 Pz y sin actividad pendiente.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AccountCard(
    account: Account,
    engine: EconomyEngine,
    selected: Boolean = false,
    onSelect: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val height by animateDpAsState(if (expanded) 188.dp else 132.dp, label = "account-card-height")
    Card(
        modifier = Modifier
            .size(width = 248.dp, height = height)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Gold else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                onSelect()
                expanded = !expanded
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (account.type == AccountType.Business) DeepPurple else PremiumPurple)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(account.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(account.iban, color = Color(0xFFDCCAF7), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatPz(account.balancePz)} Pz", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Reserva IRM: ${formatPz(engine.irmReserve(account.balancePz))} Pz", color = Mint, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                AnimatedVisibility(expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("IBAN COMPLETO", color = Gold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                        Text(account.iban, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Tipo ${account.type.label()} · Límite ${account.sendLimitPz?.let(::formatPz) ?: "sin límite"}", color = Color(0xFFDCCAF7), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Text(if (selected) "Activa" else account.type.label(), modifier = Modifier.align(Alignment.BottomEnd), color = Gold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun WidgetPreviewCard(
    totalBalance: Long,
    account: Account,
    onPay: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepPurple, contentColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("WIDGET ACTIVO", color = Gold, fontWeight = FontWeight.ExtraBold)
                    Text("${formatPz(totalBalance)} Pz", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(account.iban, color = Color(0xFFDCCAF7))
                }
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Mint, modifier = Modifier.size(36.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction(Icons.Default.Contactless, "Pagar", Modifier.weight(1f), onPay)
                QuickAction(Icons.Default.Add, "Añadir", Modifier.weight(1f), onAdd)
            }
        }
    }
}

@Composable
private fun TransferSlideUp(
    accounts: Map<String, Account>,
    from: Account,
    engine: EconomyEngine,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onClose: () -> Unit,
    onResult: (EconomyResult<LedgerTransaction>) -> Unit,
    onError: (String) -> Unit
) {
    var targetIban by remember(from.id) { mutableStateOf("") }
    var amount by remember { mutableStateOf("120") }
    var conceptKind by remember { mutableStateOf(if (from.type == AccountType.Business) TransactionKind.Consumption else TransactionKind.Placezum) }
    val resolvedReceiver = accounts.values.firstOrNull { it.iban == targetIban.uppercase() }
    val isBusiness = from.type == AccountType.Business

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("NUEVA TRANSFERENCIA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Desde ${from.displayName} · ${from.iban}", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            OutlinedTextField(
                value = targetIban,
                onValueChange = { targetIban = it.toGdlpIbanInput() },
                label = { Text("IBAN destino app o web") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PremiumPurple) }
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = sanitizeMoneyInput(it) },
                label = { Text("Importe Pz") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            if (isBusiness) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = conceptKind == TransactionKind.Placezum,
                        onClick = { conceptKind = TransactionKind.Placezum },
                        label = { Text("PlaceZum (sin IVA)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = conceptKind == TransactionKind.Consumption,
                        onClick = { conceptKind = TransactionKind.Consumption },
                        label = { Text("Consumo + IVA") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EEFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Destino", color = Color(0xFF6C5878))
                        Text(resolvedReceiver?.displayName ?: "IBAN pendiente de localizar", fontWeight = FontWeight.Bold)
                        Text(targetIban.ifBlank { "GDLP-APXX-XXX / GDLP-XXXX-XXXX" }, color = PremiumPurple)
                    }
                    Text("${formatPz(parseMoneyInput(amount))} Pz", fontWeight = FontWeight.Black, color = PremiumPurple)
                }
            }
            PressableButton(
                icon = Icons.Default.Payments,
                label = "FIRMAR Y ENVIAR",
                modifier = Modifier.fillMaxWidth(),
                containerColor = PremiumPurple
            ) {
                val value = parseMoneyInput(amount)
                if (value <= 0) {
                    onError("Introduce un importe válido")
                    return@PressableButton
                }
                if (!IbanGdlp.isOfficial(targetIban)) {
                    onError("Usa un IBAN GDLP válido de app o web")
                    return@PressableButton
                }
                authenticateTransfer(
                    {
                        onResult(engine.executeTransfer(accounts, from.id, targetIban, value, conceptKind))
                    },
                    onError
                )
            }
        }
    }
}

@Composable
private fun InlineSuccessPopup(title: String, message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PremiumPurple, contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(PremiumPurple)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = Mint, modifier = Modifier.size(34.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black)
                Text(message, color = Color.White.copy(alpha = 0.78f))
            }
            TextButton(onClick = onDismiss) { Text("OK", color = Mint) }
        }
    }
}

@Composable
private fun CardWalletPanel(
    cards: List<DigitalCard>,
    allCards: List<DigitalCard>,
    account: Account,
    engine: EconomyEngine,
    upsertDigitalCard: (DigitalCard) -> Unit,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    show: (String) -> Unit
) {
    val ensuredCards = if (cards.isEmpty()) {
        listOf(DigitalCard("card-${account.id}", account.id, "Tarjeta Virtual", MemberTier.Standard, cardNumber = randomSixDigits(), pin = randomFourDigits()))
    } else {
        cards.sortedWith(compareByDescending<DigitalCard> { it.promoPhysical }.thenBy { it.alias })
    }
    var selectedCardId by remember(ensuredCards.map { it.id }) { mutableStateOf(ensuredCards.first().id) }
    val selected = ensuredCards.firstOrNull { it.id == selectedCardId } ?: ensuredCards.first()
    var registerPromoOpen by remember { mutableStateOf(false) }
    var paymentOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    val virtualCardCount = ensuredCards.count { !it.promoPhysical }
    val canCreateVirtualCard = virtualCardCount < MAX_VIRTUAL_CARDS_PER_ACCOUNT

    LaunchedEffect(cards.isEmpty()) {
        if (cards.isEmpty()) upsertDigitalCard(ensuredCards.first())
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CardBlock {
            Text("Mis tarjetas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Tarjeta virtual para pago móvil. Promo Card física aparecerá cuando salga de fabricación.", color = Color(0xFF6C5878))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ensuredCards) { card ->
                    FilterChip(
                        selected = card.id == selected.id,
                        onClick = { selectedCardId = card.id },
                        label = { Text("${if (card.promoPhysical) "Promo" else "Virtual"} · ${card.cardNumber.takeLast(3)}") },
                        leadingIcon = {
                            Icon(if (card.promoPhysical) Icons.Default.Contactless else Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (!canCreateVirtualCard) {
                            show("Límite de $MAX_VIRTUAL_CARDS_PER_ACCOUNT tarjetas virtuales por cuenta")
                            return@OutlinedButton
                        }
                        val newCard = DigitalCard(
                            id = "card-${System.currentTimeMillis()}",
                            accountId = account.id,
                            alias = "Tarjeta Virtual ${ensuredCards.count { !it.promoPhysical } + 1}",
                            tier = MemberTier.Standard,
                            cardNumber = randomSixDigits(),
                            pin = randomFourDigits()
                        )
                        upsertDigitalCard(newCard)
                        selectedCardId = newCard.id
                        show("Tarjeta virtual creada")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canCreateVirtualCard,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Virtual $virtualCardCount/$MAX_VIRTUAL_CARDS_PER_ACCOUNT")
                }
                Button(
                    onClick = { registerPromoOpen = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Contactless, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Vincular NFC")
                }
            }
        }
        DigitalCardPreview(
            card = selected,
            account = account,
            engine = engine,
            upsertDigitalCard = upsertDigitalCard,
            authenticateTransfer = authenticateTransfer,
            onPay = { paymentOpen = true },
            onSettings = { settingsOpen = true },
            show = show
        )
        BottomSlideUpOverlay(visible = registerPromoOpen, onDismiss = { registerPromoOpen = false }) {
            PromoCardComingSoonSlideUp(account = account, onClose = { registerPromoOpen = false })
        }
        BottomSlideUpOverlay(visible = paymentOpen, onDismiss = { paymentOpen = false }) {
            CardPaymentSlideUp(
                card = selected,
                account = account,
                engine = engine,
                authenticateTransfer = authenticateTransfer,
                onClose = { paymentOpen = false },
                show = show
            )
        }
        BottomSlideUpOverlay(visible = settingsOpen, onDismiss = { settingsOpen = false }) {
            CardSettingsSlideUp(
                card = selected,
                onClose = { settingsOpen = false },
                onSave = {
                    upsertDigitalCard(it)
                    settingsOpen = false
                    show("Tarjeta actualizada")
                },
                onRelease = {
                    upsertDigitalCard(it.copy(released = true, frozen = true, promoPhysical = true, accountId = "", alias = "Promo Card libre"))
                    settingsOpen = false
                    show("Promo Card liberada para nuevo registro")
                }
            )
        }
    }
}

@Composable
private fun DigitalCardPreview(
    card: DigitalCard,
    account: Account,
    engine: EconomyEngine,
    upsertDigitalCard: (DigitalCard) -> Unit,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onPay: () -> Unit,
    onSettings: () -> Unit,
    show: (String) -> Unit
) {
    val frozen = card.frozen
    val cardNumber = remember(card.id, card.cardNumber) { card.cardNumber.filter(Char::isDigit).padStart(6, '0').takeLast(6) }
    val pin = remember(card.id, card.pin) { card.pin.filter(Char::isDigit).padEnd(4, '0').take(4) }
    val promoPhysical = card.promoPhysical
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = event.values.getOrNull(1)?.coerceIn(-1.2f, 1.2f) ?: 0f
                tiltY = event.values.getOrNull(0)?.coerceIn(-1.2f, 1.2f) ?: 0f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager?.unregisterListener(listener) }
    }
    val cardArt = if (promoPhysical) R.drawable.promocard else R.drawable.vitualcard
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black, RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(cardArt),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(0.dp),
                    contentScale = ContentScale.Crop
                )
                if (frozen) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.36f)))
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp, bottom = 14.dp)
                        .background(Color.Black.copy(alpha = 0.44f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text("Nº $cardNumber", color = Color.White, fontWeight = FontWeight.Black)
                    Text("PIN ${pin.padStart(4, '0')}", color = Color(0xFFE8D9FF), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                if (!promoPhysical && !frozen) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .background(PremiumPurple.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Móvil", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.alias, fontWeight = FontWeight.Black)
                Text(
                    when {
                        frozen -> "Congelada"
                        card.released -> "Liberada"
                        promoPhysical -> "Promo Card física · paga acercando la tarjeta"
                        else -> "Tarjeta virtual"
                    },
                    color = Color(0xFF6C5878)
                )
            }
            Text("${formatPz(account.balancePz)} Pz", color = PremiumPurple, fontWeight = FontWeight.Black)
        }
        CardBlock {
            Text("Acciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !frozen && !promoPhysical,
                    onClick = onPay,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Contactless, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Pagar")
                }
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Detalles")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Config")
                }
                Button(
                    onClick = {
                        upsertDigitalCard(card.copy(frozen = !frozen))
                        show(if (frozen) "Tarjeta reactivada" else "Tarjeta congelada")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (frozen) IncomeGreen else ErrorRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(if (frozen) Icons.Default.Lock else Icons.Default.AcUnit, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (frozen) "Reactivar" else "Congelar")
                }
            }
            Text(
                if (promoPhysical) "Para pagar con Promo Card, acerca la tarjeta física al lector PlaceZum. Desde aquí solo se consulta y libera."
                else "El pago móvil de la tarjeta virtual se activa desde el slideup Pagar.",
                color = Color(0xFF6C5878)
            )
        }
    }
}

@Composable
private fun PromoCardComingSoonSlideUp(account: Account, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Promo Card", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Función próximamente", color = PremiumPurple, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.08f), contentColor = Ink),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Contactless, contentDescription = null, tint = PremiumPurple, modifier = Modifier.size(38.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Aún está en fabricación", fontWeight = FontWeight.Black)
                        Text("Cuando esté lista se podrá vincular a ${account.displayName} desde aquí. Por ahora usa tarjetas virtuales y PlaceZum NFC.", color = Color(0xFF6C5878))
                    }
                }
            }
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CardPaymentSlideUp(
    card: DigitalCard,
    account: Account,
    engine: EconomyEngine,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onClose: () -> Unit,
    show: (String) -> Unit
) {
    val context = LocalContext.current
    val hasNfc = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) && NfcAdapter.getDefaultAdapter(context) != null
    }
    var nfcEnabled by remember(card.id) { mutableStateOf(false) }
    var nfcStatus by remember { mutableStateOf("NFC apagado.") }
    DisposableEffect(nfcEnabled, card.id, account.id) {
        if (nfcEnabled && !card.frozen && hasNfc && !card.promoPhysical) {
            val freshCode = engine.generatePlacezumCode(account)
            PlacezumNfcSession.publish(freshCode)
            nfcStatus = "Tarjeta virtual activa · código ${freshCode.code} · caduca en 2 min"
        }
        onDispose { PlacezumNfcSession.clear() }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pago móvil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(card.alias, color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.08f), contentColor = Ink),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tarjeta virtual contra lector PlaceZum", fontWeight = FontWeight.Black)
                    Text("Activa NFC y acerca este móvil al lector. La otra parte confirma el cobro desde PlaceZum.", color = Color(0xFF6C5878))
                }
            }
            Button(
                enabled = hasNfc && !card.frozen && !card.promoPhysical,
                onClick = {
                    authenticateTransfer(
                        {
                            nfcEnabled = !nfcEnabled
                            if (!nfcEnabled) {
                                PlacezumNfcSession.clear()
                                nfcStatus = "Tarjeta virtual desactivada."
                            }
                        },
                        show
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Contactless, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (nfcEnabled) "Apagar pago móvil" else "Activar pago móvil", fontWeight = FontWeight.Black)
            }
            Text(
                when {
                    card.promoPhysical -> "Las Promo Cards pagan acercando la tarjeta física al lector, no emulándola desde el móvil."
                    !hasNfc -> "Este dispositivo no tiene NFC disponible."
                    card.frozen -> "Reactiva la tarjeta para pagar."
                    else -> nfcStatus
                },
                color = if (nfcEnabled) PremiumPurple else Color(0xFF6C5878),
                fontWeight = if (nfcEnabled) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CardSettingsSlideUp(
    card: DigitalCard,
    onClose: () -> Unit,
    onSave: (DigitalCard) -> Unit,
    onRelease: (DigitalCard) -> Unit
) {
    var alias by remember(card.id, card.alias) { mutableStateOf(card.alias) }
    var pin by remember(card.id, card.pin) { mutableStateOf(card.pin.filter(Char::isDigit).padEnd(4, '0').take(4)) }
    var tier by remember(card.id, card.tier) { mutableStateOf(card.tier) }
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Detalles de tarjeta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(if (card.promoPhysical) "Promo Card física vinculada" else "Tarjeta virtual", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePurple, contentColor = Ink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Número de tarjeta", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        Text(card.cardNumber.padStart(6, '0').takeLast(6), color = PremiumPurple, fontWeight = FontWeight.Black)
                    }
                    Icon(if (card.promoPhysical) Icons.Default.Contactless else Icons.Default.CreditCard, contentDescription = null, tint = PremiumPurple)
                }
            }
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it.take(28) },
                label = { Text("Nombre de tarjeta") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                label = { Text("PIN 4") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePurple, contentColor = Ink),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = PremiumPurple)
                    Column {
                        Text("Tipo de tarjeta", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        Text(tier.name, fontWeight = FontWeight.Black, color = PremiumPurple)
                    }
                }
            }
            Button(
                enabled = pin.length == 4,
                onClick = { onSave(card.copy(alias = alias.ifBlank { card.alias }, pin = pin, released = false)) },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Guardar cambios", fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                enabled = card.promoPhysical,
                onClick = { onRelease(card) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Replay, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Liberar Promo Card")
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    PressableButton(icon, label.uppercase(), modifier, PremiumPurple, Color.White, onClick)
}

@Composable
private fun PressableButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = PremiumPurple,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "button-press")
    val animatedColor by animateColorAsState(if (pressed) containerColor.copy(alpha = 0.86f) else containerColor, label = "button-color")
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.scale(scale).height(54.dp),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        colors = ButtonDefaults.buttonColors(containerColor = animatedColor, contentColor = contentColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Text(label, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PlacezumScreen(
    activeUser: UserProfile,
    accounts: Map<String, Account>,
    selectedAccountId: String,
    onSelectedAccount: (String) -> Unit,
    transactions: List<LedgerTransaction>,
    savedContacts: List<SavedContact>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    upsertSavedContact: (SavedContact) -> Unit,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val citizenAccounts = accounts.values.filter { it.isActiveAccount() && it.isOwnedBy(activeUser) && it.role == Role.Citizen }.sortedBy { it.type.ordinal }
    val selected = citizenAccounts.firstOrNull { it.id == selectedAccountId }
        ?: citizenAccounts.firstOrNull { it.id == activeUser.primaryAccountId }
        ?: citizenAccounts.firstOrNull()
        ?: return
    var code by remember(selected.id) { mutableStateOf(engine.generatePlacezumCode(selected)) }
    var amount by remember { mutableStateOf("88") }
    var payCode by remember { mutableStateOf("") }
    var concept by remember { mutableStateOf("") }
    var paymentSent by remember { mutableStateOf(false) }
    var readerOpen by remember { mutableStateOf(false) }
    var terminalOpen by remember { mutableStateOf(false) }
    var nfcCard by remember { mutableStateOf<NfcPlacezumCard?>(null) }
    var nfcStatus by remember { mutableStateOf("") }
    var payPanelOpen by remember { mutableStateOf(false) }
    var contactsOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hasNfc = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) && NfcAdapter.getDefaultAdapter(context) != null
    }

    fun chime() {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, if (selected.type == AccountType.Child) 100 else 80)
        tone.startTone(if (selected.type == AccountType.Child) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_PROMPT, 180)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    fun weekSpent(): Long {
        val zone = ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return transactions.filter {
            it.fromAccountId == selected.id &&
                it.kind == TransactionKind.Placezum &&
                !it.createdAt.atZone(zone).toLocalDate().isBefore(weekStart)
        }.sumOf { it.amountPz }
    }
    fun canSpend(value: Long): Boolean {
        if (value <= 0) {
            show("Introduce un importe válido")
            return false
        }
        val spent = weekSpent()
        if (spent + value > config.placezumWeeklyLimitPz) {
            show("Límite semanal PlaceZum superado: ${formatPz(spent)} de ${formatPz(config.placezumWeeklyLimitPz)} Pz")
            return false
        }
        return true
    }
    val spentThisWeek = weekSpent()
    val remainingThisWeek = (config.placezumWeeklyLimitPz - spentThisWeek).coerceAtLeast(0)
    val usagePercent = if (config.placezumWeeklyLimitPz > 0) {
        ((spentThisWeek * 100) / config.placezumWeeklyLimitPz).coerceIn(0, 100)
    } else 0
    val savedContactCount = savedContacts.count { contact ->
        contact.ownerPlacetaId == activeUser.placetaId && contact.accountId != selected.id && accounts.containsKey(contact.accountId)
    }

    DisposableEffect(readerOpen, selected.id) {
        if (readerOpen && hasNfc) {
            val freshCode = engine.generatePlacezumCode(selected)
            code = freshCode
            PlacezumNfcSession.publish(freshCode)
            nfcStatus = "Tarjeta PlaceZum NFC activa: ${freshCode.code}"
        }
        onDispose {
            PlacezumNfcSession.clear()
        }
    }

    DisposableEffect(terminalOpen, hasNfc, selected.id) {
        val activity = context as? android.app.Activity
        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (terminalOpen && hasNfc && activity != null && adapter != null) {
            nfcStatus = "Datafono esperando tarjeta PlaceZum..."
            val callback = NfcAdapter.ReaderCallback { tag ->
                val detected = readPlacezumCard(tag)
                activity.runOnUiThread {
                    if (detected == null) {
                        nfcStatus = "Tarjeta NFC no compatible"
                    } else {
                        nfcCard = detected
                        nfcStatus = "Tarjeta leída: ${detected.code} · ${detected.iban}"
                    }
                }
            }
            adapter.enableReaderMode(
                activity,
                callback,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
        onDispose {
            if (terminalOpen && activity != null && adapter != null) {
                adapter.disableReaderMode(activity)
            }
        }
    }

    Box {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AccountSwitcher(citizenAccounts, selected.id) {
            onSelectedAccount(it)
            accounts[it]?.let { account -> code = engine.generatePlacezumCode(account) }
        }

        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Modo recepción", style = MaterialTheme.typography.titleMedium)
                    Text(code.code, style = MaterialTheme.typography.headlineLarge, color = PremiumPurple, fontWeight = FontWeight.ExtraBold)
                    Text("Código temporal para que te paguen · caduca en 2 min", color = Color(0xFF6C5878))
                }
                IconButton(onClick = {
                    code = engine.generatePlacezumCode(selected)
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70).startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                    show("Código PlaceZum regenerado")
                }) {
                    Icon(Icons.Default.Replay, contentDescription = "Regenerar", tint = PremiumPurple)
                }
            }
        }

        CardBlock {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PlaceZum fácil", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { payPanelOpen = true },
                        modifier = Modifier.weight(1f).height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Pagar", fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(
                        onClick = { contactsOpen = true },
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PremiumPurple)
                        Spacer(Modifier.size(8.dp))
                        Text("Contactos", color = PremiumPurple, fontWeight = FontWeight.Black)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResultMetric("Disponible", "${formatPz(remainingThisWeek)} Pz", Modifier.weight(1f), IncomeGreen)
                    ResultMetric("Contactos", "$savedContactCount", Modifier.weight(1f), PremiumPurple)
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Límite semanal", color = Color(0xFF6C5878))
                        Text("${formatPz(spentThisWeek)} / ${formatPz(config.placezumWeeklyLimitPz)} Pz", fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).background(PremiumPurple.copy(alpha = 0.10f), RoundedCornerShape(100.dp))) {
                        Box(
                            Modifier
                                .fillMaxWidth(usagePercent / 100f)
                                .height(8.dp)
                                .background(PremiumPurple, RoundedCornerShape(100.dp))
                        )
                    }
                }
            }
        }

        CardBlock {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cobro por código", fontWeight = FontWeight.Black)
                Text("Muestra el código temporal para recibir. Para enviar, pulsa Pagar y escribe el código de la otra persona.", color = Color(0xFF6C5878))
                Text("Cuenta ${selected.displayName}", color = PremiumPurple, fontWeight = FontWeight.Bold)
            }
            if (hasNfc) {
                Button(
                    onClick = { terminalOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Contactless, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Abrir lector PlaceZum NFC", fontWeight = FontWeight.Black)
                }
            }
        }



        CardBlock {
            var linkAmountP by remember { mutableStateOf("") }
            var linkConceptP by remember { mutableStateOf("") }
            var showLinkForm by remember { mutableStateOf(false) }
            var myLinks by remember { mutableStateOf<List<Triple<String, String, Long>>>(emptyList()) }
            var showPersonalQr by remember { mutableStateOf(false) }
            var personalQrData by remember { mutableStateOf<Triple<String, String, Long>?>(null) }
            val placezumContext = context
            Text("Enlaces de pago personales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Crea un enlace para que te paguen sin compartir tu código.", color = Color(0xFF6C5878))
            if (showLinkForm) {
                OutlinedTextField(linkAmountP, { linkAmountP = sanitizeMoneyInput(it) }, label = { Text("Importe Pz") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(linkConceptP, { linkConceptP = it.take(60) }, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showLinkForm = false; linkAmountP = ""; linkConceptP = "" }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancelar") }
                    Button(onClick = {
                        val pz = parseMoneyInput(linkAmountP)
                        if (pz <= 0) return@Button
                        val linkId = "PZ-${(1000..9999).random()}"
                        val newLink = Triple(linkId, linkConceptP.ifBlank { "Pago PlaceZum" }, pz)
                        myLinks = myLinks + newLink
                        personalQrData = newLink
                        showPersonalQr = true
                        showLinkForm = false; linkAmountP = ""; linkConceptP = ""
                    }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) { Text("Crear") }
                }
            } else {
                Button(onClick = { showLinkForm = true }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Nuevo enlace", fontWeight = FontWeight.Black)
                }
            }
            if (myLinks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                myLinks.forEach { (id, title, amount) ->
                    Row(Modifier.fillMaxWidth().background(PremiumPurple.copy(alpha = 0.06f), RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(id, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatPz(amount), fontWeight = FontWeight.Black, color = PremiumPurple)
                    }
                }
            }
            personalQrData?.let { (id, title, amount) ->
                if (showPersonalQr) {
                    BottomSlideUpOverlay(visible = showPersonalQr, onDismiss = { showPersonalQr = false }) {
                        PaymentLinkQrSlideUp(
                            linkId = id, linkTitle = title, linkAmount = amount,
                            onClose = { showPersonalQr = false },
                            onShare = { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url)
                                }
                                placezumContext.startActivity(Intent.createChooser(intent, "Compartir enlace"))
                            }
                        )
                    }
                }
            }
        }

    }
        AnimatedVisibility(paymentSent) {
            PaymentSuccessOverlay { paymentSent = false }
        }
        BottomSlideUpOverlay(visible = contactsOpen, onDismiss = { contactsOpen = false }) {
            PlacezumContactsSlideUp(
                accounts = accounts,
                activeUser = activeUser,
                selected = selected,
                savedContacts = savedContacts,
                engine = engine,
                onAddContact = upsertSavedContact,
                onClose = { contactsOpen = false },
                onPick = { contact, contactCode ->
                    payCode = contactCode
                    concept = "PlaceZum a ${contact.displayName}"
                    contactsOpen = false
                    payPanelOpen = true
                }
            )
        }
        BottomSlideUpOverlay(visible = payPanelOpen, onDismiss = { payPanelOpen = false }) {
            PlacezumPaySlideUp(
                payCode = payCode,
                amount = amount,
                concept = concept,
                onPayCode = { payCode = it.filter(Char::isDigit).take(5) },
                onAmount = { amount = sanitizeMoneyInput(it) },
                onConcept = { concept = it.take(80) },
                onClose = { payPanelOpen = false },
                onPay = {
                    val pz = parseMoneyInput(amount)
                    if (!canSpend(pz)) return@PlacezumPaySlideUp
                    authenticateTransfer(
                        {
                            val result = engine.payWithPlacezumCode(accounts, selected.id, payCode, pz, concept)
                            apply(result)
                            if (result is EconomyResult.Success) {
                                chime()
                                paymentSent = true
                                payPanelOpen = false
                                payCode = ""
                                concept = ""
                                PlacetaNotifications.placezumReceived(context, pz, accounts[result.value.toAccountId]?.displayName ?: "PlaceZum")
                            }
                        },
                        show
                    )
                }
            )
        }
        AnimatedVisibility(
            visible = readerOpen || terminalOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            ContactlessPaymentSlideUp(
                title = if (terminalOpen) "Datafono personal" else "Tarjeta PlaceZum NFC",
                subtitle = if (terminalOpen) "Acerca otro móvil con Banco de La Placeta en modo tarjeta NFC para cobrar." else "Acerca este móvil al datafono PlaceZum para pagar.",
                isTerminal = terminalOpen,
                nfcStatus = nfcStatus,
                scannedCard = nfcCard,
                amount = amount,
                from = selected,
                receiver = selected,
                concept = concept,
                limitPz = config.contactlessLimitPz,
                onAmount = { amount = sanitizeMoneyInput(it) },
                onConcept = { concept = it.take(80) },
                onClose = {
                    readerOpen = false
                    terminalOpen = false
                    nfcCard = null
                    nfcStatus = ""
                    PlacezumNfcSession.clear()
                },
                onPay = {
                    val pz = parseMoneyInput(amount)
                    if (terminalOpen) {
                        val card = nfcCard
                        if (card == null) {
                            show("Acerca primero una tarjeta PlaceZum NFC")
                            return@ContactlessPaymentSlideUp
                        }
                        if (Instant.now().epochSecond > card.expiresAtEpochSeconds) {
                            show("Tarjeta PlaceZum NFC caducada")
                            nfcCard = null
                            return@ContactlessPaymentSlideUp
                        }
                        val payer = accounts[card.accountId] ?: accounts.values.firstOrNull { it.iban == card.iban }
                        if (payer == null) {
                            show("Cuenta NFC no localizada")
                            return@ContactlessPaymentSlideUp
                        }
                        if (payer.id == selected.id) {
                            show("No puedes cobrarte a la misma cuenta")
                            return@ContactlessPaymentSlideUp
                        }
                        if (pz <= 0) {
                            show("Introduce un importe válido")
                            return@ContactlessPaymentSlideUp
                        }
                        val submit = {
                            apply(engine.transferByIban(accounts, payer.id, selected.iban, pz, TransactionKind.Placezum, concept.ifBlank { "PlaceZum NFC ${card.code}" }))
                            chime()
                            paymentSent = true
                            nfcCard = null
                            terminalOpen = false
                            PlacetaNotifications.placezumReceived(context, pz, payer.displayName)
                        }
                        if (pz > config.contactlessLimitPz) authenticateTransfer(submit, show) else submit()
                    } else if (canSpend(pz)) {
                        val submit = {
                            show("Tarjeta NFC activa. Acerca este móvil al datafono para pagar.")
                            chime()
                        }
                        if (pz > config.contactlessLimitPz) authenticateTransfer(submit, show) else submit()
                    }
                }
            )
        }
    }
}

@Composable
private fun PlacezumContactsSlideUp(
    accounts: Map<String, Account>,
    activeUser: UserProfile,
    selected: Account,
    savedContacts: List<SavedContact>,
    engine: EconomyEngine,
    onAddContact: (SavedContact) -> Unit,
    onClose: () -> Unit,
    onPick: (Account, String) -> Unit
) {
    var ibanInput by remember { mutableStateOf("") }
    val resolved = accounts.values.firstOrNull { it.iban == ibanInput.toGdlpIbanInput() && it.id != selected.id }
    val contacts = savedContacts
        .filter { it.ownerPlacetaId == activeUser.placetaId }
        .mapNotNull { accounts[it.accountId] }
        .filter { it.id != selected.id }
        .distinctBy { it.id }
        .sortedBy { it.displayName }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Favoritos PlaceZum", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Opcional: guarda personas por IBAN para repetir pagos más rápido", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple) }
            }
            OutlinedTextField(
                value = ibanInput,
                onValueChange = { ibanInput = it.toGdlpIbanInput() },
                label = { Text("Guardar favorito por IBAN AP o web") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                            Text(resolved?.displayName ?: "Introduce un IBAN de la app o web", fontWeight = FontWeight.Bold)
                        Text(resolved?.let { "Código ${engine.generatePlacezumCode(it).code}" } ?: "Guardar es opcional; para pagar basta con el código", color = Color(0xFF6C5878))
                    }
                    Button(
                        enabled = resolved != null,
                        onClick = {
                            val contact = resolved ?: return@Button
                            onAddContact(SavedContact("contact-${activeUser.placetaId}-${contact.id}", activeUser.placetaId, contact.id))
                            ibanInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Guardar") }
                }
            }
            if (contacts.isEmpty()) {
                EmptyState(Icons.Default.SupportAgent, "Sin favoritos", "Puedes pagar por código sin guardar contactos.")
            } else {
                contacts.forEach { contact ->
                    val contactCode = engine.generatePlacezumCode(contact).code
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(PremiumPurple.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            .clickable { onPick(contact, contactCode) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.displayName, fontWeight = FontWeight.Black)
                            Text(contact.iban, color = Color(0xFF6C5878))
                        }
                        Text(contactCode, color = PremiumPurple, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacezumPaySlideUp(
    payCode: String,
    amount: String,
    concept: String,
    onPayCode: (String) -> Unit,
    onAmount: (String) -> Unit,
    onConcept: (String) -> Unit,
    onClose: () -> Unit,
    onPay: () -> Unit
) {
    val canPay = payCode.length == 5 && parseMoneyInput(amount) > 0
    val quickAmounts = listOf("5", "12", "25", "50", "100")
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Pagar con código", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Contactos es solo un atajo: puedes escribir cualquier código temporal", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple) }
            }
            OutlinedTextField(
                value = payCode,
                onValueChange = onPayCode,
                label = { Text("Código PlaceZum de quien recibe") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = amount,
                onValueChange = onAmount,
                label = { Text("Importe Pz") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickAmounts.take(3).forEach { value ->
                    OutlinedButton(
                        onClick = { onAmount(value) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("${value} Pz", color = PremiumPurple, fontWeight = FontWeight.Bold) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickAmounts.drop(3).forEach { value ->
                    OutlinedButton(
                        onClick = { onAmount(value) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("${value} Pz", color = PremiumPurple, fontWeight = FontWeight.Bold) }
                }
                OutlinedButton(
                    onClick = { onConcept("PlaceZum") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Concepto", color = PremiumPurple, fontWeight = FontWeight.Bold) }
            }
            OutlinedTextField(concept, onConcept, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Button(
                onClick = onPay,
                enabled = canPay,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple)
            ) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (canPay) "PAGAR" else "COMPLETA CÓDIGO E IMPORTE", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ContactlessPaymentSlideUp(
    title: String,
    subtitle: String,
    isTerminal: Boolean,
    nfcStatus: String,
    scannedCard: NfcPlacezumCard?,
    amount: String,
    from: Account,
    receiver: Account,
    concept: String,
    limitPz: Long,
    onAmount: (String) -> Unit,
    onConcept: (String) -> Unit,
    onClose: () -> Unit,
    onPay: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var confirmationReady by remember(scannedCard?.code, amount) { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "nfc-pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "nfc-pulse-scale"
    )
    val validAmount = parseMoneyInput(amount) > 0
    val validConcept = concept.isNotBlank()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(if (isTerminal) "Paso $step de 3: ${listOf("Importe", "Concepto", "Escaneo")[step-1]}" else subtitle, color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            // Indicador de pasos
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..3) {
                    val isActive = step == i
                    val isDone = step > i
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 14.dp else 10.dp)
                            .background(
                                if (isDone) IncomeGreen else if (isActive) PremiumPurple else Color(0xFFDCCAF7),
                                RoundedCornerShape(if (isActive) 7.dp else 5.dp)
                            )
                    )
                    if (i < 3) Box(modifier = Modifier.size(width = 24.dp, height = 2.dp).background(if (step > i) IncomeGreen else Color(0xFFDCCAF7)))
                }
            }
            when (step) {
                1 -> {
                    // Paso 1: Importe
                    Box(
                        modifier = Modifier.size(120.dp).background(DeepPurple, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Mint, modifier = Modifier.size(48.dp))
                            Text("PASO 1", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                    OutlinedTextField(value = amount, onValueChange = onAmount, label = { Text("Importe Pz") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Button(
                        onClick = { if (validAmount) { step = 2 } },
                        enabled = validAmount, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple)
                    ) { Text("SIGUIENTE: CONCEPTO", fontWeight = FontWeight.Black) }
                }
                2 -> {
                    // Paso 2: Concepto
                    Box(
                        modifier = Modifier.size(120.dp).background(DeepPurple, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Mint, modifier = Modifier.size(48.dp))
                            Text("PASO 2", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                    OutlinedTextField(value = concept, onValueChange = onConcept, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(18.dp)) { Text("Atrás", color = PremiumPurple) }
                        Button(
                            onClick = { if (validConcept) { step = 3 } },
                            enabled = validConcept, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple)
                        ) { Text("SIGUIENTE: ESCANEAR", fontWeight = FontWeight.Black) }
                    }
                }
                3 -> {
                    // Paso 3: Escanear tarjeta virtual o PromoCard
                    Box(
                        modifier = Modifier.size(190.dp).background(DeepPurple, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Mint.copy(alpha = 0.10f), radius = size.minDimension * 0.44f * pulseScale)
                            drawCircle(Color.White.copy(alpha = 0.10f), radius = size.minDimension * 0.34f * pulseScale)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Contactless, contentDescription = null, tint = Mint, modifier = Modifier.size(72.dp))
                            Text(if (scannedCard != null) "TARJETA DETECTADA" else "ACERCA TARJETA", color = Color.White, fontWeight = FontWeight.Black)
                            Text(if (scannedCard != null) scannedCard.iban else "Virtual o PromoCard", color = Color(0xFFDCCAF7))
                        }
                    }
                    if (nfcStatus.isNotBlank()) {
                        Text(nfcStatus, color = if (scannedCard != null) IncomeGreen else PremiumPurple, fontWeight = FontWeight.Bold)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EEFF), contentColor = Ink),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Cobras ${formatPz(parseMoneyInput(amount))} Pz en ${receiver.displayName}", fontWeight = FontWeight.Bold)
                            Text("Concepto: ${concept.ifBlank { "Sin concepto" }}", color = Color(0xFF6C5878))
                            Text(scannedCard?.let { "Tarjeta: ${it.iban} · ${it.code}" } ?: "Esperando tarjeta NFC...", color = if (scannedCard != null) IncomeGreen else Color(0xFF6C5878))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(18.dp)) { Text("Atrás", color = PremiumPurple) }
                        Button(
                            onClick = { onPay() },
                            enabled = scannedCard != null, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (scannedCard != null) IncomeGreen else PremiumPurple.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Contactless, contentDescription = null, tint = Mint)
                            Spacer(Modifier.size(8.dp))
                            Text(if (scannedCard != null) "COBRAR ${formatPz(parseMoneyInput(amount))} Pz" else "ESPERANDO TARJETA", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

private fun readPlacezumCard(tag: Tag): NfcPlacezumCard? {
    val isoDep = IsoDep.get(tag) ?: return null
    return runCatching {
        isoDep.connect()
        isoDep.timeout = 4_000
        val response = isoDep.transceive(PlacezumNfcProtocol.selectApdu)
        PlacezumNfcProtocol.parse(response)
    }.getOrNull().also {
        runCatching { isoDep.close() }
    }
}

private fun readPromoCardNumber(tag: Tag): String {
    val ndefDigits = runCatching {
        val ndef = Ndef.get(tag) ?: return@runCatching ""
        val message = ndef.cachedNdefMessage ?: return@runCatching ""
        message.records
            .asSequence()
            .mapNotNull { record ->
                val payload = record.payload ?: return@mapNotNull null
                when {
                    record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                        record.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT) &&
                        payload.isNotEmpty() -> {
                        val languageLength = payload[0].toInt() and 0x3F
                        payload.drop(1 + languageLength).toByteArray().toString(Charsets.UTF_8)
                    }
                    payload.isNotEmpty() -> payload.toString(Charsets.UTF_8)
                    else -> null
                }
            }
            .joinToString(" ")
            .filter(Char::isDigit)
    }.getOrDefault("")
    if (ndefDigits.length >= 6) return ndefDigits.takeLast(6)

    val uidHex = tag.id.joinToString("") { byte -> "%02X".format(byte) }
    val uidDigits = uidHex.map { char ->
        when (char) {
            in '0'..'9' -> char
            else -> ('0'.code + ((char.code - 'A'.code + 10) % 10)).toChar()
        }
    }.joinToString("")
    return uidDigits.padStart(6, '0').takeLast(6)
}

@Composable
private fun FavoriteBubble(account: Account) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(PremiumPurple, RoundedCornerShape(31.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(account.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
        }
        Text(account.displayName.take(9), color = Ink)
    }
}

@Composable
private fun PaymentSuccessOverlay(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).startTone(ToneGenerator.TONE_PROP_ACK, 220)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(IncomeGreen)
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(82.dp))
            Text("¡DINERO ENVIADO!", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Black)
            Text("Operación firmada y sincronizada", color = Color.White.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun GachaPendingCard() {
    val infinite = rememberInfiniteTransition(label = "gacha-pending")
    val pulse by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "gacha-pulse"
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3F00D8), contentColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size((46 * pulse).dp)
                    .background(Gold, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text("Sorpresa en curso", fontWeight = FontWeight.Black)
                Text("La inversión se revelará en 60 segundos.", color = Color(0xFFDCCAF7))
            }
        }
    }
}

@Composable
private fun InvestmentRevealSlideUp(result: InvestmentReveal, onDismiss: () -> Unit) {
    LaunchedEffect(result) {
        val tone = if (result.userWins) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).startTone(tone, 260)
    }
    val infinite = rememberInfiniteTransition(label = "investment-reveal")
    val pop by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "investment-pop"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "investment-glow"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pop)
            .border(2.dp, (if (result.userWins) IncomeGreen else ErrorRed).copy(alpha = glow), RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (result.userWins) Color(0xFFF4FFF8) else Color(0xFFFFF4F5),
            contentColor = Ink
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(86.dp)
                    .background(if (result.userWins) IncomeGreen.copy(alpha = 0.14f) else ErrorRed.copy(alpha = 0.14f), RoundedCornerShape(43.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (result.userWins) Icons.Default.TrendingUp else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.userWins) IncomeGreen else ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                if (result.userWins) "Resultado a favor" else "Resultado en contra",
                style = MaterialTheme.typography.headlineMedium,
                color = if (result.userWins) IncomeGreen else ErrorRed,
                fontWeight = FontWeight.Black
            )
            Text(result.assetName, color = Color(0xFF6C5878), fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultMetric("Movimiento", if (result.userWins) "+${result.movementPercent}%" else "-${result.movementPercent}%", Modifier.weight(1f), if (result.userWins) IncomeGreen else ErrorRed)
                ResultMetric("Neto devuelto", "${formatPz(result.amountPz)} Pz", Modifier.weight(1f), PremiumPurple)
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Cerrar resultado", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Color(0xFF6C5878), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InvestmentAnalysisCard(rows: List<InvestmentResultRow>) {
    val latest = rows.take(6)
    val net = latest.sumOf { it.netResultPz }
    val wins = latest.count { it.netResultPz >= 0 }
    CardBlock {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Análisis reciente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    if (latest.isEmpty()) "Aún no hay resultados cerrados"
                    else if (net >= 0) "Te está saliendo a ganar" else "Te está saliendo a perder",
                    color = Color(0xFF6C5878)
                )
            }
            Icon(if (net >= 0) Icons.Default.TrendingUp else Icons.Default.Warning, contentDescription = null, tint = if (net >= 0) IncomeGreen else ErrorRed)
        }
        if (latest.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultMetric("Balance 6 últimas", "${if (net >= 0) "+" else ""}${formatPz(net)} Pz", Modifier.weight(1f), if (net >= 0) IncomeGreen else ErrorRed)
                ResultMetric("Aciertos", "$wins/${latest.size}", Modifier.weight(1f), PremiumPurple)
            }
            latest.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.assetName, fontWeight = FontWeight.Bold)
                        Text("${formatPz(row.principalPz)} -> ${formatPz(row.returnedPz)} Pz · ${if (row.won) "+" else "-"}${row.movementPercent}%", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "${if (row.netResultPz >= 0) "+" else ""}${formatPz(row.netResultPz)}",
                        color = if (row.netResultPz >= 0) IncomeGreen else ErrorRed,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfettiField() {
    val infinite = rememberInfiniteTransition(label = "confetti")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Restart),
        label = "confetti-progress"
    )
    val colors = listOf(Gold, Mint, Color.White, Color(0xFFFF6EA8), Color(0xFF7CFF8A))
    Canvas(Modifier.fillMaxSize()) {
        repeat(42) { index ->
            val xSeed = ((index * 37) % 100) / 100f
            val ySeed = ((index * 19) % 100) / 100f
            val x = size.width * xSeed + sin((progress * 6.28f + index).toDouble()).toFloat() * 22f
            val y = (size.height * (ySeed + progress)) % size.height
            val piece = 6f + (index % 5) * 2f
            drawRect(
                color = colors[index % colors.size],
                topLeft = Offset(x, y),
                size = Size(piece, piece * 1.8f)
            )
        }
    }
}

@Composable
private fun SadFaceField() {
    val infinite = rememberInfiniteTransition(label = "sad-faces")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "sad-drift"
    )
    Canvas(Modifier.fillMaxSize()) {
        repeat(20) { index ->
            val x = size.width * (((index * 23) % 100) / 100f)
            val y = size.height * (((index * 31) % 100) / 100f) + cos((drift * 6.28f + index).toDouble()).toFloat() * 12f
            drawCircle(Color.White.copy(alpha = 0.18f), radius = 22f, center = Offset(x, y))
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 42.dp)) {
        items(List(12) { it }) {
            Text(":(", color = Color.White.copy(alpha = 0.34f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PortfolioPill(text: String, color: Color) {
    Box(
        Modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun PortfolioActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PremiumPurple),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun PortfolioDetailsSlideUp(
    account: Account,
    pendingCount: Int,
    resultRows: List<InvestmentResultRow>,
    dailyCount: Int,
    dailyLimit: Int,
    onDismiss: () -> Unit
) {
    val totalNet = resultRows.sumOf { it.netResultPz }
    val wins = resultRows.count { it.won }
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Portfolio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Cuenta de inversión", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultMetric("Saldo", formatPz(account.balancePz), Modifier.weight(1f), PremiumPurple)
                ResultMetric("Hoy", "$dailyCount/$dailyLimit", Modifier.weight(1f), PremiumPurple)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultMetric("Pendientes", pendingCount.toString(), Modifier.weight(1f), Gold)
                ResultMetric("Últimas", resultRows.size.toString(), Modifier.weight(1f), if (totalNet >= 0) IncomeGreen else ErrorRed)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = if (totalNet >= 0) IncomeGreen.copy(alpha = 0.10f) else ErrorRed.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (totalNet >= 0) "Vas ganando" else "Vas perdiendo", fontWeight = FontWeight.Black)
                        Text("$wins/${resultRows.size.coerceAtLeast(1)} operaciones positivas", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${if (totalNet >= 0) "+" else ""}${formatPz(totalNet)} Pz", color = if (totalNet >= 0) IncomeGreen else ErrorRed, fontWeight = FontWeight.Black)
                }
            }
            if (resultRows.isNotEmpty()) {
                Text("Últimas liquidaciones", fontWeight = FontWeight.Black)
                resultRows.take(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.assetName, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${formatPz(row.principalPz)} -> ${formatPz(row.returnedPz)} Pz", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "${if (row.netResultPz >= 0) "+" else ""}${formatPz(row.netResultPz)}",
                            color = if (row.netResultPz >= 0) IncomeGreen else ErrorRed,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FundDetailsSlideUp(
    company: Account,
    onInvest: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(company.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 2)
                    Text(if (company.listedInvestmentFund) "Fondo registrado GDLP" else "Empresa visible", color = Color(0xFF6C5878))
                }
                RiskBadge(company.investmentRiskLevel)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultMetric("Liquidez", formatPz(company.balancePz), Modifier.weight(1f), PremiumPurple)
                ResultMetric("Riesgo", "R${company.investmentRiskLevel.coerceIn(1, 7)}", Modifier.weight(1f), riskColor(company.investmentRiskLevel))
            }
            RiskIndicator(level = company.investmentRiskLevel)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Cerrar")
                }
                Button(onClick = onInvest, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Invertir")
                }
            }
        }
    }
}

@Composable
private fun TradeSlideUp(
    company: Account,
    investmentAccount: Account,
    tradeAmount: String,
    onTradeAmountChange: (String) -> Unit,
    dailyInvestmentCount: Int,
    config: TreasuryConfig,
    limits: InvestmentRiskLimits,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    val amount = parseMoneyInput(tradeAmount)
    val canStart = amount in 1..limits.maxAmountPz && dailyInvestmentCount < limits.dailyLimit
    val riskProfile = investmentRiskProfile(company.investmentRiskLevel)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Inversión 60s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(company.displayName, color = Color(0xFF6C5878), maxLines = 1)
                }
                RiskBadge(company.investmentRiskLevel)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultMetric("Disponible", formatPz(investmentAccount.balancePz), Modifier.weight(1f), PremiumPurple)
                ResultMetric("Límite R${limits.riskLevel}", formatPz(limits.maxAmountPz), Modifier.weight(1f), Gold)
            }
            OutlinedTextField(
                value = tradeAmount,
                onValueChange = onTradeAmountChange,
                label = { Text("Importe Pz") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(100L, 500L, 1000L, limits.maxAmountPz).filter { it <= limits.maxAmountPz }.distinct()) { quickAmount ->
                    FilterChip(
                        selected = amount == quickAmount,
                        onClick = { onTradeAmountChange(quickAmount.toString()) },
                        label = { Text(if (quickAmount == limits.maxAmountPz) "Máx" else formatPz(quickAmount)) }
                    )
                }
            }
            Text("60 segundos · ${dailyInvestmentCount}/${limits.dailyLimit} hoy · prob. usuario ${riskProfile.userWinProbabilityPercent}% · si gana +${riskProfile.winMovementMinPercent}-${riskProfile.winMovementMaxPercent}% · comisión ${config.investmentGainCommissionPercent}%", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Cancelar")
                }
                Button(
                    enabled = canStart,
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Iniciar")
                }
            }
        }
    }
}

@Composable
private fun BusinessFundRegistrationScreen(
    userAccounts: List<Account>,
    businessAccount: Account,
    transactions: List<LedgerTransaction>,
    onSelectedAccount: (String) -> Unit,
    onRegisterFund: (Account) -> Unit
) {
    var riskLevel by remember(businessAccount.id, businessAccount.investmentRiskLevel) {
        mutableStateOf(businessAccount.investmentRiskLevel.coerceIn(1, 7))
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AccountSwitcher(userAccounts, businessAccount.id, onSelectedAccount)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3F00D8), contentColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Alta como fondo", color = Mint, fontWeight = FontWeight.ExtraBold)
                        Text(businessAccount.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("Tu empresa podrá aparecer en la bolsa de inversiones 60s.", color = Color(0xFFD3EDE9))
                    }
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Gold, modifier = Modifier.size(42.dp))
                }
                RiskIndicator(level = riskLevel, light = true)
            }
        }
        CardBlock {
            Text("Riesgo del fondo", style = MaterialTheme.typography.titleMedium)
            Text("Como en los bancos, el indicador va de 1 a 7. Cuanto más alto, mayor variación potencial y mayor probabilidad de pérdidas para quien invierte.", color = Color(0xFF6C5878))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..7).toList()) { level ->
                    FilterChip(
                        selected = riskLevel == level,
                        onClick = { riskLevel = level },
                        label = { Text("Riesgo $level") }
                    )
                }
            }
            RiskWarningBox(riskLevel)
            Button(
                onClick = {
                    onRegisterFund(
                        businessAccount.copy(
                            listedInvestmentFund = true,
                            investmentRiskLevel = riskLevel
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ShowChart, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (businessAccount.listedInvestmentFund) "Actualizar ficha de fondo" else "Dar de alta como fondo", fontWeight = FontWeight.Black)
            }
        }
        BusinessInvestmentAnalyticsCard(businessAccount, transactions)
        CardBlock {
            Text("Responsabilidad", fontWeight = FontWeight.Black)
            Text("El alta expone a la empresa a liquidaciones contra su liquidez declarada. Si no hay saldo suficiente, la liquidación puede fallar y afectar la confianza del fondo.", color = Color(0xFF6C5878))
        }
    }
}

@Composable
private fun BusinessInvestmentAnalyticsCard(
    businessAccount: Account,
    transactions: List<LedgerTransaction>
) {
    val buys = transactions.filter { it.toAccountId == businessAccount.id && it.kind == TransactionKind.InvestmentBuy }
    val payouts = transactions.filter { it.fromAccountId == businessAccount.id && it.kind == TransactionKind.InvestmentSell }
    val captured = buys.sumOf { it.amountPz }
    val paid = payouts.sumOf { it.amountPz }
    val net = captured - paid
    val roi = if (captured > 0) (net.toDouble() / captured.toDouble()) * 100.0 else 0.0
    CardBlock {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Analítica del fondo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(if (net >= 0) "La bolsa te está saliendo rentable" else "La bolsa está pagando más de lo captado", color = Color(0xFF6C5878))
            }
            Text("${if (net >= 0) "+" else ""}${formatPz(net)} Pz", color = if (net >= 0) IncomeGreen else ErrorRed, fontWeight = FontWeight.Black)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultMetric("Captado", formatPz(captured), Modifier.weight(1f), PremiumPurple)
            ResultMetric("Pagado", formatPz(paid), Modifier.weight(1f), ErrorRed)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultMetric("Operaciones", buys.size.toString(), Modifier.weight(1f), Gold)
            ResultMetric("Rentab.", "${"%.1f".format(roi)}%", Modifier.weight(1f), if (net >= 0) IncomeGreen else ErrorRed)
        }
        payouts.sortedByDescending { it.createdAt }.take(3).forEach { tx ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tx.note.ifBlank { "Liquidación inversión" }, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(HistoryDateFormat.format(tx.createdAt.atZone(ZoneId.systemDefault())), color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                }
                Text("-${formatPz(tx.amountPz)}", color = ErrorRed, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun FundMarketRow(
    company: Account,
    selected: Boolean,
    onClick: () -> Unit,
    onInvest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, if (selected) PremiumPurple else PremiumPurple.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = if (selected) SurfacePurple else Color.White, contentColor = Ink),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).background(PremiumPurple.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PremiumPurple)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(company.displayName, fontWeight = FontWeight.Black, maxLines = 1)
                Text("${formatPz(company.balancePz)} Pz · ${if (company.listedInvestmentFund) "Fondo registrado" else "Empresa visible"}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RiskBadge(company.investmentRiskLevel)
                IconButton(onClick = onInvest, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = "Invertir", tint = PremiumPurple)
                }
            }
        }
    }
}

private fun List<Account>.averageRiskLabel(): String =
    if (isEmpty()) "-" else "%.1f/7".format(map { it.investmentRiskLevel.coerceIn(1, 7) }.average())

@Composable
private fun RiskBadge(level: Int) {
    val safeLevel = level.coerceIn(1, 7)
    val color = riskColor(safeLevel)
    Box(
        Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("R$safeLevel", color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RiskIndicator(level: Int, light: Boolean = false) {
    val safeLevel = level.coerceIn(1, 7)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Indicador de riesgo", color = if (light) Color.White else Ink, fontWeight = FontWeight.Bold)
            Text("$safeLevel/7", color = riskColor(safeLevel), fontWeight = FontWeight.Black)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            (1..7).forEach { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(if (index <= safeLevel) riskColor(index) else Color(0xFFE7DDF4), RoundedCornerShape(5.dp))
                )
            }
        }
        Text(riskDescription(safeLevel), color = if (light) Color(0xFFD3EDE9) else Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RiskWarningBox(level: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = riskColor(level).copy(alpha = 0.10f), contentColor = Ink),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = riskColor(level))
            Text(riskDescription(level), color = Color(0xFF6C5878), modifier = Modifier.weight(1f))
        }
    }
}

private fun riskColor(level: Int): Color = when (level.coerceIn(1, 7)) {
    1, 2 -> IncomeGreen
    3, 4 -> Gold
    5 -> Color(0xFFFF8A00)
    else -> ErrorRed
}

private fun riskDescription(level: Int): String = when (level.coerceIn(1, 7)) {
    1 -> "Riesgo muy bajo: variación limitada, pérdidas menos probables."
    2 -> "Riesgo bajo: exposición moderada a movimientos negativos."
    3 -> "Riesgo medio-bajo: puede fluctuar, adecuado para importes prudentes."
    4 -> "Riesgo medio: equilibrio entre probabilidad y resultado potencial."
    5 -> "Riesgo alto: ganar es menos probable, pero el resultado a favor sube."
    6 -> "Riesgo muy alto: probabilidad menor para el usuario y pago potencial superior."
    else -> "Riesgo extremo: probabilidad baja para el usuario, con el mayor porcentaje si acierta."
}

private data class InvestmentRiskLimits(
    val riskLevel: Int,
    val allowedPercent: Int,
    val maxAmountPz: Long,
    val dailyLimit: Int
)

private data class InvestmentRiskProfile(
    val riskLevel: Int,
    val userWinProbabilityPercent: Int,
    val companyWinProbabilityPercent: Int,
    val winMovementMinPercent: Int,
    val winMovementMaxPercent: Int
)

private fun investmentRiskLimits(config: TreasuryConfig, riskLevel: Int): InvestmentRiskLimits {
    val safeRisk = riskLevel.coerceIn(1, 7)
    val allowedPercent = (100 - (safeRisk - 1) * 10).coerceIn(40, 100)
    return InvestmentRiskLimits(
        riskLevel = safeRisk,
        allowedPercent = allowedPercent,
        maxAmountPz = ((config.maxInvestmentAmountPz * allowedPercent) / 100).coerceAtLeast(1),
        dailyLimit = ((config.dailyInvestmentLimit * allowedPercent) / 100).coerceAtLeast(1)
    )
}

private fun investmentRiskProfile(riskLevel: Int, economyWeight: Int = 0): InvestmentRiskProfile {
    val safeRisk = riskLevel.coerceIn(1, 7)
    val userWinProbabilityPercent = (78 - (safeRisk - 1) * 8).coerceIn(30, 78)
    val minMovement = (3 + (safeRisk - 1) * 3).coerceIn(3, 45)
    val maxMovement = (7 + (safeRisk - 1) * 5 + economyWeight.coerceAtLeast(0)).coerceIn(minMovement, 60)
    return InvestmentRiskProfile(
        riskLevel = safeRisk,
        userWinProbabilityPercent = userWinProbabilityPercent,
        companyWinProbabilityPercent = 100 - userWinProbabilityPercent,
        winMovementMinPercent = minMovement,
        winMovementMaxPercent = maxMovement
    )
}

@Composable
private fun HubScreen(
    activeUser: UserProfile,
    accounts: Map<String, Account>,
    selectedAccountId: String,
    onSelectedAccount: (String) -> Unit,
    transactions: List<LedgerTransaction>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    upsertDigitalCard: (DigitalCard) -> Unit,
    users: List<UserProfile> = emptyList(),
    savedContacts: List<SavedContact> = emptyList(),
    payrollContracts: List<PayrollContract> = emptyList(),
    payrollPeriods: List<PayrollPeriod> = emptyList(),
    upsertPayrollContract: (PayrollContract) -> Unit = {},
    upsertPayrollPeriod: (PayrollPeriod) -> Unit = {},
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit,
    onCreatePaymentLink: ((String, String, Long, String, (String?, String?) -> Unit) -> Unit)? = null,
    accountHolders: List<AccountHolder> = emptyList(),
    onAddAccountHolder: (AccountHolder) -> Unit = {},
    onRemoveAccountHolder: (String) -> Unit = {}
) {
    val userAccounts = accounts.values.filter { it.isActiveAccount() && it.isOwnedBy(activeUser) }.sortedBy { it.type.ordinal }
    val account = userAccounts.firstOrNull { it.id == selectedAccountId }
        ?: userAccounts.firstOrNull { it.id == activeUser.primaryAccountId }
        ?: userAccounts.firstOrNull()
        ?: return
    val scope = rememberCoroutineScope()
    val businessAccounts = userAccounts.filter { it.type == AccountType.Business }
    val foundation = accounts[FOUNDATION_RBU_ID] ?: accounts[AGLDP_ID]
    val context = LocalContext.current
    var showProductPanel by remember { mutableStateOf(false) }
    var showMorePanel by remember { mutableStateOf(false) }
    var payrollAmount by remember { mutableStateOf("1000") }
    var donationAmount by remember { mutableStateOf("25") }
    val accountTransactions = transactionsForAccount(transactions, account.id)
    val accountContracts = payrollContracts.filter { it.companyAccountId == account.id || it.employeeAccountId == account.id }
    val accountPeriods = payrollPeriods.filter { it.companyAccountId == account.id || it.employeeAccountId == account.id }
    val documents = remember(account.id, transactions.size, payrollContracts.size, payrollPeriods.size) {
        contextualDocuments(account, accountTransactions, accountContracts, accountPeriods)
    }
    Box {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Hub de Servicios")
        AccountSwitcher(userAccounts, account.id, onSelectedAccount)
        QuickAction(Icons.Default.MoreHoriz, "Más", Modifier.fillMaxWidth()) { showMorePanel = true }
        CardBlock {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = PremiumPurple)
                Column {
                    Text("Chat con Hacienda", fontWeight = FontWeight.Bold)
                    Text("Guías interactivas, reclamaciones y estado fiscal.", color = Color(0xFF6C5878))
                }
            }
        }
        SectionTitle("Mis Impuestos")
        WeeklyTaxEstimateCard(account = account, config = config, engine = engine)
        CardBlock {
            Text("Generador de Certificados", style = MaterialTheme.typography.titleMedium)
            Text("Certificado de Solvencia con sello oficial GDLP listo para descarga.", color = Color(0xFF6C5878))
            Button(
                onClick = {
                    val solvency = DigitalDocument("account-solvency-${account.id}", account.id, "Solvencia · ${account.iban}", DocumentKind.SolvencyCertificate)
                    show(PdfExporter.createAndOpenTaxPdf(context, account, solvency, transactionsForAccount(transactions, account.id), config))
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("DESCARGAR CERTIFICADO")
            }
        }
        SectionTitle("Configuración de Cuentas")
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tus productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("Selecciona arriba para ver documentos y operaciones de una cuenta.", color = Color(0xFF6C5878))
                }
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PremiumPurple)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.10f), contentColor = Ink),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(account.displayName, fontWeight = FontWeight.Black)
                        Text("${account.type.label()} · ${account.iban}", color = Color(0xFF6C5878))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${formatPz(account.balancePz)} Pz", color = PremiumPurple, fontWeight = FontWeight.Black)
                        Text(if (account.huchaLocked) "Hucha" else "Operativa", color = Color(0xFF6C5878))
                    }
                }
            }
        }
        if (account.type != AccountType.Business && account.type != AccountType.State) {
            CoOwnerManagementPanel(
                account = account,
                holders = accountHolders,
                users = users,
                onAddHolder = onAddAccountHolder,
                onRemoveHolder = onRemoveAccountHolder,
                show = show
            )
        }
        if (account.type in setOf(AccountType.Business, AccountType.State)) {
            val payrollGross = parseMoneyInput(payrollAmount)
            val payrollWorkerTax = payrollTax(payrollGross, config.payrollWorkerTaxPercent)
            val payrollEmployerTax = payrollTax(payrollGross, config.payrollEmployerTaxPercent)
            val payrollNet = (payrollGross - payrollWorkerTax).coerceAtLeast(0L)
            val payrollIssuerLabel = if (account.type == AccountType.State) "Estado" else "Empresa"
            SectionTitle("Panel $payrollIssuerLabel")
            CardBlock {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(account.displayName, fontWeight = FontWeight.Black)
                        Text("${account.iban} · ${formatPz(account.balancePz)} Pz", color = Color(0xFF6C5878))
                    }
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PremiumPurple)
                }
                OutlinedTextField(
                    value = payrollAmount,
                    onValueChange = { payrollAmount = sanitizeMoneyInput(it) },
                    label = { Text("Nómina bruta semanal Pz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ResultMetric("Neto empleado", "${formatPz(payrollNet)} Pz", Modifier.weight(1f), IncomeGreen)
                    ResultMetric("Coste empresa", "${formatPz(payrollGross + payrollEmployerTax)} Pz", Modifier.weight(1f), PremiumPurple)
                }
                Text(
                    "Tributos GDLP: trabajador ${config.payrollWorkerTaxPercent}% (${formatPz(payrollWorkerTax)} Pz) · ${payrollIssuerLabel.lowercase()} ${config.payrollEmployerTaxPercent}% (${formatPz(payrollEmployerTax)} Pz).",
                    color = Color(0xFF6C5878),
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        val current = userAccounts.firstOrNull { it.type == AccountType.Current } ?: account
                        apply(engine.transferPayrollOrLoan(accounts, account.id, current.id, payrollGross, "Nómina ${payrollIssuerLabel.lowercase()} ${account.displayName}"))
                    },
                    enabled = payrollGross >= 150L,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("PAGAR NÓMINA")
                }
            }
        }
        documents.forEach { doc ->
            CardBlock {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Mint)
                    Column(Modifier.weight(1f)) {
                        Text(doc.title, fontWeight = FontWeight.Bold)
                        Text("${doc.kind.name} · PDF firmado por el Grupo", color = Color(0xFF6C5878))
                    }
                    IconButton(
                        onClick = {
                            show(PdfExporter.createAndOpenTaxPdf(context, account, doc, transactionsForAccount(transactions, account.id), config))
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Descargar PDF", tint = PremiumPurple)
                    }
                }
            }
        }
    }
        BottomSlideUpOverlay(visible = showMorePanel, onDismiss = { showMorePanel = false }) {
            MoreServicesSlideUp(
                account = account,
                documents = documents,
                paymentTransactions = transactionsForAccount(transactions, account.id),
                accounts = userAccounts,
                allAccounts = accounts,
                users = users,
                accountHolders = accountHolders,
                onAddAccountHolder = onAddAccountHolder,
                onRemoveAccountHolder = onRemoveAccountHolder,
                investmentTransactions = transactionsForAccount(transactions, account.id).filter { it.kind == TransactionKind.InvestmentBuy || it.kind == TransactionKind.InvestmentSell },
                digitalCards = emptyList(),
                config = config,
                engine = engine,
                payrollTargets = accounts.values.filter { it.isActiveAccount() && it.type == AccountType.Current && it.id != account.id },
                savedContacts = savedContacts,
                payrollContracts = payrollContracts.filter { it.companyAccountId == account.id },
                payrollPeriods = payrollPeriods.filter { it.companyAccountId == account.id },
                donationAmount = donationAmount,
                activeUser = activeUser,
                onDonationAmount = { donationAmount = sanitizeMoneyInput(it) },
                onClose = { showMorePanel = false },
                onNewProduct = {
                    showMorePanel = false
                    showProductPanel = true
                },
                onSupportTicket = { ticketInfo ->
                    val parts = ticketInfo.split(" · ")
                    val ticketId = parts.getOrElse(0) { "SUP-000000" }
                    scope.launch {
                        try {
                            val json = org.json.JSONObject().apply {
                                put("id", ticketId)
                                put("category", parts.getOrElse(1) { "General" })
                                put("priority", parts.getOrElse(2) { "Media" })
                                put("subject", parts.getOrElse(3) { "" })
                                put("message", ticketInfo)
                                put("dip", activeUser.dip)
                                put("name", activeUser.displayName)
                            }
                            val url = URL("http://127.0.0.1:18731/api/support-tickets")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.doOutput = true
                            conn.setRequestProperty("Content-Type", "application/json")
                            conn.outputStream.write(json.toString().toByteArray())
                            val code = conn.responseCode
                            conn.disconnect()
                            if (code == 200) show("✅ Ticket enviado: $ticketId")
                            else show("✅ Ticket creado localmente: $ticketId")
                        } catch (_: Exception) {
                            show("✅ Ticket creado: $ticketId (admin offline)")
                        }
                    }
                },
                onRegisterPayroll = { target, amount ->
                    apply(engine.transferPayrollOrLoan(accounts, account.id, target.id, amount, "Nómina registrada ${account.displayName} -> ${target.displayName}"))
                    show("Nómina registrada para ${target.displayName}")
                },
                onPayrollContract = upsertPayrollContract,
                onPayrollPeriod = upsertPayrollPeriod,
                onPayPayrollPeriod = { period ->
                    val result = engine.transferPayrollOrLoan(
                        accounts,
                        period.companyAccountId,
                        period.employeeAccountId,
                        period.grossSalaryPz,
                        "Nómina ${period.label} · DIP ${period.employeeDip}"
                    )
                    when (result) {
                        is EconomyResult.Failure -> show(result.message)
                        is EconomyResult.Success -> {
                            apply(result)
                            upsertPayrollPeriod(period.copy(status = PayrollPeriodStatus.Paid, paidAt = Instant.now(), transactionId = result.value.id))
                            val employee = accounts[period.employeeAccountId] ?: account
                            val doc = DigitalDocument("payroll-${period.id}", employee.id, "Nómina ${period.label}", DocumentKind.PayrollPayslip)
                            show(PdfExporter.createAndOpenTaxPdf(context, employee, doc, listOf(result.value), config))
                        }
                    }
                },
                onPayrollContractPdf = { contract ->
                    val employee = accounts[contract.employeeAccountId] ?: account
                    val doc = DigitalDocument("alta-${contract.id}", employee.id, "Alta laboral ${contract.employeeName}", DocumentKind.LaborContract)
                    show(PdfExporter.createAndOpenTaxPdf(context, employee, doc, emptyList(), config, contract))
                },
                onDonate = {
                    val destination = foundation
                    if (destination == null) {
                        show("Fundación no disponible")
                    } else {
                        apply(engine.transferByIban(accounts, account.id, destination.iban, parseMoneyInput(donationAmount), TransactionKind.Donation, "Donación a Fundación Banco de La Placeta para RBU"))
                        showMorePanel = false
                    }
                },
                onDocument = { doc ->
                    show(PdfExporter.createAndOpenTaxPdf(context, account, doc, transactionsForAccount(transactions, account.id), config))
                },
                onTransactionReceipt = { transaction ->
                    val doc = DigitalDocument("transfer-${transaction.id}", account.id, "Justificante ${transaction.kind.name} · ${formatPz(transaction.amountPz)} Pz", DocumentKind.PaymentReceipt)
                    show(PdfExporter.createAndOpenTaxPdf(context, account, doc, listOf(transaction), config))
                },
                onCreatePaymentLink = onCreatePaymentLink
            )
        }
        BottomSlideUpOverlay(visible = showProductPanel, onDismiss = { showProductPanel = false }) {
            ProductSlideUp(
                account = account,
                accounts = accounts,
                activeUser = activeUser,
                config = config,
                engine = engine,
                onClose = { showProductPanel = false },
                onCreated = { result, tier ->
                    apply(result)
                    val created = (result as? EconomyResult.Success)?.value as? Account
                    if (created != null) {
                        upsertDigitalCard(DigitalCard("card-${created.id}", created.id, "${created.displayName} Card", tier))
                        val contract = DigitalDocument("contract-${created.id}", created.id, "Contrato ${created.type.label()} ${created.displayName}", DocumentKind.LoanContract)
                        show(PdfExporter.createAndOpenTaxPdf(context, created, contract, transactionsForAccount(transactions, created.id), config))
                        showProductPanel = false
                        show("Producto dado de alta: ${created.iban}")
                    }
                }
            )
        }
    }
}

@Composable
private fun SociedadesScreen(
    activeUser: UserProfile,
    accounts: Map<String, Account>,
    selectedAccountId: String,
    onSelectedAccount: (String) -> Unit,
    transactions: List<LedgerTransaction>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    users: List<UserProfile>,
    savedContacts: List<SavedContact>,
    payrollContracts: List<PayrollContract>,
    payrollPeriods: List<PayrollPeriod>,
    upsertPayrollContract: (PayrollContract) -> Unit,
    upsertPayrollPeriod: (PayrollPeriod) -> Unit,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val context = LocalContext.current
    val businessAccounts = accounts.values
        .filter { it.isActiveAccount() && (it.isOwnedBy(activeUser) || it.placetaId == activeUser.placetaId) && it.type == AccountType.Business }
        .sortedBy { it.id }
    val selected = businessAccounts.firstOrNull { it.id == selectedAccountId } ?: businessAccounts.firstOrNull()
    var bizTab by remember { mutableStateOf("resumen") }
    val bizTabs = listOf("resumen" to "Resumen", "trabajadores" to "Trabajadores", "facturacion" to "Facturación", "pedidos" to "Pedidos", "gestores" to "Gestores")
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Gestión de Sociedades")
        if (businessAccounts.isEmpty()) {
            EmptyState(Icons.Default.AccountBalance, "Sin sociedades", "No tienes acceso a ninguna cuenta de empresa activa. Solicita acceso o crea una desde Más > Dar de alta producto.")
        } else {
            AccountSwitcher(businessAccounts, selected?.id.orEmpty(), onSelectedAccount)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bizTabs) { (key, label) ->
                    FilterChip(selected = bizTab == key, onClick = { bizTab = key }, label = { Text(label) })
                }
            }
            selected?.let { company ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B69), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("SOCIEDAD ACTIVA", color = Gold, fontWeight = FontWeight.ExtraBold)
                                Text(company.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                Text(company.iban, color = Color(0xFFDCCAF7))
                            }
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Mint, modifier = Modifier.size(42.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ResultMetric("Saldo", formatPz(company.balancePz), Modifier.weight(1f), Gold)
                            ResultMetric("Riesgo", "R${company.investmentRiskLevel}", Modifier.weight(1f), riskColor(company.investmentRiskLevel))
                        }
                    }
                }
                
                when (bizTab) {
                    "resumen" -> {
                        BusinessInvestmentAnalyticsCard(company, transactions)
                        CardBlock {
                            Text("Herramientas de empresa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("Gestiona nóminas, facturación, empleados y gestores desde las pestañas superiores.", color = Color(0xFF6C5878))
                        }
                    }
                    "trabajadores" -> {
                        PayrollRegistrationPanel(
                    company = company,
                    targets = accounts.values.filter { it.isActiveAccount() && it.type == AccountType.Current && it.id != company.id },
                    allAccounts = accounts,
                    users = users,
                    savedContacts = savedContacts,
                    activeUser = activeUser,
                    contracts = payrollContracts.filter { it.companyAccountId == company.id },
                    periods = payrollPeriods.filter { it.companyAccountId == company.id },
                    config = config,
                    engine = engine,
                    onRegister = { target, amount ->
                        apply(engine.transferPayrollOrLoan(accounts, company.id, target.id, amount, "Nómina rápida ${company.displayName} -> ${target.displayName}"))
                    },
                    onContract = upsertPayrollContract,
                    onPeriod = upsertPayrollPeriod,
                    onPayPeriod = { period ->
                        val result = engine.transferPayrollOrLoan(accounts, period.companyAccountId, period.employeeAccountId, period.grossSalaryPz, "Nómina ${period.label}")
                        apply(result)
                        if (result is EconomyResult.Success) {
                            upsertPayrollPeriod(period.copy(status = PayrollPeriodStatus.Paid, paidAt = Instant.now(), transactionId = result.value.id))
                            show("Nómina abonada")
                        }
                    },
                    onContractPdf = { contract ->
                        val doc = DigitalDocument("alta-${contract.id}", company.id, "Alta laboral ${contract.employeeName}", DocumentKind.LaborContract)
                        show(PdfExporter.createAndOpenTaxPdf(context, company, doc, emptyList(), config, contract))
                    }
                )
                    }
                    "facturacion" -> {
                        BusinessInvoicePanel(
                            company = company,
                            transactions = transactions,
                            config = config,
                            accounts = accounts,
                            engine = engine,
                            authenticateTransfer = authenticateTransfer,
                            show = show,
                            apply = apply
                        )
                    }
                    "pedidos" -> {
                        val companyTx = transactions.filter { it.fromAccountId == company.id || it.toAccountId == company.id }
                        BusinessOrdersPanel(
                            company = company,
                            transactions = companyTx,
                            config = config,
                            engine = engine,
                            accounts = accounts,
                            activeUser = activeUser,
                            authenticateTransfer = authenticateTransfer,
                            show = show,
                            apply = apply,
                            context = context
                        )
                    }
                    "gestores" -> {
                        BusinessManagersPanel(company, activeUser, users, accounts, savedContacts)
                    }
                }
            }
        }
    }
}

@Composable
private fun EstatalScreen(
    activeUser: UserProfile,
    accounts: Map<String, Account>,
    selectedAccountId: String,
    onSelectedAccount: (String) -> Unit,
    transactions: List<LedgerTransaction>,
    flags: List<ComplianceFlag>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val stateAccounts = accounts.values
        .filter { it.isActiveAccount() && (it.isOwnedBy(activeUser) || it.role == Role.Tributos) && (it.type == AccountType.State || it.role == Role.Tributos) }
        .sortedBy { it.type.ordinal }
    val selected = stateAccounts.firstOrNull { it.id == selectedAccountId } ?: stateAccounts.firstOrNull()
    var stateTab by remember { mutableStateOf("tesoreria") }
    val stateTabs = listOf("tesoreria" to "Tesorería", "cumplimiento" to "Cumplimiento", "sanciones" to "Sanciones")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Herramientas Estatales")
        if (stateAccounts.isEmpty()) {
            EmptyState(Icons.Default.AssuredWorkload, "Sin acceso estatal", "No tienes permisos de administración o cuentas institucionales vinculadas.")
        } else {
            AccountSwitcher(stateAccounts, selected?.id.orEmpty(), onSelectedAccount)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stateTabs) { (key, label) ->
                    FilterChip(selected = stateTab == key, onClick = { stateTab = key }, label = { Text(label) })
                }
            }
            selected?.let { account ->
                VaultCard(
                    title = "CONTROL INSTITUCIONAL",
                    amount = account.balancePz,
                    subtitle = "${account.displayName} · ${account.role.name}"
                )
                when (stateTab) {
                    "tesoreria" -> {
                        CardBlock {
                            Text("Tesorería", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            val stateTx = transactions.filter { it.fromAccountId == account.id || it.toAccountId == account.id }
                            if (stateTx.isEmpty()) {
                                Text("No hay movimientos recientes en esta cuenta.", color = Color(0xFF6C5878))
                            } else {
                                stateTx.take(5).forEach { tx ->
                                    val isOutgoing = tx.fromAccountId == account.id
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isOutgoing) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (isOutgoing) ErrorRed else IncomeGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.size(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(tx.concept.ifBlank { tx.kind.name }, fontWeight = FontWeight.Bold)
                                            Text(formatTimestamp(tx.createdAt), color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(
                                            "${if (isOutgoing) "-" else "+"}${formatPz(tx.amountPz)}",
                                            fontWeight = FontWeight.Black,
                                            color = if (isOutgoing) ErrorRed else IncomeGreen
                                        )
                                    }
                                }
                            }
                        }
                        CardBlock {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                QuickAction(Icons.Default.Search, "Auditar cuentas", Modifier.weight(1f)) {
                                    show("Auditoría: Revisa las transacciones de todas las cuentas desde el panel de control del estado.")
                                }
                                QuickAction(Icons.Default.AccountBalance, "Emitir", Modifier.weight(1f)) {
                                    show("Emisión: Crea movimientos institucionales desde la administración central.")
                                }
                            }
                        }
                    }
                    "cumplimiento" -> {
                        CardBlock {
                            Text("Alertas de cumplimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("Revisa banderas de incidencias en cuentas ciudadanas.", color = Color(0xFF6C5878))
                            val pending = flags.filter { it.status != com.placeta.banco.core.ComplianceStatus.Approved }
                            if (pending.isEmpty()) {
                                Text("No hay banderas de revisión pendientes.", color = Color(0xFF6C5878))
                            } else {
                                pending.take(8).forEach { flag ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(accounts[flag.accountId]?.displayName ?: flag.accountId, fontWeight = FontWeight.Bold)
                                            Text("${flag.reason} · ${formatPz(flag.amountPz)} Pz", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                                            Text(formatTimestamp(flag.createdAt), color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                    "sanciones" -> {
                        CardBlock {
                            Text("Sanciones y Ejecuciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("Gestiona sanciones sobre cuentas con incidencias. Las sanciones generan códigos de ejecución.", color = Color(0xFF6C5878))
                        }
                        CardBlock {
                            var sanctionTarget by remember { mutableStateOf("") }
                            var sanctionReason by remember { mutableStateOf("") }
                            var sanctionAmount by remember { mutableStateOf("") }
                            var sanctionCode by remember { mutableStateOf("") }
                            var sanctionsList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

                            Text("Nueva sanción", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = sanctionTarget,
                                onValueChange = { sanctionTarget = it.take(30) },
                                label = { Text("IBAN o PlacetaID destino") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            OutlinedTextField(
                                value = sanctionAmount,
                                onValueChange = { sanctionAmount = sanitizeMoneyInput(it) },
                                label = { Text("Importe Pz") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            OutlinedTextField(
                                value = sanctionReason,
                                onValueChange = { sanctionReason = it.take(100) },
                                label = { Text("Motivo") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        sanctionTarget = ""
                                        sanctionAmount = ""
                                        sanctionReason = ""
                                        sanctionCode = ""
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) { Text("Limpiar") }
                                Button(
                                    onClick = {
                                        val pz = parseMoneyInput(sanctionAmount)
                                        if (pz <= 0 || sanctionTarget.isBlank() || sanctionReason.isBlank()) {
                                            show("Completa todos los campos")
                                            return@Button
                                        }
                                        val code = "SANC-${(1000..9999).random()}"
                                        sanctionCode = code
                                        sanctionsList = sanctionsList + Pair(code, sanctionReason)
                                        show("Sanción creada: $code por ${formatPz(pz)} Pz a $sanctionTarget")
                                        sanctionTarget = ""
                                        sanctionAmount = ""
                                        sanctionReason = ""
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    shape = RoundedCornerShape(16.dp)
                                ) { Text("Ejecutar sanción") }
                            }
                            if (sanctionCode.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Código de ejecución", fontWeight = FontWeight.Bold, color = ErrorRed)
                                        Text(sanctionCode, fontWeight = FontWeight.ExtraBold, color = ErrorRed, style = MaterialTheme.typography.headlineSmall)
                                        Text("Este código debe registrarse en el expediente del ciudadano.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (sanctionsList.size > 1) {
                                Spacer(Modifier.height(8.dp))
                                Text("Historial de sanciones", fontWeight = FontWeight.Bold)
                                sanctionsList.forEach { (code, reason) ->
                                    Row(Modifier.fillMaxWidth().background(ErrorRed.copy(alpha = 0.04f), RoundedCornerShape(10.dp)).padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(code, fontWeight = FontWeight.Bold, color = ErrorRed)
                                            Text(reason, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Icon(Icons.Default.Gavel, contentDescription = null, tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessInvoicePanel(
    company: Account,
    transactions: List<LedgerTransaction>,
    config: TreasuryConfig,
    accounts: Map<String, Account>,
    engine: EconomyEngine,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val invoiceTransactions = transactions.filter { it.fromAccountId == company.id || it.toAccountId == company.id }
    val totalInvoiced = invoiceTransactions.sumOf { it.amountPz }
    val totalReceived = invoiceTransactions.filter { it.fromAccountId == company.id }.sumOf { it.amountPz }
    val totalSent = invoiceTransactions.filter { it.toAccountId == company.id }.sumOf { it.amountPz }
    var showCreateLink by remember { mutableStateOf(false) }
    var linkAmount by remember { mutableStateOf("") }
    var linkConcept by remember { mutableStateOf("") }
    var linkTitle by remember { mutableStateOf("") }
    var createdLinks by remember { mutableStateOf<List<Triple<String, String, Long>>>(emptyList()) }
    var showQrSlideUp by remember { mutableStateOf(false) }
    var qrLinkData by remember { mutableStateOf<Triple<String, String, Long>?>(null) }
    val context = LocalContext.current
    val hasNfc = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) && NfcAdapter.getDefaultAdapter(context) != null
    }
    var tpvAmount by remember { mutableStateOf("") }
    var tpvConcept by remember { mutableStateOf("") }
    var tpvMode by remember { mutableStateOf<String?>(null) } // "reader" o "terminal"
    var nfcCard by remember { mutableStateOf<NfcPlacezumCard?>(null) }
    var nfcStatus by remember { mutableStateOf("") }

    DisposableEffect(tpvMode, hasNfc, company.id) {
        val activity = context as? android.app.Activity
        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (tpvMode == "terminal" && hasNfc && activity != null && adapter != null) {
            nfcStatus = "Datafono esperando tarjeta PlaceZum NFC..."
            val callback = NfcAdapter.ReaderCallback { tag ->
                val detected = readPlacezumCard(tag)
                activity.runOnUiThread {
                    if (detected == null) {
                        nfcStatus = "Tarjeta NFC no compatible"
                    } else {
                        nfcCard = detected
                        nfcStatus = "Tarjeta leída: ${detected.code} · ${detected.iban}"
                    }
                }
            }
            adapter.enableReaderMode(
                activity, callback,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null
            )
        }
        onDispose {
            if (tpvMode == "terminal" && activity != null && adapter != null) {
                adapter.disableReaderMode(activity)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardBlock {
            Text("Facturación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultMetric("Facturado", formatPz(totalInvoiced), Modifier.weight(1f), PremiumPurple)
                ResultMetric("Recibido", formatPz(totalReceived), Modifier.weight(1f), IncomeGreen)
            }
        }
        CardBlock {
            Text("Enlaces de pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Crea enlaces para cobrar a clientes.", color = Color(0xFF6C5878))
            if (showCreateLink) {
                OutlinedTextField(
                    value = linkTitle,
                    onValueChange = { linkTitle = it.take(60) },
                    label = { Text("Título (ej: Servicio)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = linkAmount,
                    onValueChange = { linkAmount = sanitizeMoneyInput(it) },
                    label = { Text("Importe Pz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showCreateLink = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val pz = parseMoneyInput(linkAmount)
                            if (pz <= 0 || linkTitle.isBlank()) return@Button
                            val linkId = "LNK-${company.id.take(4)}-${(1000..9999).random()}"
                            val newLink = Triple(linkId, linkTitle, pz)
                            createdLinks = createdLinks + newLink
                            qrLinkData = newLink
                            showQrSlideUp = true
                            showCreateLink = false
                            linkAmount = ""
                            linkTitle = ""
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Crear enlace") }
                }
            } else {
                Button(
                    onClick = { showCreateLink = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Nuevo enlace de pago", fontWeight = FontWeight.Black)
                }
            }
            if (createdLinks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Enlaces activos", fontWeight = FontWeight.Bold, color = PremiumPurple)
                createdLinks.forEach { (id, title, amount) ->
                    Row(Modifier.fillMaxWidth().background(PremiumPurple.copy(alpha = 0.06f), RoundedCornerShape(12.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(id, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatPz(amount), fontWeight = FontWeight.Black, color = PremiumPurple)
                    }
                }
            }
        }
        CardBlock {
            Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultMetric("Facturado", formatPz(totalInvoiced), Modifier.weight(1f), PremiumPurple)
                ResultMetric("Recibido", formatPz(totalReceived), Modifier.weight(1f), IncomeGreen)
            }
        }
        // TPV NFC Empresa funcional
        CardBlock {
            Text("TPV NFC Empresa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Cobra con tu móvil como datafono. Importe y concepto antes de activar.", color = Color(0xFF6C5878))
            Text("Límite por operación: ${formatPz(config.contactlessLimitPz)} Pz", color = PremiumPurple, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tpvAmount,
                onValueChange = { tpvAmount = sanitizeMoneyInput(it) },
                label = { Text("Importe a cobrar Pz") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = tpvConcept,
                onValueChange = { tpvConcept = it.take(60) },
                label = { Text("Concepto") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            if (tpvMode == "terminal") {
                nfcStatus.let { status ->
                    Text(status, color = if (nfcCard != null) IncomeGreen else PremiumPurple, fontWeight = FontWeight.Bold)
                }
                nfcCard?.let { card ->
                    Text("Cobrando ${formatPz(parseMoneyInput(tpvAmount))}Pz a ${card.iban}", fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val pz = parseMoneyInput(tpvAmount)
                            val detected = nfcCard
                            if (pz <= 0) { show("Introduce un importe válido"); return@Button }
                            if (detected == null) { show("Acerca primero una tarjeta PlaceZum NFC"); return@Button }
                            val payer = accounts[detected.accountId] ?: accounts.values.firstOrNull { it.iban == detected.iban }
                            if (payer == null) { show("Cuenta NFC no localizada"); return@Button }
                            if (payer.id == company.id) { show("No puedes cobrarte a la misma cuenta"); return@Button }
                            val submit = {
                                apply(engine.transferByIban(accounts, payer.id, company.iban, pz, TransactionKind.Placezum, tpvConcept.ifBlank { "TPV NFC ${company.displayName}" }))
                                show("Cobro TPV realizado: ${formatPz(pz)} Pz")
                                tpvMode = null; nfcCard = null; tpvAmount = ""; tpvConcept = ""
                            }
                            if (pz > config.contactlessLimitPz) authenticateTransfer(submit, show) else submit()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        shape = RoundedCornerShape(16.dp),
                        enabled = nfcCard != null
                    ) { Text("COBRAR ${formatPz(parseMoneyInput(tpvAmount))} Pz", fontWeight = FontWeight.Black) }
                    OutlinedButton(
                        onClick = { tpvMode = null; nfcCard = null; nfcStatus = "" },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Cancelar", color = ErrorRed) }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { tpvMode = "terminal" },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Contactless, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Activar Datafono", fontWeight = FontWeight.Black)
                    }
                }
                if (!hasNfc) {
                    Text("Este dispositivo no tiene NFC. Usa enlaces de pago para cobrar.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    qrLinkData?.let { (id, title, amount) ->
        if (showQrSlideUp) {
            val context = LocalContext.current
            BottomSlideUpOverlay(visible = showQrSlideUp, onDismiss = { showQrSlideUp = false }) {
                PaymentLinkQrSlideUp(
                    linkId = id,
                    linkTitle = title,
                    linkAmount = amount,
                    onClose = { showQrSlideUp = false },
                    onShare = { url ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir enlace de pago"))
                    }
                )
            }
        }
    }
}

private data class BusinessOrder(
    val id: String,
    val title: String,
    val amountPz: Long,
    val clientName: String,
    val status: String, // Pendiente, Aprobado, Pagado, Facturado
    val createdAt: Instant = Instant.now(),
    val linkUrl: String = "",
    val paymentLinkId: String = ""
)

@Composable
private fun BusinessOrdersPanel(
    company: Account,
    transactions: List<LedgerTransaction>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    accounts: Map<String, Account>,
    activeUser: UserProfile,
    authenticateTransfer: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit,
    context: Context
) {
    var orders by remember { mutableStateOf<List<BusinessOrder>>(emptyList()) }
    var showCreateOrder by remember { mutableStateOf(false) }
    var orderTitle by remember { mutableStateOf("") }
    var orderAmount by remember { mutableStateOf("") }
    var orderClient by remember { mutableStateOf("") }
    var showQrSlideUp by remember { mutableStateOf(false) }
    var qrData by remember { mutableStateOf<Triple<String, String, Long>?>(null) }

    fun statusColor(status: String): Color = when (status) {
        "Pendiente" -> Color(0xFFF59E0B)
        "Aprobado" -> PremiumPurple
        "Pagado" -> IncomeGreen
        "Facturado" -> Color(0xFF3B82F6)
        else -> Color(0xFF6C5878)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardBlock {
            Text("Gestión de Pedidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Crea un pedido → enlace al cliente → aprueba → genera pago → factura.", color = Color(0xFF6C5878))
        }

        if (showCreateOrder) {
            CardBlock {
                OutlinedTextField(orderTitle, { orderTitle = it.take(60) }, label = { Text("Producto/Servicio") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(orderAmount, { orderAmount = sanitizeMoneyInput(it) }, label = { Text("Importe Pz") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(orderClient, { orderClient = it.take(40) }, label = { Text("Nombre del cliente") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showCreateOrder = false; orderTitle = ""; orderAmount = ""; orderClient = "" }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancelar") }
                    Button(onClick = {
                        val pz = parseMoneyInput(orderAmount)
                        if (pz <= 0 || orderTitle.isBlank()) return@Button
                        val orderId = "PED-${company.id.take(4)}-${(1000..9999).random()}"
                        val linkUrl = "https://banco.laplaceta.org/pedido/$orderId"
                        val newOrder = BusinessOrder(orderId, orderTitle, pz, orderClient.ifBlank { "Cliente" }, "Pendiente", linkUrl = linkUrl)
                        orders = orders + newOrder
                        show("✅ Pedido creado: $orderId. Comparte el enlace con el cliente.")
                        showCreateOrder = false; orderTitle = ""; orderAmount = ""; orderClient = ""
                    }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) { Text("Crear pedido") }
                }
            }
        } else {
            Button(onClick = { showCreateOrder = true }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Nuevo pedido", fontWeight = FontWeight.Black)
            }
        }

        if (orders.isEmpty()) {
            CardBlock {
                EmptyState(Icons.Default.ShoppingCart, "Sin pedidos", "Crea tu primer pedido para empezar el flujo de facturación.")
            }
        } else {
            orders.forEach { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(order.title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                Text(order.id, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(color = statusColor(order.status).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                                Text(order.status, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = statusColor(order.status), fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${order.clientName} · ${formatPz(order.amountPz)} Pz", color = Color(0xFF6C5878))
                            Text(formatTimestamp(order.createdAt), color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }

                        when (order.status) {
                            "Pendiente" -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "📋 Pedido: ${order.title}\nImporte: ${formatPz(order.amountPz)} Pz\nCliente: ${order.clientName}\n\nAprueba aquí: ${order.linkUrl}") }
                                        context.startActivity(Intent.createChooser(intent, "Enviar pedido a cliente"))
                                    }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("Enviar", color = PremiumPurple)
                                    }
                                    Button(onClick = {
                                        orders = orders.map { if (it.id == order.id) it.copy(status = "Aprobado") else it }
                                        show("Pedido ${order.id} aprobado. Genera el enlace de pago.")
                                    }, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen), shape = RoundedCornerShape(14.dp)) {
                                        Text("Aprobar", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(onClick = {
                                        orders = orders.filter { it.id != order.id }
                                        show("Pedido ${order.id} cancelado")
                                    }, modifier = Modifier.weight(0.5f).height(44.dp), shape = RoundedCornerShape(14.dp)) {
                                        Text("X", color = ErrorRed)
                                    }
                                }
                            }
                            "Aprobado" -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val linkId = "LNK-${company.id.take(4)}-${(1000..9999).random()}"
                                        val paymentUrl = "https://banco.laplaceta.org/pagar/$linkId"
                                        qrData = Triple(linkId, order.title, order.amountPz)
                                        showQrSlideUp = true
                                        orders = orders.map { if (it.id == order.id) it.copy(status = "Pagado", paymentLinkId = linkId) else it }
                                        show("💰 Enlace de pago generado: $paymentUrl. Compártelo con el cliente.")
                                    }, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("Generar pago", fontWeight = FontWeight.Bold)
                                    }
                                    Button(onClick = {
                                        val linkId = "LNK-${company.id.take(4)}-${(1000..9999).random()}"
                                        val paymentUrl = "https://banco.laplaceta.org/pagar/$linkId"
                                        qrData = Triple(linkId, order.title, order.amountPz)
                                        showQrSlideUp = true
                                        orders = orders.map { if (it.id == order.id) it.copy(status = "Pagado", paymentLinkId = linkId) else it }
                                        show("💰 Enlace de pago generado. También puedes crear factura directa.")
                                    }, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("Crear factura", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "Pagado" -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val doc = DigitalDocument("factura-${order.id}", company.id, "Factura ${order.id} · ${order.title}", DocumentKind.PaymentReceipt)
                                        val result = PdfExporter.createAndOpenTaxPdf(context, company, doc, transactions, config)
                                        orders = orders.map { if (it.id == order.id) it.copy(status = "Facturado") else it }
                                        show(result)
                                    }, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("Generar factura PDF", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(onClick = {
                                        val shareText = "🧾 Factura ${order.id}\n${company.displayName}\nCliente: ${order.clientName}\nConcepto: ${order.title}\nImporte: ${formatPz(order.amountPz)} Pz\nEstado: PAGADA"
                                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                                        context.startActivity(Intent.createChooser(intent, "Compartir factura"))
                                    }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("Compartir", color = PremiumPurple)
                                    }
                                }
                            }
                            "Facturado" -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅ Factura generada y pagada", color = IncomeGreen, fontWeight = FontWeight.Bold)
                                    OutlinedButton(onClick = {
                                        val shareText = "🧾 Factura ${order.id} (PAGADA)\n${company.displayName}\nCliente: ${order.clientName}\nConcepto: ${order.title}\nImporte: ${formatPz(order.amountPz)} Pz"
                                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                                        context.startActivity(Intent.createChooser(intent, "Compartir factura"))
                                    }, shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("Compartir", color = PremiumPurple)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Crear factura directa (sin pedido)
        CardBlock {
            Text("Factura directa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Crea una factura directamente. El cliente paga vía web y se genera la factura pagada.", color = Color(0xFF6C5878))
            var directTitle by remember { mutableStateOf("") }
            var directAmount by remember { mutableStateOf("") }
            var directClient by remember { mutableStateOf("") }
            OutlinedTextField(directTitle, { directTitle = it.take(60) }, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(directAmount, { directAmount = sanitizeMoneyInput(it) }, label = { Text("Importe Pz") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(directClient, { directClient = it.take(40) }, label = { Text("Cliente") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    val pz = parseMoneyInput(directAmount)
                    if (pz <= 0 || directTitle.isBlank()) return@Button
                    val linkId = "LNK-${company.id.take(4)}-${(1000..9999).random()}"
                    val paymentUrl = "https://banco.laplaceta.org/pagar/$linkId"
                    qrData = Triple(linkId, directTitle, pz)
                    showQrSlideUp = true
                    val orderId = "FAC-${company.id.take(4)}-${(1000..9999).random()}"
                    orders = orders + BusinessOrder(orderId, directTitle, pz, directClient.ifBlank { "Cliente" }, "Pagado", linkUrl = paymentUrl, paymentLinkId = linkId)
                    show("💰 Factura creada. Enlace de pago: $paymentUrl. Cuando el cliente pague, genera el PDF.")
                    directTitle = ""; directAmount = ""; directClient = ""
                }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.size(6.dp)); Text("Crear factura + pago", fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }

        // QR SlideUp
        qrData?.let { (id, title, amount) ->
            if (showQrSlideUp) {
                BottomSlideUpOverlay(visible = showQrSlideUp, onDismiss = { showQrSlideUp = false }) {
                    PaymentLinkQrSlideUp(
                        linkId = id, linkTitle = title, linkAmount = amount,
                        onClose = { showQrSlideUp = false },
                        onShare = { url ->
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }
                            context.startActivity(Intent.createChooser(intent, "Compartir enlace de pago"))
                        }
                    )
                }
            }
        }
    }
}

private data class BusinessManager(
    val placetaId: String,
    val displayName: String,
    val ownershipPercent: Int
)

@Composable
private fun BusinessManagersPanel(
    company: Account,
    activeUser: UserProfile,
    users: List<UserProfile>,
    accounts: Map<String, Account>,
    savedContacts: List<SavedContact>
) {
    val context = LocalContext.current
    val initialManagers = users.filter { user ->
        accounts.values.any { account -> account.isOwnedBy(user) && (account.placetaId == company.placetaId || account.id == company.id) }
    }.let { list ->
        if (list.size == 1) list.map { BusinessManager(it.placetaId, it.displayName, 100) }
        else list.map { BusinessManager(it.placetaId, it.displayName, 0) }
    }
    var managerList by remember { mutableStateOf(initialManagers) }
    var showAddManager by remember { mutableStateOf(false) }
    var newManagerPlacetaId by remember { mutableStateOf("") }
    var newManagerPercent by remember { mutableStateOf("") }
    var newManagerLookupName by remember { mutableStateOf("") }
    var showDissolve by remember { mutableStateOf(false) }
    val totalPct = managerList.sumOf { it.ownershipPercent }
    val pctOk = totalPct == 100

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardBlock {
            Text("Gestores y Participación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Vincula gestores reales con su PlacetaID. Define el % de participación de cada gestor. Al disolver la sociedad el saldo se reparte según estos porcentajes.", color = Color(0xFF6C5878))
        }
        CardBlock {
            Text("Gestores actuales", fontWeight = FontWeight.Bold)
            if (managerList.isEmpty()) {
                Text("No hay gestores asignados además del titular.", color = Color(0xFF6C5878))
            } else {
                managerList.forEachIndexed { idx, manager ->
                    val isRealUser = users.any { it.placetaId.normalizedOwnerId() == manager.placetaId.normalizedOwnerId() }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isRealUser) Icons.Default.CheckCircle else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isRealUser) IncomeGreen else PremiumPurple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(manager.displayName, fontWeight = FontWeight.Bold)
                            Text(manager.placetaId, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                            if (!isRealUser) Text("⚠ No vinculado a persona real", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(color = PremiumPurple.copy(alpha = 0.10f), shape = RoundedCornerShape(8.dp)) {
                            Text("${manager.ownershipPercent}%", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = PremiumPurple, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { managerList = managerList.toMutableList().also { it.removeAt(idx) } }) {
                            Text("X", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (!pctOk) {
                    Text("Total: $totalPct% ${if (totalPct < 100) "(falta ${100 - totalPct}%)" else "(sobra ${totalPct - 100}%)"}",
                        color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showAddManager) {
            CardBlock {
                OutlinedTextField(newManagerPlacetaId, {
                    newManagerPlacetaId = it.take(40)
                    // Autocompletar nombre si el PlacetaID coincide con un usuario real
                    val matched = users.firstOrNull { u -> u.placetaId.normalizedOwnerId() == it.normalizedOwnerId() }
                    newManagerLookupName = matched?.displayName ?: ""
                }, label = { Text("PlacetaID del gestor") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                if (newManagerLookupName.isNotBlank()) {
                    Row(Modifier.fillMaxWidth().background(IncomeGreen.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Persona real: $newManagerLookupName", color = IncomeGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (newManagerPlacetaId.isNotBlank()) {
                    Row(Modifier.fillMaxWidth().background(ErrorRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("No se encontró persona con ese PlacetaID", color = ErrorRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(newManagerPercent, { newManagerPercent = it.filter(Char::isDigit).take(3) }, label = { Text("% participación (0-100)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showAddManager = false; newManagerPlacetaId = ""; newManagerPercent = ""; newManagerLookupName = "" }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancelar") }
                    Button(onClick = {
                        val pct = newManagerPercent.toIntOrNull()?.coerceIn(0, 100) ?: 0
                        if (newManagerPlacetaId.isBlank()) return@Button
                        val displayName = newManagerLookupName.ifBlank { newManagerPlacetaId }
                        managerList = managerList + BusinessManager(newManagerPlacetaId, displayName, pct)
                        showAddManager = false; newManagerPlacetaId = ""; newManagerPercent = ""; newManagerLookupName = ""
                    }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) { Text("Añadir") }
                }
            }
        } else {
            Button(onClick = { showAddManager = true }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Añadir gestor con %", fontWeight = FontWeight.Black)
            }
        }
        CardBlock {
            Text("Disolver sociedad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Para disolver deben cumplirse todos los requisitos. El saldo se reparte según los % de participación.", color = Color(0xFF6C5878))
            val allManagersLinked = managerList.all { m -> users.any { it.placetaId.normalizedOwnerId() == m.placetaId.normalizedOwnerId() } }
            if (showDissolve) {
                val hasInv = false
                val isFund = company.listedInvestmentFund
                val hasDebts = false
                val taxesOk = true
                val canDissolve = !hasInv && !isFund && !hasDebts && taxesOk && pctOk && managerList.isNotEmpty() && allManagersLinked
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DissolveReq("Sin inversiones activas", !hasInv)
                    DissolveReq("No es cuenta fondo", !isFund)
                    DissolveReq("Sin deudas pendientes", !hasDebts)
                    DissolveReq("Impuestos al día", taxesOk)
                    DissolveReq("% participación suma 100%", pctOk)
                    DissolveReq("Todos los gestores son personas reales", allManagersLinked)
                }
                if (canDissolve) {
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.08f)), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Reparto del saldo (${formatPz(company.balancePz)} Pz)", fontWeight = FontWeight.Bold)
                            managerList.forEach { manager ->
                                val toUser = users.firstOrNull { it.placetaId.normalizedOwnerId() == manager.placetaId.normalizedOwnerId() }
                                val targetIban = toUser?.primaryAccountId?.let { accounts[it]?.iban } ?: "(no localizado)"
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(manager.displayName, color = Color(0xFF6C5878))
                                        Text(targetIban, style = MaterialTheme.typography.labelSmall, color = PremiumPurple)
                                    }
                                    Text("${formatPz(company.balancePz * manager.ownershipPercent / 100)} Pz (${manager.ownershipPercent}%)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val txt = managerList.joinToString("\n") { manager ->
                            val toUser = users.firstOrNull { it.placetaId.normalizedOwnerId() == manager.placetaId.normalizedOwnerId() }
                            val targetIban = toUser?.primaryAccountId?.let { accounts[it]?.iban } ?: manager.placetaId
                            "${manager.displayName} ($targetIban): ${formatPz(company.balancePz * manager.ownershipPercent / 100)} Pz (${manager.ownershipPercent}%)"
                        }
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Liquidación de ${company.displayName}\nSaldo: ${formatPz(company.balancePz)} Pz\n\n$txt") }, "Compartir liquidación"))
                        showDissolve = false
                    }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.Gavel, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("INICIAR DISOLUCIÓN", fontWeight = FontWeight.Black)
                    }
                }
            } else {
                Button(onClick = { showDissolve = true }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.12f)), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = ErrorRed); Spacer(Modifier.size(8.dp)); Text("Ver requisitos de disolución", color = ErrorRed, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DissolveReq(label: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (met) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (met) IncomeGreen else ErrorRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(8.dp)); Text(label, color = if (met) IncomeGreen else ErrorRed, fontWeight = if (met) FontWeight.Normal else FontWeight.Bold)
    }
}

@Composable
private fun PaymentLinkQrSlideUp(
    linkId: String,
    linkTitle: String,
    linkAmount: Long,
    onClose: () -> Unit,
    onShare: (String) -> Unit
) {
    val paymentUrl = "https://banco.laplaceta.org/pagar/$linkId"
    val qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${Uri.encode(paymentUrl)}"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Enlace de pago", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(linkId, color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.White, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                        }
                    },
                    update = { view ->
                        val html = """
                            <html><body style="margin:0;display:flex;align-items:center;justify-content:center;background:transparent;height:100%">
                            <img src="$qrApiUrl" style="max-width:100%;max-height:100%;object-fit:contain"/>
                            </body></html>
                        """.trimIndent()
                        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EEFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(linkTitle, fontWeight = FontWeight.Bold)
                        Text(paymentUrl, color = PremiumPurple, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    Text(formatPz(linkAmount), fontWeight = FontWeight.Black, color = PremiumPurple)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onShare(paymentUrl) },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Compartir", color = PremiumPurple, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("CERRAR", fontWeight = FontWeight.Black)
                }
            }
            Text("Escanea el QR o comparte el enlace para recibir el pago.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ContractRequiredPopup(
    show: Boolean,
    productName: String,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onViewContract: () -> Unit
) {
    if (!show) return
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f).clickable(enabled = false) { },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Default.Description, contentDescription = null, tint = PremiumPurple, modifier = Modifier.size(48.dp))
                Text("Contrato requerido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("$productName necesita que aceptes el contrato de servicio para poder usarlo.", color = Color(0xFF6C5878))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onViewContract,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Ver contrato") }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Aceptar", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun MoreServicesSlideUp(
    account: Account,
    documents: List<DigitalDocument>,
    paymentTransactions: List<LedgerTransaction>,
    accounts: List<Account>,
    allAccounts: Map<String, Account>,
    users: List<UserProfile> = emptyList(),
    investmentTransactions: List<LedgerTransaction>,
    digitalCards: List<DigitalCard>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    payrollTargets: List<Account>,
    savedContacts: List<SavedContact> = emptyList(),
    payrollContracts: List<PayrollContract> = emptyList(),
    payrollPeriods: List<PayrollPeriod> = emptyList(),
    donationAmount: String,
    activeUser: UserProfile? = null,
    donationRewards: List<DonationReward> = emptyList(),
    onDonationAmount: (String) -> Unit,
    onClose: () -> Unit,
    onNewProduct: () -> Unit,
    onSupportTicket: (String) -> Unit,
    onRegisterPayroll: (Account, Long) -> Unit,
    onPayrollContract: (PayrollContract) -> Unit = {},
    onPayrollPeriod: (PayrollPeriod) -> Unit = {},
    onPayPayrollPeriod: (PayrollPeriod) -> Unit = {},
    onPayrollContractPdf: (PayrollContract) -> Unit = {},
    onDonate: () -> Unit,
    onDonationReward: (DonationReward) -> Unit = {},
    onDonatePointsToFoundation: (Long) -> Unit = {},
    moduleHidden: Set<String> = OptionalModuleIds,
    hasBusinessAccess: Boolean = false,
    hasStateAccess: Boolean = false,
    onModuleEnabledChange: (String, Boolean) -> Unit = { _, _ -> },
    accountHolders: List<AccountHolder> = emptyList(),
    onAddAccountHolder: (AccountHolder) -> Unit = {},
    onRemoveAccountHolder: (String) -> Unit = {},
    onDocument: (DigitalDocument) -> Unit,
    onTransactionReceipt: (LedgerTransaction) -> Unit,
    onCreatePaymentLink: ((String, String, Long, String, (String?, String?) -> Unit) -> Unit)? = null
) {
    var showDocs by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showPaymentLinks by remember { mutableStateOf(false) }
    var showPayroll by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCustomStatement by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf("") }
    var customEndDate by remember { mutableStateOf("") }
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Más servicios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("${account.displayName} · ${account.iban}", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction(Icons.Default.SupportAgent, "Soporte", Modifier.weight(1f)) { showSupport = !showSupport }
                QuickAction(Icons.Default.Add, "Alta", Modifier.weight(1f), onNewProduct)
            }
            QuickAction(Icons.Default.Settings, "Configuración", Modifier.fillMaxWidth()) { showSettings = !showSettings }
            AnimatedVisibility(showSettings,
                enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleSettingsPanel(
                        hiddenModules = moduleHidden,
                        hasBusinessAccess = hasBusinessAccess,
                        hasStateAccess = hasStateAccess,
                        onModuleEnabledChange = onModuleEnabledChange
                    )
                    NotificationSettingsPanel(context = context)
                }
            }

            AnimatedVisibility(showSupport,
                enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(150))
            ) {
                SupportTicketPanel(
                    account = account,
                    transactions = paymentTransactions,
                    accounts = accounts,
                    investmentTransactions = investmentTransactions,
                    digitalCards = digitalCards,
                    onSend = onSupportTicket
                )
            }
            if (account.type in setOf(AccountType.Business, AccountType.State)) {
                QuickAction(Icons.Default.Payments, "Registrar nómina", Modifier.fillMaxWidth()) { showPayroll = !showPayroll }
                AnimatedVisibility(showPayroll,
                    enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(150))
                ) {
                    PayrollRegistrationPanel(
                        company = account,
                        targets = payrollTargets,
                        allAccounts = allAccounts,
                        users = users,
                        savedContacts = savedContacts,
                        activeUser = activeUser,
                        contracts = payrollContracts.filter { it.companyAccountId == account.id },
                        periods = payrollPeriods.filter { it.companyAccountId == account.id },
                        config = config,
                        engine = engine,
                        onRegister = onRegisterPayroll,
                        onContract = onPayrollContract,
                        onPeriod = onPayrollPeriod,
                        onPayPeriod = onPayPayrollPeriod,
                        onContractPdf = onPayrollContractPdf
                    )
                }
            }
            QuickAction(Icons.Default.Description, "Documentación", Modifier.fillMaxWidth()) { showDocs = !showDocs }
            AnimatedVisibility(showDocs,
                enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Extracto personalizado por rango de fechas
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showCustomStatement = !showCustomStatement },
                        colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.10f), contentColor = Ink),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Gold)
                            Column(Modifier.weight(1f)) {
                                Text("Extracto personalizado", fontWeight = FontWeight.Bold)
                                Text("Elige el rango de fechas que quieras", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(if (showCustomStatement) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Gold)
                        }
                    }
                    if (showCustomStatement) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Rango de fechas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(
                                    value = customStartDate,
                                    onValueChange = { customStartDate = it.take(10) },
                                    label = { Text("Desde (YYYY-MM-DD)") },
                                    placeholder = { Text(LocalDate.now().minusMonths(1).toString()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = customEndDate,
                                    onValueChange = { customEndDate = it.take(10) },
                                    label = { Text("Hasta (YYYY-MM-DD)") },
                                    placeholder = { Text(LocalDate.now().toString()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val start = runCatching { LocalDate.parse(customStartDate.ifBlank { LocalDate.now().minusMonths(1).toString() }) }.getOrNull()
                                        val end = runCatching { LocalDate.parse(customEndDate.ifBlank { LocalDate.now().toString() }) }.getOrNull()
                                        if (start == null || end == null) return@Button
                                        val filteredTx = transactionsForAccount(paymentTransactions, account.id)
                                            .filter { tx ->
                                                val txDate = tx.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
                                                !txDate.isBefore(start) && !txDate.isAfter(end)
                                            }
                                        val title = "Extracto ${account.displayName} · $start → $end"
                                        val doc = DigitalDocument(
                                            "custom-statement-${account.id}-${start}-${end}",
                                            account.id, title, DocumentKind.MonthlyStatement
                                        )
                                        val msg = PdfExporter.createAndOpenTaxPdf(context, account, doc, filteredTx, config)
                                        onDocument(doc)
                                        showCustomStatement = false
                                        customStartDate = ""
                                        customEndDate = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = customStartDate.isNotBlank() || customEndDate.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text("GENERAR EXTRACTO", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    documents.forEach { doc ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(PremiumPurple.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .clickable { onDocument(doc) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PremiumPurple)
                            Column(Modifier.weight(1f)) {
                                Text(doc.title, fontWeight = FontWeight.Bold)
                                Text(doc.kind.name, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.Download, contentDescription = null, tint = PremiumPurple)
                        }
                    }
                }
            }
            var showDonateConfirm by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.10f), contentColor = Ink),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = PremiumPurple)
                        Column {
                            Text("Fundación Banco de La Placeta", fontWeight = FontWeight.Black)
                            Text("Fondo encargado de sostener los RBU.", color = Color(0xFF6C5878))
                        }
                    }
                    OutlinedTextField(
                        value = donationAmount,
                        onValueChange = onDonationAmount,
                        label = { Text("Donación Pz") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Button(
                        onClick = { showDonateConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("DONAR A LA FUNDACIÓN", fontWeight = FontWeight.Black)
                    }
                }
            }
            if (showDonateConfirm) {
                AlertDialog(
                    onDismissRequest = { showDonateConfirm = false },
                    title = { Text("Confirmar donación", fontWeight = FontWeight.Bold) },
                    text = { Text("¿Enviar ${donationAmount.ifBlank { "0" }} Pz a la Fundación Banco de La Placeta? Los fondos se destinan a sostener los RBU.") },
                    confirmButton = {
                        Button(onClick = { showDonateConfirm = false; onDonate() }, colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(14.dp)) {
                            Text("Sí, donar", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDonateConfirm = false }, shape = RoundedCornerShape(14.dp)) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CoOwnerManagementPanel(
    account: Account,
    holders: List<AccountHolder>,
    users: List<UserProfile>,
    onAddHolder: (AccountHolder) -> Unit,
    onRemoveHolder: (String) -> Unit,
    show: (String) -> Unit
) {
    val currentHolders = holders.filter { it.accountId == account.id }
    var newPlacetaId by remember { mutableStateOf("") }
    var newPercent by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.08f), contentColor = Ink),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cotitulares", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("Añade cotitulares a ${account.displayName}. Tendrán acceso a la cuenta.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PremiumPurple)
            }
            if (currentHolders.isEmpty()) {
                Text("Sin cotitulares. Solo el titular tiene acceso.", color = Color(0xFF6C5878))
            } else {
                currentHolders.forEach { holder ->
                    val userName = users.firstOrNull { it.placetaId.normalizedOwnerId() == holder.placetaId.normalizedOwnerId() }?.displayName ?: holder.placetaId
                    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(userName, fontWeight = FontWeight.Bold)
                            Text(holder.role, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }
                        if (holder.ownershipPercent > 0) {
                            Text("${holder.ownershipPercent}%", color = PremiumPurple, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { onRemoveHolder(holder.id); show("Cotitular eliminado") }) {
                            Text("Quitar", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (showAdd) {
                OutlinedTextField(newPlacetaId, { newPlacetaId = it.take(30) }, label = { Text("PlacetaID del cotitular") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(newPercent, { newPercent = it.filter(Char::isDigit).take(3) }, label = { Text("% participación (0-100)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showAdd = false; newPlacetaId = ""; newPercent = "" }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(14.dp)) { Text("Cancelar") }
                    Button(onClick = {
                        if (newPlacetaId.isBlank()) return@Button
                        val pct = newPercent.toIntOrNull()?.coerceIn(0, 100) ?: 0
                        val holder = AccountHolder(
                            id = "holder-${account.id}-${newPlacetaId}",
                            accountId = account.id,
                            placetaId = newPlacetaId,
                            role = "CoOwner",
                            ownershipPercent = pct,
                            linkedAt = Instant.now().toString()
                        )
                        onAddHolder(holder)
                        show("Cotitular añadido: $newPlacetaId")
                        showAdd = false; newPlacetaId = ""; newPercent = ""
                    }, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(14.dp)) { Text("Añadir") }
                }
            } else {
                Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Añadir cotitular", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ModuleSettingsPanel(
    hiddenModules: Set<String>,
    hasBusinessAccess: Boolean,
    hasStateAccess: Boolean,
    onModuleEnabledChange: (String, Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.08f), contentColor = Ink),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Configuración de módulos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Activa aquí las pestañas inferiores y herramientas extra. Si no tienes acceso a una cuenta compatible, el módulo aparecerá bloqueado abajo.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            ModuleSwitchRow(
                title = "Sociedades",
                detail = if (hasBusinessAccess) "Facturas, nóminas, empleados, expedientes y documentos de empresa." else "Selecciona una cuenta de empresa para desbloquear la pestaña.",
                checked = MODULE_SOCIETIES !in hiddenModules,
                enabled = true,
                onCheckedChange = { onModuleEnabledChange(MODULE_SOCIETIES, it) }
            )
            ModuleSwitchRow(
                title = "Herramientas estatales",
                detail = if (hasStateAccess) "Sanciones, cuentas estatales y operaciones administrativas autorizadas." else "Solo aparece si tienes titularidad o cotitularidad en una cuenta estatal.",
                checked = MODULE_STATE_TOOLS !in hiddenModules,
                enabled = true,
                onCheckedChange = { onModuleEnabledChange(MODULE_STATE_TOOLS, it) }
            )
        }
    }
}

@Composable
private fun ModuleSwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NotificationSettingsPanel(context: Context) {
    var enabled by remember { mutableStateOf(AppNotificationState.notificationsEnabled(context)) }
    var money by remember { mutableStateOf(AppNotificationState.channelEnabled(context, PlacetaNotifications.CHANNEL_MONEY)) }
    var investments by remember { mutableStateOf(AppNotificationState.channelEnabled(context, PlacetaNotifications.CHANNEL_MONEY, "Market")) }
    var security by remember { mutableStateOf(AppNotificationState.channelEnabled(context, PlacetaNotifications.CHANNEL_SECURITY)) }
    var fiscal by remember { mutableStateOf(AppNotificationState.channelEnabled(context, PlacetaNotifications.CHANNEL_FISCAL)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.08f), contentColor = Ink),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ajustes de notificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Controla qué avisos salen del sistema Android.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            NotificationSwitchRow("Sistema Android", "Activa o pausa todas las notificaciones", enabled) {
                enabled = it
                AppNotificationState.setNotificationsEnabled(context, it)
            }
            NotificationSwitchRow("Movimientos", "Ingresos, Placezum y actividad de saldo", money, enabled) {
                money = it
                AppNotificationState.setChannelEnabled(context, PlacetaNotifications.CHANNEL_MONEY, it)
            }
            NotificationSwitchRow("Inversiones", "Resultados y operaciones listas", investments, enabled) {
                investments = it
                AppNotificationState.setChannelEnabled(context, PlacetaNotifications.CHANNEL_MONEY, it, "Market")
            }
            NotificationSwitchRow("Seguridad", "Inicio de sesión y acciones sensibles", security, enabled) {
                security = it
                AppNotificationState.setChannelEnabled(context, PlacetaNotifications.CHANNEL_SECURITY, it)
            }
            NotificationSwitchRow("Fiscal", "IRM, alertas y recibos tributarios", fiscal, enabled) {
                fiscal = it
                AppNotificationState.setChannelEnabled(context, PlacetaNotifications.CHANNEL_FISCAL, it)
            }
        }
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SupportTicketPanel(
    account: Account,
    transactions: List<LedgerTransaction>,
    accounts: List<Account>,
    investmentTransactions: List<LedgerTransaction>,
    digitalCards: List<DigitalCard>,
    onSend: (String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Cuenta") }
    var priority by remember { mutableStateOf("Media") }
    var activeTicketId by remember { mutableStateOf<String?>(null) }
    var showAttachments by remember { mutableStateOf(false) }
    val selectedAttachments = remember { mutableStateListOf<String>() }
    val chatMessages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    fun toggleAttachment(id: String) {
        if (id in selectedAttachments) selectedAttachments.remove(id) else selectedAttachments.add(id)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EEFF), contentColor = Ink),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (activeTicketId == null) "Nuevo ticket de soporte" else "Ticket $activeTicketId", fontWeight = FontWeight.Black)
                    Text("$category · prioridad $priority · chat con adjuntos", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PremiumPurple)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Cuenta", "Pago", "Inversión", "Nómina", "Fiscal", "Seguridad")) { item ->
                    FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Baja", "Media", "Alta", "Urgente")) { item ->
                    FilterChip(selected = priority == item, onClick = { priority = item }, label = { Text(item) })
                }
            }
            if (activeTicketId == null) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it.take(60) },
                    label = { Text("Asunto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (chatMessages.isEmpty()) {
                SupportBubble(fromUser = false, text = "Cuéntanos qué ocurre. Puedes adjuntar transacciones, cuentas, inversiones o tarjetas al ticket.")
            } else {
                chatMessages.takeLast(6).forEach { (fromUser, text) ->
                    SupportBubble(fromUser = fromUser, text = text)
                }
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(260) },
                label = { Text(if (activeTicketId == null) "Primer mensaje" else "Responder") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            FilterChip(
                selected = showAttachments,
                onClick = { showAttachments = !showAttachments },
                label = { Text("Adjuntos (${selectedAttachments.size})") }
            )
            AnimatedVisibility(showAttachments) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttachmentChips(
                        title = "Cuentas",
                        items = accounts.take(6).map { it.id to "${it.displayName} · ${it.type.label()}" },
                        selected = selectedAttachments,
                        onToggle = ::toggleAttachment
                    )
                    AttachmentChips(
                        title = "Transacciones",
                        items = transactions.take(6).map { it.id to "${it.publicDescription()} · ${formatPz(it.amountPz)} Pz" },
                        selected = selectedAttachments,
                        onToggle = ::toggleAttachment
                    )
                    AttachmentChips(
                        title = "Inversiones",
                        items = investmentTransactions.take(6).map { it.id to "${it.publicDescription()} · ${formatPz(it.amountPz)} Pz" },
                        selected = selectedAttachments,
                        onToggle = ::toggleAttachment
                    )
                    AttachmentChips(
                        title = "Tarjetas",
                        items = digitalCards.take(6).map { it.id to it.alias },
                        selected = selectedAttachments,
                        onToggle = ::toggleAttachment
                    )
                }
            }
            Button(
                onClick = {
                    val ticketId = activeTicketId ?: "SUP-${System.currentTimeMillis().toString().takeLast(6)}"
                    activeTicketId = ticketId
                    val cleanMessage = message.ifBlank { subject.ifBlank { "Consulta de soporte" } }
                    chatMessages += true to cleanMessage
                    chatMessages += false to "Recibido en $ticketId. Categoría $category, prioridad $priority. Revisaremos ${selectedAttachments.size} adjuntos y te responderemos en este hilo."
                    onSend("$ticketId · $category · $priority · ${subject.ifBlank { "Sin asunto" }} · ${selectedAttachments.size} adjuntos")
                    subject = ""
                    message = ""
                },
                enabled = subject.isNotBlank() || message.isNotBlank() || selectedAttachments.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (activeTicketId == null) "Abrir ticket" else "Enviar respuesta", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SupportBubble(fromUser: Boolean, text: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.82f)
                .background(if (fromUser) PremiumPurple else Color.White, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(text, color = if (fromUser) Color.White else Ink)
        }
    }
}

@Composable
private fun PaymentLinksPanel(
    account: Account,
    onCreatePaymentLink: ((String, String, Long, String, (String?, String?) -> Unit) -> Unit)? = null
) {
    var linkAmount by remember { mutableStateOf("100") }
    var paymentMode by remember { mutableStateOf(true) }
    var lastLink by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.dp, PremiumPurple, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Enlaces de pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Crea enlaces para cobrar o recibir Placetas. En cuentas empresa, cobrar calcula IVA; las nóminas siguen en su panel específico.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = linkAmount,
                onValueChange = { linkAmount = sanitizeMoneyInput(it, maxLength = 9) },
                label = { Text("Importe Pz") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = paymentMode, onClick = { paymentMode = true }, label = { Text("Cobrar") }) }
                item { FilterChip(selected = !paymentMode, onClick = { paymentMode = false }, label = { Text("Recibir") }) }
            }
            val amount = parseMoneyInput(linkAmount).takeIf { it > 0 } ?: 1_00
            val iva = if (paymentMode && account.type == AccountType.Business) payrollTax(amount, 12) else 0L
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultMetric("Base", "${formatPz(amount)} Pz", Modifier.weight(1f), PremiumPurple)
                ResultMetric("IVA", "${formatPz(iva)} Pz", Modifier.weight(1f), if (iva > 0) Gold else Color.Black)
            }
            Button(
                onClick = {
                    if (onCreatePaymentLink != null) {
                        isGenerating = true
                        errorMsg = ""
                        val kind = if (paymentMode) "Payment" else "Send"
                        val conceptStr = if (paymentMode) "Enlace de pago desde app" else "Enlace para recibir Placetas"
                        onCreatePaymentLink(kind, account.id, amount, conceptStr) { url, err ->
                            isGenerating = false
                            if (err != null) {
                                errorMsg = err
                            } else if (url != null) {
                                lastLink = url
                                shareText(context, url)
                            }
                        }
                    } else {
                        lastLink = buildAppPaymentLink(account, amount, paymentMode)
                        shareText(context, lastLink)
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(19.dp))
                } else {
                    Icon(Icons.Default.Payments, contentDescription = null)
                }
                Spacer(Modifier.size(8.dp))
                Text(if (isGenerating) "Generando..." else "Generar y compartir", fontWeight = FontWeight.Black)
            }
            if (errorMsg.isNotBlank()) Text(errorMsg, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            if (lastLink.isNotBlank()) Text(lastLink, color = PremiumPurple, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun buildAppPaymentLink(account: Account, amountPz: Long, payment: Boolean): String {
    val safeAmount = amountPz.coerceAtLeast(1_00L)
    val kind = if (payment) "payment" else "send"
    val iva = if (payment && account.type == AccountType.Business) (safeAmount * 12 + 99) / 100 else 0L
    val total = safeAmount + iva
    val id = "mobile-${System.currentTimeMillis()}"
    val concept = java.net.URLEncoder.encode(if (payment) "Enlace de pago desde app" else "Enlace para recibir Placetas", "UTF-8")
    fun webAmount(value: Long) = BigDecimal(value).movePointLeft(2).stripTrailingZeros().toPlainString()
    return "https://banco.laplaceta.org/pay-link/$id?kind=$kind&account=${account.id}&iban=${account.iban}&amount=${webAmount(safeAmount)}&iva=${webAmount(iva)}&total=${webAmount(total)}&concept=$concept"
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir enlace Banco de La Placeta"))
}

@Composable
private fun AttachmentChips(
    title: String,
    items: List<Pair<String, String>>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    if (items.isEmpty()) return
    Text(title, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.first }) { item ->
            FilterChip(
                selected = item.first in selected,
                onClick = { onToggle(item.first) },
                label = { Text(item.second.take(26)) }
            )
        }
    }
}

@Composable
private fun PayrollRegistrationPanel(
    company: Account,
    targets: List<Account>,
    allAccounts: Map<String, Account>,
    users: List<UserProfile>,
    savedContacts: List<SavedContact>,
    activeUser: UserProfile?,
    contracts: List<PayrollContract>,
    periods: List<PayrollPeriod>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    onRegister: (Account, Long) -> Unit,
    onContract: (PayrollContract) -> Unit,
    onPeriod: (PayrollPeriod) -> Unit,
    onPayPeriod: (PayrollPeriod) -> Unit,
    onContractPdf: (PayrollContract) -> Unit
) {
    var employeeDipInput by remember { mutableStateOf("") }
    var selectedContactAccountId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("200") }
    var roleTitle by remember { mutableStateOf("Trabajador") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var frequency by remember { mutableStateOf(PayrollFrequency.Weekly) }
    var selectedContractId by remember(contracts.map { it.id }) { mutableStateOf(contracts.firstOrNull()?.id.orEmpty()) }
    var periodLabel by remember { mutableStateOf("Periodo ${LocalDate.now()}") }
    var periodStart by remember { mutableStateOf(LocalDate.now().minusDays(6).toString()) }
    var periodEnd by remember { mutableStateOf(LocalDate.now().toString()) }
    var salaryReason by remember { mutableStateOf("Actualización salarial") }
    val contactTargets = savedContacts
        .filter { it.ownerPlacetaId == activeUser?.placetaId }
        .mapNotNull { contact -> allAccounts[contact.accountId]?.takeIf { it.type == AccountType.Current && it.id != company.id } }
        .distinctBy { it.id }
    val selectedContactTarget = contactTargets.firstOrNull { it.id == selectedContactAccountId }
    val normalizedDip = employeeDipInput.trim().uppercase()
    val manualUser = users.firstOrNull { it.dip.uppercase() == normalizedDip }
    val manualTarget = manualUser?.let { user ->
        allAccounts[user.primaryAccountId]?.takeIf { it.type == AccountType.Current }
            ?: allAccounts.values.firstOrNull { it.type == AccountType.Current && it.placetaId == user.placetaId }
    }
    val target = selectedContactTarget ?: manualTarget
    val targetDip = selectedContactTarget?.employeeDip(users) ?: manualUser?.dip ?: normalizedDip
    val dipLooksValid = Regex("^\\d{8}[A-Z]$").matches(targetDip)
    val grossSalary = parseMoneyInput(amount)
    val workerTax = payrollTax(grossSalary, config.payrollWorkerTaxPercent)
    val employerTax = payrollTax(grossSalary, config.payrollEmployerTaxPercent)
    val netSalary = (grossSalary - workerTax).coerceAtLeast(0L)
    val employerCost = grossSalary + employerTax
    val selectedContract = contracts.firstOrNull { it.id == selectedContractId }
    val nextPaymentDate = engine.nextPayrollPaymentDate(frequency)
    val pendingPeriods = periods.filter { it.status == PayrollPeriodStatus.Pending }.sortedBy { it.periodEnd }
    val paidPeriods = periods.filter { it.status == PayrollPeriodStatus.Paid }.sortedByDescending { it.paidAt ?: it.createdAt }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EEFF), contentColor = Ink),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Administración de nóminas", fontWeight = FontWeight.Black)
            Text("Contratos por DIP, periodos pendientes y recibos PDF trazables.", color = Color(0xFF6C5878))
            if (targets.isEmpty()) {
                Text("No hay cuentas personales disponibles como empleado.", color = Color(0xFF6C5878))
            } else {
                Text("Contrato trabajador", fontWeight = FontWeight.Bold)
                if (contactTargets.isNotEmpty()) {
                    Text("Elegir contacto guardado", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contactTargets, key = { it.id }) { item ->
                            val dip = item.employeeDip(users)
                            FilterChip(
                                selected = selectedContactAccountId == item.id,
                                onClick = {
                                    selectedContactAccountId = item.id
                                    employeeDipInput = dip
                                },
                                label = { Text("${item.displayName.take(16)} · $dip") }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = employeeDipInput,
                    onValueChange = {
                        selectedContactAccountId = ""
                        employeeDipInput = it.take(9).uppercase()
                    },
                    label = { Text("DIP trabajador obligatorio") },
                    placeholder = { Text("12345678A") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f), contentColor = Ink),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(target?.displayName ?: "Trabajador pendiente de verificar", fontWeight = FontWeight.Bold)
                        Text(if (target != null) "$targetDip · ${target.iban}" else "Introduce un DIP registrado o elige un contacto guardado.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it.take(44) },
                    label = { Text("Puesto / función") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it.take(10) },
                        label = { Text("Inicio AAAA-MM-DD") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = sanitizeMoneyInput(it, maxLength = 9) },
                        label = { Text("Bruto Pz") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PayrollFrequency.entries, key = { it.name }) { item ->
                        FilterChip(
                            selected = frequency == item,
                            onClick = { frequency = item },
                            label = { Text(item.label()) }
                        )
                    }
                }
                Text(
                    "Próximo pago automático: ${formatPayrollPaymentDate(nextPaymentDate)}",
                    color = PremiumPurple,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        val employee = target ?: return@Button
                        val cleanStart = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: LocalDate.now()
                        val existing = contracts.firstOrNull { it.employeeAccountId == employee.id && it.status != PayrollContractStatus.Ended }
                        val contract = PayrollContract(
                            id = existing?.id ?: "contract-${UUID.randomUUID()}",
                            companyAccountId = company.id,
                            employeeAccountId = employee.id,
                            employeeDip = targetDip,
                            employeeName = employee.displayName,
                            roleTitle = roleTitle.ifBlank { "Trabajador" },
                            grossSalaryPz = grossSalary,
                            frequency = frequency,
                            status = PayrollContractStatus.Active,
                            startDate = cleanStart,
                            salaryHistory = existing?.salaryHistory.orEmpty(),
                            createdAt = existing?.createdAt ?: Instant.now()
                        )
                        onContract(contract)
                        selectedContractId = contract.id
                    },
                    enabled = target != null && dipLooksValid && grossSalary >= config.minimumWeeklySalaryPz,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AssuredWorkload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Guardar contrato por DIP", fontWeight = FontWeight.Black)
                }

                if (contracts.isNotEmpty()) {
                    Text("Plantilla de ${company.displayName}", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contracts, key = { it.id }) { contract ->
                            val statusColor = contract.status.statusColor()
                            FilterChip(
                                selected = selectedContractId == contract.id,
                                onClick = {
                                    selectedContractId = contract.id
                                    amount = formatPz(contract.grossSalaryPz)
                                    roleTitle = contract.roleTitle
                                    frequency = contract.frequency
                                    employeeDipInput = contract.employeeDip
                                    selectedContactAccountId = ""
                                },
                                label = { Text("${contract.employeeDip} · ${contract.status.name}", color = statusColor) }
                            )
                        }
                    }
                    selectedContract?.let { contract ->
                        ContractSummary(contract, periods.filter { it.contractId == contract.id })
                        OutlinedButton(
                            onClick = { onContractPdf(contract) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Descargar PDF de alta")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = salaryReason,
                                onValueChange = { salaryReason = it.take(60) },
                                label = { Text("Motivo cambio sueldo") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Button(
                                onClick = {
                                    if (grossSalary > 0 && grossSalary != contract.grossSalaryPz) {
                                        onContract(
                                            contract.copy(
                                                grossSalaryPz = grossSalary,
                                                salaryHistory = contract.salaryHistory + PayrollSalaryChange(
                                                    previousGrossSalaryPz = contract.grossSalaryPz,
                                                    newGrossSalaryPz = grossSalary,
                                                    reason = salaryReason.ifBlank { "Cambio salarial" }
                                                )
                                            )
                                        )
                                    }
                                },
                                enabled = grossSalary >= config.minimumWeeklySalaryPz && grossSalary != contract.grossSalaryPz,
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("Cambiar sueldo") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(PayrollContractStatus.Active, PayrollContractStatus.Paused, PayrollContractStatus.Ended).forEach { status ->
                                FilterChip(
                                    selected = contract.status == status,
                                    onClick = { onContract(contract.copy(status = status, endDate = if (status == PayrollContractStatus.Ended) LocalDate.now() else null)) },
                                    label = { Text(status.name) }
                                )
                            }
                        }
                    }
                }

                Text("Periodos de pago", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = periodLabel,
                    onValueChange = { periodLabel = it.take(42) },
                    label = { Text("Etiqueta del periodo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = periodStart,
                        onValueChange = { periodStart = it.take(10) },
                        label = { Text("Desde") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = periodEnd,
                        onValueChange = { periodEnd = it.take(10) },
                        label = { Text("Hasta") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ResultMetric("Bruto", "${formatPz(grossSalary)} Pz", Modifier.weight(1f), PremiumPurple)
                    ResultMetric("Neto empleado", "${formatPz(netSalary)} Pz", Modifier.weight(1f), IncomeGreen)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ResultMetric("Trabajador ${config.payrollWorkerTaxPercent}%", "-${formatPz(workerTax)} Pz", Modifier.weight(1f), ErrorRed)
                    ResultMetric("Empresa ${config.payrollEmployerTaxPercent}%", "+${formatPz(employerTax)} Pz", Modifier.weight(1f), Gold)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f), contentColor = Ink),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Coste total empresa", fontWeight = FontWeight.Bold)
                            Text("Bruto + cotización empresarial", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${formatPz(employerCost)} Pz", color = PremiumPurple, fontWeight = FontWeight.Black)
                    }
                }
                Button(
                    onClick = {
                        val contract = selectedContract ?: return@Button
                        val start = runCatching { LocalDate.parse(periodStart) }.getOrNull() ?: LocalDate.now()
                        val end = runCatching { LocalDate.parse(periodEnd) }.getOrNull() ?: start.plusDays(contract.frequency.periodDays() - 1)
                        onPeriod(
                            PayrollPeriod(
                                id = "period-${UUID.randomUUID()}",
                                contractId = contract.id,
                                companyAccountId = contract.companyAccountId,
                                employeeAccountId = contract.employeeAccountId,
                                employeeDip = contract.employeeDip,
                                label = periodLabel.ifBlank { "${contract.frequency.label()} ${end}" },
                                periodStart = start,
                                periodEnd = end,
                                grossSalaryPz = contract.grossSalaryPz,
                                workerTaxPz = payrollTax(contract.grossSalaryPz, config.payrollWorkerTaxPercent),
                                employerTaxPz = payrollTax(contract.grossSalaryPz, config.payrollEmployerTaxPercent),
                                netSalaryPz = (contract.grossSalaryPz - payrollTax(contract.grossSalaryPz, config.payrollWorkerTaxPercent)).coerceAtLeast(0L)
                            )
                        )
                    },
                    enabled = selectedContract?.status == PayrollContractStatus.Active,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Añadir periodo pendiente", fontWeight = FontWeight.Black)
                }

                pendingPeriods.take(6).forEach { period ->
                    val contract = contracts.firstOrNull { it.id == period.contractId }
                    val scheduled = engine.nextPayrollPaymentDate(contract?.frequency ?: PayrollFrequency.Weekly, period.periodEnd)
                    PayrollPeriodRow(period = period, scheduledDate = scheduled, onPay = { onPayPeriod(period) })
                }
                if (paidPeriods.isNotEmpty()) {
                    Text("Últimos abonados", fontWeight = FontWeight.Bold)
                    paidPeriods.take(4).forEach { period ->
                        Text("${period.label} · ${period.employeeDip} · ${formatPz(period.netSalaryPz)} Pz netos · PDF disponible", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedButton(
                    onClick = {
                        val employee = target ?: return@OutlinedButton
                        if (grossSalary >= config.minimumWeeklySalaryPz) onRegister(employee, grossSalary)
                    },
                    enabled = target != null && dipLooksValid && grossSalary >= config.minimumWeeklySalaryPz,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Pago rápido sin contrato")
                }
            }
        }
    }
}

@Composable
private fun ContractSummary(contract: PayrollContract, periods: List<PayrollPeriod>) {
    val paid = periods.count { it.status == PayrollPeriodStatus.Paid }
    val pending = periods.count { it.status == PayrollPeriodStatus.Pending }
    val nextPayment = EconomyEngine().nextPayrollPaymentDate(contract.frequency)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${contract.employeeName} · ${contract.employeeDip}", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            StatusBadge(contract.status.name, contract.status.statusColor())
        }
        Text("${contract.roleTitle} · ${contract.frequency.label()} · antigüedad ${contract.seniorityDays()} días", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        Text("Próximo pago: ${formatPayrollPaymentDate(nextPayment)}", color = PremiumPurple, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ResultMetric("Sueldo", "${formatPz(contract.grossSalaryPz)} Pz", Modifier.weight(1f), PremiumPurple)
            ResultMetric("Pendientes", pending.toString(), Modifier.weight(1f), Gold)
            ResultMetric("Abonados", paid.toString(), Modifier.weight(1f), IncomeGreen)
        }
        if (contract.salaryHistory.isNotEmpty()) {
            Text("Cambios: ${contract.salaryHistory.takeLast(2).joinToString(" · ") { "${formatPz(it.previousGrossSalaryPz)}>${formatPz(it.newGrossSalaryPz)}" }}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PayrollPeriodRow(period: PayrollPeriod, scheduledDate: LocalDate, onPay: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.84f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Payments, contentDescription = null, tint = PremiumPurple)
        Column(Modifier.weight(1f)) {
            Text(period.label, fontWeight = FontWeight.Bold)
            Text("${period.employeeDip} · ${period.periodStart} / ${period.periodEnd}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            Text(
                "Bruto ${formatPz(period.grossSalaryPz)} Pz · Ret. ${formatPz(period.workerTaxPz)} Pz · Neto ${formatPz(period.netSalaryPz)} Pz",
                color = Color(0xFF6C5878),
                style = MaterialTheme.typography.bodySmall
            )
            Text("Programado: ${formatPayrollPaymentDate(scheduledDate)}", color = PremiumPurple, style = MaterialTheme.typography.labelSmall)
        }
        Button(onClick = onPay, colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(12.dp)) {
            Text("Abonar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProductSlideUp(
    account: Account,
    accounts: Map<String, Account>,
    activeUser: UserProfile,
    config: TreasuryConfig,
    engine: EconomyEngine,
    onClose: () -> Unit,
    onCreated: (EconomyResult<Account>, MemberTier) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(AccountType.Current) }
    var cardTier by remember { mutableStateOf(MemberTier.Standard) }
    var acceptedTerms by remember { mutableStateOf(false) }
    val productTypes = listOf(AccountType.Current, AccountType.Savings, AccountType.Child, AccountType.Business, AccountType.Investment)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Dar de alta producto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Nuevo IBAN app GDLP-APXX-XXX automático", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it.take(34) },
                label = { Text("Nombre visible") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(productTypes) { type ->
                    FilterChip(
                        selected = accountType == type,
                        onClick = { accountType = type },
                        label = { Text(type.label()) }
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MemberTier.entries) { tier ->
                    FilterChip(
                        selected = cardTier == tier,
                        onClick = { cardTier = tier },
                        label = { Text("Tarjeta ${tier.name}") }
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EEFF), contentColor = Ink),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(accountType.label(), fontWeight = FontWeight.Black)
                    Text(
                        when (accountType) {
                            AccountType.Current -> "Cuenta corriente para pagos, ingresos y transferencias diarias."
                            AccountType.Savings -> "Hucha bloqueada para ahorro y rentabilidad anual."
                            AccountType.Child -> "Cuenta infantil con límite de envío y cuenta tutora."
                            AccountType.Business -> "Producto empresa con umbral institucional."
                            AccountType.Investment -> "Cartera separada para activos del mercado GDLP."
                            AccountType.State -> "Cuenta institucional reservada para sistema y tesorería."
                        },
                        color = Color(0xFF6C5878)
                    )
                    Text("Alta tarjeta ${formatPz(config.cardIssueFeePz)} Pz · Alta empresa ${formatPz(config.businessRegistrationFeePz)} Pz", color = PremiumPurple, fontWeight = FontWeight.Bold)
                    Text("Límite: ${accountTypeAccountLimit(config, accountType)} cuentas · saldo máx ${formatPz(accountTypeBalanceLimit(config, accountType))} Pz", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (acceptedTerms) PremiumPurple else Color.Black, RoundedCornerShape(16.dp))
                    .clickable { acceptedTerms = !acceptedTerms }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (acceptedTerms) Icons.Default.CheckCircle else Icons.Default.Info, contentDescription = null, tint = if (acceptedTerms) IncomeGreen else Color.Black)
                Column(Modifier.weight(1f)) {
                    Text("Acepto las condiciones de ${accountType.label()}", fontWeight = FontWeight.Black)
                    Text("Se generará el contrato PDF de este producto solo tras aceptar y crear el alta.", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = {
                    val name = accountName.ifBlank { accountType.label() }
                    val tier = if (accountType == AccountType.Child) MemberTier.Child else cardTier
                    onCreated(engine.createAccount(accounts, activeUser.placetaId, name, accountType, parentAccountId = if (accountType == AccountType.Child) account.id else null), tier)
                    accountName = ""
                    acceptedTerms = false
                },
                enabled = acceptedTerms,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("CREAR PRODUCTO", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun InversionesScreen(
    activeUser: UserProfile,
    accounts: Map<String, Account>,
    ownedAccounts: List<Account>,
    businessAccounts: List<Account>,
    selectedAccountId: String,
    onSelectedAccount: (String) -> Unit,
    transactions: List<LedgerTransaction>,
    holdings: List<InvestmentHolding>,
    operations: List<InvestmentOperation>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val userAccounts = ownedAccounts.filter { it.isActiveAccount() }.sortedBy { it.type.ordinal }
    val selected = userAccounts.firstOrNull { it.id == selectedAccountId } ?: userAccounts.firstOrNull()
    val isBusiness = selected?.type == AccountType.Business
    val fundAccounts = accounts.values.filter { it.isActiveAccount() && it.type == AccountType.Business && it.listedInvestmentFund }
        .sortedBy { it.displayName }
    var selectedFund by remember { mutableStateOf<Account?>(null) }
    var showTrade by remember { mutableStateOf<Account?>(null) }
    var tradeAmount by remember { mutableStateOf("") }
    var showFundRegistration by remember { mutableStateOf(false) }
    var showHoldings by remember { mutableStateOf(false) }

    // Limpieza de estado al cambiar cuenta
    LaunchedEffect(selectedAccountId) { selectedFund = null; showTrade = null; showFundRegistration = false }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Inversiones · Mercado GDLP")
        if (userAccounts.isNotEmpty()) {
            AccountSwitcher(userAccounts, selected?.id.orEmpty(), onSelectedAccount)
        }
        if (isBusiness && selected != null) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showFundRegistration = !showFundRegistration },
                colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.08f), contentColor = Ink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (selected.listedInvestmentFund) Icons.Default.CheckCircle else Icons.Default.Add, contentDescription = null, tint = PremiumPurple)
                    Column(Modifier.weight(1f)) {
                        Text(if (selected.listedInvestmentFund) "Tu empresa es un fondo registrado" else "Registra tu empresa como fondo", fontWeight = FontWeight.Black)
                        Text(if (selected.listedInvestmentFund) "Riesgo R${selected.investmentRiskLevel} · Recibe inversiones 60s" else "Aparece en el catálogo y recibe inversiones", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(if (showFundRegistration) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = PremiumPurple)
                }
            }
            if (showFundRegistration) {
                BusinessFundRegistrationScreen(listOf(selected), selected, transactions, onSelectedAccount) { updated ->
                    show("Fondo actualizado")
                    showFundRegistration = false
                }
            }
        }

        // Catálogo de fondos disponibles
        val availableFunds = fundAccounts.filter { it.id != selected?.id }
        if (availableFunds.isNotEmpty() || isBusiness) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showHoldings = !showHoldings },
                colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = PremiumPurple)
                    Column(Modifier.weight(1f)) {
                        Text("Catálogo de fondos", fontWeight = FontWeight.Black)
                        Text("${availableFunds.size} fondo${if (availableFunds.size != 1) "s" else ""} disponible${if (availableFunds.size != 1) "s" else ""} para invertir", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(if (showHoldings) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = PremiumPurple)
                }
            }
            if (showHoldings) {
                if (availableFunds.isEmpty() && isBusiness) {
                    CardBlock { Text("No hay otros fondos disponibles en el mercado.", color = Color(0xFF6C5878)) }
                }
                availableFunds.forEach { fund ->
                    FundMarketRow(
                        company = fund,
                        selected = selectedFund?.id == fund.id,
                        onClick = { selectedFund = if (selectedFund?.id == fund.id) null else fund },
                        onInvest = { showTrade = fund }
                    )
                }
            }
        } else {
            CardBlock { Text("No hay fondos de inversión disponibles. Las empresas pueden darse de alta como fondo desde una cuenta de empresa.", color = Color(0xFF6C5878)) }
        }

        // Detalle del fondo seleccionado
        selectedFund?.let { fund ->
            FundDetailsSlideUp(
                company = fund,
                onInvest = { showTrade = fund },
                onDismiss = { selectedFund = null }
            )
        }

        // Trading - solo cuentas de tipo Investment pueden invertir
        showTrade?.let { fund ->
            val invAccount = accounts.values.firstOrNull { it.type == AccountType.Investment && it.isOwnedBy(activeUser) && it.isActiveAccount() }
            if (invAccount != null) {
                TradeSlideUp(
                    company = fund,
                    investmentAccount = invAccount,
                    tradeAmount = tradeAmount,
                    onTradeAmountChange = { tradeAmount = it },
                    dailyInvestmentCount = operations.count { it.companyId == fund.id && it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() },
                    config = config,
                    limits = investmentRiskLimits(config, fund.investmentRiskLevel),
                    onDismiss = { showTrade = null; tradeAmount = "" },
                    onStart = {
                        val amt = parseMoneyInput(tradeAmount)
                        if (amt > 0) {
                            apply(engine.buyInvestment(accounts, invAccount.id, fund.id, amt, fund.displayName))
                            showTrade = null; tradeAmount = ""
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) { show("No hay cuenta de inversión disponible"); showTrade = null }
            }
        }

        // Cartera de inversiones (holdings)
        if (holdings.isNotEmpty()) {
            val totalValue = holdings.sumOf { it.currentValuePz * it.units }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B69), contentColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MI CARTERA", color = Gold, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                    Text(formatPz(totalValue), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("${holdings.size} activo${if (holdings.size != 1) "s" else ""} en cartera", color = Color(0xFFDCCAF7))
                }
            }
            holdings.forEach { holding ->
                val lastPerf = holding.performance.lastOrNull() ?: 0
                val perfColor = if (lastPerf >= 0) IncomeGreen else ErrorRed
                val perfIcon = if (lastPerf >= 0) Icons.Default.TrendingUp else Icons.Default.Warning
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = PremiumPurple, modifier = Modifier.size(36.dp))
                        Column(Modifier.weight(1f)) {
                            Text(holding.assetName, fontWeight = FontWeight.Black)
                            Text("${holding.units} unidad${if (holding.units != 1) "es" else ""} · ${formatPz(holding.currentValuePz)} Pz c/u", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatPz(holding.currentValuePz * holding.units), fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(perfIcon, contentDescription = null, tint = perfColor, modifier = Modifier.size(14.dp))
                                Text("${if (lastPerf >= 0) "+" else ""}${lastPerf}%", color = perfColor, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TributosScreen(
    accounts: Map<String, Account>,
    transactions: List<LedgerTransaction>,
    flags: List<ComplianceFlag>,
    config: TreasuryConfig,
    engine: EconomyEngine,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val tglp = accounts[TGLP_ID] ?: return
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    val realTransfers = transactions.filter { it.kind != TransactionKind.Fine && it.kind != TransactionKind.ExternalBlocked }
    val matches = realTransfers.filter {
        it.id.contains(search, true) ||
            it.note.contains(search, true) ||
            it.kind.name.contains(search, true) ||
            accounts[it.fromAccountId]?.displayName.orEmpty().contains(search, true) ||
            accounts[it.toAccountId]?.displayName.orEmpty().contains(search, true)
    }
    val weeklyTaxTotal = transactions.sumOf { it.fiscalCollectedAmount() }
    val pendingFlags = flags.count { it.status != com.placeta.banco.core.ComplianceStatus.Approved }
    var section by remember { mutableStateOf(TributosSection.Overview) }
    var showAuditSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    Box {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TributosPurple, contentColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TGLP", color = Color(0xFFE5D5FF), fontWeight = FontWeight.ExtraBold)
                            Text("${formatPz(tglp.balancePz)} Pz", style = MaterialTheme.typography.headlineMedium, color = Mint, fontWeight = FontWeight.Black)
                            Text("Tesorería fiscal activa", color = Color(0xFFE5D5FF))
                        }
                        Icon(Icons.Default.AssuredWorkload, contentDescription = null, tint = Gold, modifier = Modifier.size(42.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PortfolioActionButton("PDF", Icons.Default.Download, Modifier.weight(1f)) { showReportSheet = true }
                        PortfolioActionButton("Auditar", Icons.Default.Search, Modifier.weight(1f)) {
                            section = TributosSection.Audit
                            showAuditSheet = true
                        }
                        PortfolioActionButton("Alertas", Icons.Default.Warning, Modifier.weight(1f)) { showAlertsSheet = true }
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TributosSection.entries) { item ->
                    FilterChip(selected = section == item, onClick = { section = item }, label = { Text(item.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FiscalMiniCard("Recaudado", "${formatPz(weeklyTaxTotal)} Pz", Icons.Default.AssuredWorkload, TributosPurple, Modifier.weight(1f))
                FiscalMiniCard("Alertas", "$pendingFlags pendientes", Icons.Default.Warning, ErrorRed, Modifier.weight(1f))
            }
            when (section) {
                TributosSection.Overview -> {
                    CardBlock {
                        Text("Actividad fiscal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        HeatMap(transactions)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showReportSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TributosPurple),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                section = TributosSection.Audit
                                showAuditSheet = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Auditar")
                        }
                    }
                }
                TributosSection.Audit -> {
                    CardBlock {
                        Text("Auditoría rápida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        TransactionKind.entries.filter { kind -> realTransfers.any { it.kind == kind } }.take(6).forEach { kind ->
                            val txns = realTransfers.filter { it.kind == kind }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(kind.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${txns.size} · ${formatPz(txns.sumOf { it.fiscalCollectedAmount() })} imp.", color = Color(0xFF6C5878))
                            }
                        }
                        OutlinedButton(onClick = { showAuditSheet = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Abrir buscador")
                        }
                    }
                }
            }
        }
        BottomSlideUpOverlay(visible = showReportSheet, onDismiss = { showReportSheet = false }) {
            TglpReportSlideUp(
                tglp = tglp,
                weeklyTaxTotal = weeklyTaxTotal,
                transactionCount = realTransfers.size,
                pendingFlags = pendingFlags,
                onGenerate = {
                    val doc = DigitalDocument("tglp-weekly-${System.currentTimeMillis()}", tglp.id, "Liquidación semanal TGLP", DocumentKind.WeeklyTaxReport)
                    show(PdfExporter.createAndOpenTaxPdf(context, tglp, doc, transactions, config))
                    showReportSheet = false
                },
                onDismiss = { showReportSheet = false }
            )
        }
        BottomSlideUpOverlay(visible = showAuditSheet, onDismiss = { showAuditSheet = false }) {
            TglpAuditSlideUp(
                search = search,
                onSearch = { search = it.take(50) },
                matches = matches,
                accounts = accounts,
                onReverse = { txn -> apply(engine.reverseConsumption(accounts, tglp.copy(role = Role.Tributos), txn)) },
                onDismiss = { showAuditSheet = false }
            )
        }
        BottomSlideUpOverlay(visible = showAlertsSheet, onDismiss = { showAlertsSheet = false }) {
            TglpAlertsSlideUp(
                flags = flags,
                accounts = accounts,
                onDismiss = { showAlertsSheet = false }
            )
        }
    }
}

@Composable
private fun TglpReportSlideUp(
    tglp: Account,
    weeklyTaxTotal: Long,
    transactionCount: Int,
    pendingFlags: Int,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Informe TGLP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Liquidación fiscal semanal", color = Color(0xFF6C5878))
                }
                Icon(Icons.Default.Description, contentDescription = null, tint = TributosPurple)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultMetric("Saldo", formatPz(tglp.balancePz), Modifier.weight(1f), TributosPurple)
                ResultMetric("Recaudado", formatPz(weeklyTaxTotal), Modifier.weight(1f), IncomeGreen)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultMetric("Ops", transactionCount.toString(), Modifier.weight(1f), PremiumPurple)
                ResultMetric("Alertas", pendingFlags.toString(), Modifier.weight(1f), ErrorRed)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Cerrar")
                }
                Button(onClick = onGenerate, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = TributosPurple), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Generar")
                }
            }
        }
    }
}

@Composable
private fun TglpAuditSlideUp(
    search: String,
    onSearch: (String) -> Unit,
    matches: List<LedgerTransaction>,
    accounts: Map<String, Account>,
    onReverse: (LedgerTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Auditoría TGLP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("${matches.size} coincidencias", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = TributosPurple)
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                label = { Text("Buscar ID, cuenta o concepto") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matches.take(20).forEach { txn ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfacePurple, contentColor = Ink),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(txn.note.ifBlank { txn.kind.name }, fontWeight = FontWeight.Black)
                                Text("${accounts[txn.fromAccountId]?.displayName ?: txn.fromAccountId} -> ${accounts[txn.toAccountId]?.displayName ?: txn.toAccountId}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                Text("${formatSignedPz(txn.signedAmountFor(TGLP_ID))} Pz · imp. ${formatSignedPz(txn.signedFiscalAmountFor(TGLP_ID))}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onReverse(txn) }, enabled = txn.kind == TransactionKind.Consumption && txn.status != TransactionStatus.Reversed) {
                                Icon(Icons.Default.Replay, contentDescription = "Revertir", tint = TributosPurple)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TglpAlertsSlideUp(
    flags: List<ComplianceFlag>,
    accounts: Map<String, Account>,
    onDismiss: () -> Unit
) {
    val pending = flags.filter { it.status != com.placeta.banco.core.ComplianceStatus.Approved }
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Alertas fiscales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("${pending.size} pendientes", color = Color(0xFF6C5878))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = ErrorRed)
                }
            }
            if (pending.isEmpty()) {
                EmptyState(Icons.Default.CheckCircle, "Todo limpio", "No hay requerimientos fiscales pendientes.")
            } else {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pending.take(20).forEach { flag ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f), contentColor = Ink),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(accounts[flag.accountId]?.displayName ?: flag.accountId, fontWeight = FontWeight.Black)
                                Text(flag.reason, color = Color(0xFF6C5878))
                                Text("${formatPz(flag.amountPz)} Pz · ${flag.status.name}", color = ErrorRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun HeatMap(transactions: List<LedgerTransaction>) {
    val buckets = TransactionKind.entries.take(12).map { kind -> transactions.count { it.kind == kind } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        buckets.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { count ->
                    val alpha = (0.18f + count * 0.12f).coerceAtMost(1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(height = 34.dp, width = 1.dp)
                            .background(PremiumPurple.copy(alpha = alpha), RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}



@Composable
private fun PromoEditorSlideUp(
    promo: PromoSlide,
    onClose: () -> Unit,
    onSave: (PromoSlide) -> Unit
) {
    var title by remember(promo.id, promo.title) { mutableStateOf(promo.title) }
    var subtitle by remember(promo.id, promo.subtitle) { mutableStateOf(promo.subtitle) }
    var action by remember(promo.id, promo.action) { mutableStateOf(promo.action) }
    var imageKey by remember(promo.id, promo.imageKey) { mutableStateOf(promo.imageKey) }
    var imageUrl by remember(promo.id, promo.imageUrl) { mutableStateOf(promo.imageUrl.orEmpty()) }
    var assetPath by remember(promo.id, promo.assetPath) { mutableStateOf(promo.assetPath.orEmpty()) }
    var savePressed by remember(promo.id, promo.title, promo.subtitle, promo.action, promo.imageKey, promo.imageUrl, promo.assetPath) { mutableStateOf(false) }
    val context = LocalContext.current
    val promoAssets = remember {
        context.assets.list("promos")
            ?.filter { it.endsWith(".png", true) || it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".webp", true) }
            ?.map { "promos/$it" }
            .orEmpty()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Editar promo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(promo.id, color = Color(0xFF6C5878))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Cerrar", tint = PremiumPurple)
                }
            }
            OutlinedTextField(title, { title = it.take(28) }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(subtitle, { subtitle = it.take(120) }, label = { Text("Subtítulo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PromoAction.entries) { candidate ->
                    FilterChip(selected = action == candidate, onClick = { action = candidate }, label = { Text(candidate.label()) })
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumPurple.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(170.dp)) {
                    PromoBackgroundImage(
                        slide = promo.copy(imageKey = imageKey, imageUrl = imageUrl.takeIf { it.isNotBlank() }, assetPath = assetPath.takeIf { it.isNotBlank() }),
                        offline = false,
                        contentDescription = "Previsualización promo",
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        "PREVIEW VERTICAL",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    )
                }
            }
            OutlinedTextField(
                value = imageUrl,
                onValueChange = {
                    imageUrl = it.take(500)
                    if (imageUrl.isNotBlank()) assetPath = ""
                },
                label = { Text("URL de imagen vertical") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            if (promoAssets.isNotEmpty()) {
                Text("Assets locales /assets/promos", color = Color(0xFF6C5878), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(promoAssets) { path ->
                        FilterChip(
                            selected = assetPath == path,
                            onClick = {
                                assetPath = path
                                imageUrl = ""
                            },
                            label = { Text(path.substringAfterLast('/')) }
                        )
                    }
                }
            } else {
                Text("No hay imágenes en assets/promos", color = Color(0xFF6C5878))
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(promoAssets.filter { Regex("^promos/promo\\d+\\.", RegexOption.IGNORE_CASE).containsMatchIn(it) }) { path ->
                    FilterChip(
                        selected = imageUrl.isBlank() && assetPath == path,
                        onClick = {
                            imageUrl = ""
                            assetPath = path
                        },
                        label = { Text("Fallback ${path.substringAfterLast('/').substringBeforeLast('.')}") }
                    )
                }
            }
            Button(
                onClick = {
                    savePressed = true
                    val updated = promo.copy(
                        title = title.trim().ifBlank { promo.title },
                        subtitle = subtitle.trim().ifBlank { promo.subtitle },
                        action = action,
                        imageKey = imageKey,
                        imageUrl = imageUrl.trim().takeIf { it.isNotBlank() },
                        assetPath = assetPath.trim().takeIf { it.isNotBlank() }
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (savePressed) "GUARDANDO EN MONGO..." else "GUARDAR PROMO", fontWeight = FontWeight.Black)
            }
            if (savePressed) {
                Text(
                    "Pulsa cerrar tras unos segundos; si Mongo falla aparecerá el aviso de conexión.",
                    color = Color(0xFF6C5878),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Sparkline(points: List<Long>) {
    Canvas(modifier = Modifier.fillMaxWidth().size(height = 56.dp, width = 1.dp)) {
        if (points.size < 2) return@Canvas
        val min = points.minOrNull() ?: 0
        val max = points.maxOrNull() ?: 1
        val range = (max - min).coerceAtLeast(1)
        val step = size.width / (points.size - 1)
        points.zipWithNext().forEachIndexed { index, pair ->
            val y1 = size.height - ((pair.first - min).toFloat() / range * size.height)
            val y2 = size.height - ((pair.second - min).toFloat() / range * size.height)
            drawLine(Mint, Offset(index * step, y1), Offset((index + 1) * step, y2), strokeWidth = 5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun RadarPulse() {
    val pulse by rememberInfiniteTransition(label = "radar-pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1600),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar-radius"
    )
    Canvas(modifier = Modifier.size(190.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        listOf(0.32f, 0.58f, 0.84f).forEachIndexed { index, factor ->
            val animatedFactor = (factor + pulse * 0.18f).coerceAtMost(1f)
            drawCircle(
                color = SoftPurple.copy(alpha = (0.26f - pulse * 0.16f - index * 0.04f).coerceAtLeast(0.04f)),
                radius = maxRadius * animatedFactor,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )
        }
        drawCircle(Mint.copy(alpha = 0.20f), maxRadius * 0.24f, center)
        drawCircle(Mint, 12f, center)
        drawLine(
            color = Gold,
            start = center,
            end = Offset(center.x + maxRadius * 0.75f, center.y - maxRadius * 0.22f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BalanceCard(title: String, amount: Long, subtitle: String, accent: Color) {
    CardBlock {
        Text(title, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${formatPz(amount)} Pz", style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun VaultCard(title: String, amount: Long, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TributosPurple, contentColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color(0xFFE5D5FF))
            Text("${formatPz(amount)} Pz", style = MaterialTheme.typography.headlineMedium, color = Mint)
            Text(subtitle, color = Color(0xFFE5D5FF))
        }
    }
}

@Composable
private fun History(account: Account, transactions: List<LedgerTransaction>, config: TreasuryConfig) {
    CardBlock {
        Text("Feed de Transacciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        if (transactions.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Payments,
                title = "Aún no hay movimientos",
                message = "Cuando envíes, recibas o cobres la RBU, aparecerá aquí con su recibo."
            )
        } else {
            transactions.take(10).forEach { TransactionFeedLine(account, it, config) }
        }
    }
}

@Composable
private fun SixPointStar(
    modifier: Modifier = Modifier,
    color: Color = PremiumPurple,
    strokeWidth: Float = 2f
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = minOf(cx, cy) * 0.9f
        val innerR = outerR * 0.5f
        val path = androidx.compose.ui.graphics.Path().apply {
            for (i in 0 until 6) {
                val outerAngle = Math.toRadians((-90.0 + i * 60.0))
                val innerAngle = Math.toRadians((-90.0 + i * 60.0 + 30.0))
                val ox = cx + outerR * cos(outerAngle).toFloat()
                val oy = cy + outerR * sin(outerAngle).toFloat()
                val ix = cx + innerR * cos(innerAngle).toFloat()
                val iy = cy + innerR * sin(innerAngle).toFloat()
                if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                lineTo(ix, iy)
            }
            close()
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(54.dp).background(PremiumPurple.copy(alpha = 0.10f), RoundedCornerShape(27.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PremiumPurple)
        }
        Text(title, fontWeight = FontWeight.Black)
        Text(message, color = Color(0xFF6C5878))
    }
}

@Composable
private fun CompactTransactionLine(account: Account, transaction: LedgerTransaction, config: TreasuryConfig) {
    var showReceipt by remember { mutableStateOf(false) }
    val signedAmount = transaction.signedAmountFor(account.id)
    val incoming = signedAmount >= 0
    val color = if (incoming) IncomeGreen else ErrorRed
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { showReceipt = true }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (incoming) {
                SixPointStar(modifier = Modifier.size(20.dp), color = color, strokeWidth = 2.2f)
            } else {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(transaction.publicDescription(), fontWeight = FontWeight.Bold, maxLines = 1)
            Text(HistoryDateFormat.format(transaction.createdAt.atZone(ZoneId.systemDefault())), color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
        }
        Text(formatSignedPz(signedAmount), color = color, fontWeight = FontWeight.Black)
    }
    if (showReceipt) TransactionReceiptDialog(account = account, transaction = transaction, config = config, onDismiss = { showReceipt = false })
}

@Composable
private fun TransactionFeedLine(account: Account, transaction: LedgerTransaction, config: TreasuryConfig) {
    var showReceipt by remember { mutableStateOf(false) }
    val signedAmount = transaction.signedAmountFor(account.id)
    val positive = signedAmount >= 0
    val amountColor = if (positive) IncomeGreen else ErrorRed
    val icon = when {
        positive -> Icons.Default.ArrowDownward
        transaction.kind == TransactionKind.OperationalFee || transaction.kind == TransactionKind.Tax -> Icons.Default.Gavel
        else -> Icons.Default.ArrowUpward
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { showReceipt = true }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(PremiumPurple.copy(alpha = 0.10f), RoundedCornerShape(21.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (positive) {
                SixPointStar(modifier = Modifier.size(24.dp), color = PremiumPurple, strokeWidth = 2.5f)
            } else {
                Icon(icon, contentDescription = null, tint = PremiumPurple)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(transaction.publicDescription(), fontWeight = FontWeight.Bold)
            Text(transaction.fiscalSubtitle(), color = Color(0xFF6C5878))
        }
        Text(
            "${formatSignedPz(signedAmount)} Pz",
            color = amountColor,
            fontWeight = FontWeight.Black
        )
    }
    if (showReceipt) TransactionReceiptDialog(account = account, transaction = transaction, config = config, onDismiss = { showReceipt = false })
}

@Composable
private fun TransactionRow(transaction: LedgerTransaction, onReverse: () -> Unit) {
    CardBlock {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TransactionSummary(transaction, Modifier.weight(1f))
            IconButton(onClick = onReverse, enabled = transaction.kind == TransactionKind.Consumption && transaction.status != TransactionStatus.Reversed) {
                Icon(Icons.Default.Replay, contentDescription = "Revertir")
            }
        }
    }
}

@Composable
private fun TransactionLine(transaction: LedgerTransaction, config: TreasuryConfig) {
    var expanded by remember { mutableStateOf(false) }
    var showReceipt by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                showReceipt = true
            }
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TransactionSummary(transaction, Modifier.weight(1f))
            Text("${formatPz(transaction.amountPz)} Pz", fontWeight = FontWeight.ExtraBold, color = transactionColor(transaction))
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4EEFF), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Neto ${formatPz(transaction.netAmount)} Pz · Impuesto ${formatPz(transaction.taxAmount)} Pz", color = Ink)
                Text("Origen ${transaction.IBAN_Origin}", color = Color(0xFF6C5878))
                Text("ID ${transaction.id.take(18)} · ${transaction.concept}", color = Color(0xFF6C5878))
            }
        }
    }
    if (showReceipt) {
        TransactionReceiptDialog(account = null, transaction = transaction, config = config, onDismiss = { showReceipt = false })
    }
}

@Composable
private fun TransactionReceiptDialog(account: Account?, transaction: LedgerTransaction, config: TreasuryConfig, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val signedAmount = account?.let { transaction.signedAmountFor(it.id) }
    val signedNet = signedAmount?.let { if (it < 0) -transaction.netAmount else transaction.netAmount }
    val signedTax = account?.let { transaction.signedFiscalAmountFor(it.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                account?.let { receiptAccount ->
                    TextButton(
                        onClick = {
                            val doc = DigitalDocument("transfer-${transaction.id}", receiptAccount.id, "Justificante ${transaction.kind.name} · ${formatPz(transaction.amountPz)} Pz", DocumentKind.PaymentReceipt)
                            PdfExporter.createAndOpenTaxPdf(context, receiptAccount, doc, listOf(transaction), config)
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = PremiumPurple)
                        Spacer(Modifier.size(6.dp))
                        Text("PDF", color = PremiumPurple, fontWeight = FontWeight.Black)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("CERRAR", color = PremiumPurple, fontWeight = FontWeight.Black)
                }
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("RECIBO GDLP", fontWeight = FontWeight.Black)
                Text(transaction.id.take(24), color = Color(0xFF6C5878), style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ReceiptRow("Concepto", transaction.concept)
                ReceiptRow("Estado", transaction.status.name)
                ReceiptRow("Bruto", signedAmount?.let { "${formatSignedPz(it)} Pz" } ?: "${formatPz(transaction.amountPz)} Pz")
                ReceiptRow("Neto", signedNet?.let { "${formatSignedPz(it)} Pz" } ?: "${formatPz(transaction.netAmount)} Pz")
                ReceiptRow("Impuesto", signedTax?.let { "${formatSignedPz(it)} Pz" } ?: "${formatPz(transaction.taxAmount)} Pz")
                ReceiptRow("Origen", transaction.IBAN_Origin)
                ReceiptRow("Fecha", HistoryDateFormat.format(transaction.createdAt.atZone(ZoneId.systemDefault())))
                Text(transaction.note, color = Ink, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF6C5878))
        Text(value, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun TransactionSummary(transaction: LedgerTransaction, modifier: Modifier = Modifier) {
    val color = transactionColor(transaction)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(5.dp)))
        Spacer(Modifier.size(10.dp))
        Column {
            Text(transaction.note, fontWeight = FontWeight.SemiBold)
            Text(
                "${transaction.kind.name} · ${transaction.status.name} · ${HistoryDateFormat.format(transaction.createdAt.atZone(ZoneId.systemDefault()))}",
                color = Color(0xFF6C5878)
            )
        }
    }
}

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.border(1.dp, PremiumPurple.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(width = 6.dp, height = 26.dp).background(PremiumPurple, RoundedCornerShape(5.dp)))
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = PremiumPurple)
    }
}

@Composable
private fun WeeklyTaxEstimateCard(account: Account, config: TreasuryConfig, engine: EconomyEngine) {
    val weeklyTax = engine.estimateWeeklyTax(account)
    val irm = if (account.type == AccountType.Business) engine.irmReserve(account.balancePz) else 0L
    val taxColor = when {
        weeklyTax <= 0L -> Mint
        weeklyTax < account.balancePz / 50 -> Mint
        weeklyTax < account.balancePz / 10 -> Gold
        else -> ErrorRed
    }
    CardBlock {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Estimado próximo impuesto semanal", style = MaterialTheme.typography.titleMedium)
                Text("Fecha: ${engine.nextIrmPaymentDate()}", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
                Text("Tipo impositivo: ${config.weeklyTaxPercent}%", color = Color(0xFF6C5878), style = MaterialTheme.typography.bodySmall)
            }
            Text("${formatPz(weeklyTax)} Pz", color = taxColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        }
        if (account.type == AccountType.Business) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Estimado IRM mensual", color = Color(0xFF6C5878))
                Text("${formatPz(irm)} Pz · ${config.irmBusinessPercent}%", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        Modifier
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
    }
}

private fun contextualDocuments(
    account: Account,
    transactions: List<LedgerTransaction>,
    contracts: List<PayrollContract>,
    periods: List<PayrollPeriod>
): List<DigitalDocument> {
    val statementDocs = if (transactions.isNotEmpty()) {
        val title = when (account.type) {
            AccountType.Business -> "Extracto empresa · ${account.displayName}"
            AccountType.State -> "Extracto administrativo · ${account.displayName}"
            else -> "Extracto mensual · ${account.displayName}"
        }
        listOf(DigitalDocument("statement-${account.id}-${LocalDate.now().year}-${LocalDate.now().monthValue}", account.id, title, if (account.type == AccountType.Business || account.type == AccountType.State) DocumentKind.BusinessStatement else DocumentKind.MonthlyStatement))
    } else {
        emptyList()
    }
    val vatDocs = transactions
        .filter { it.ivaPz > 0 || it.taxAmount > 0 }
        .filter { it.kind == TransactionKind.Consumption || it.kind == TransactionKind.ForcedVatRegularization }
        .sortedByDescending { it.createdAt }
        .take(12)
        .map { DigitalDocument("vat-${it.id}", account.id, "IVA liquidado · ${formatPz(it.ivaPz.coerceAtLeast(it.taxAmount))} Pz", DocumentKind.VatReceipt) }
    val taxDocs = transactions
        .filter { it.kind in setOf(TransactionKind.Tax, TransactionKind.IrmCharge, TransactionKind.InvestmentTax, TransactionKind.LateTaxInterest) }
        .sortedByDescending { it.createdAt }
        .take(12)
        .map { DigitalDocument("tax-${it.id}", account.id, "Impuesto liquidado · ${it.publicDescription()}", DocumentKind.WeeklyTaxReport) }
    val laborDocs = if (account.type == AccountType.Business) {
        contracts
            .filter { it.status == PayrollContractStatus.Active }
            .sortedBy { it.employeeName }
            .map { DigitalDocument("labor-${it.id}", account.id, "Contrato laboral · ${it.employeeName}", DocumentKind.LaborContract) }
    } else {
        emptyList()
    }
    val payslipDocs = periods
        .filter { it.status == PayrollPeriodStatus.Paid }
        .sortedByDescending { it.paidAt ?: it.createdAt }
        .map { DigitalDocument("payroll-${it.id}", it.employeeAccountId, "Nómina ${it.label} · ${it.employeeDip}", DocumentKind.PayrollPayslip) }
    val investmentDocs = transactions
        .filter { it.kind == TransactionKind.InvestmentSell && it.status == TransactionStatus.Settled }
        .sortedByDescending { it.createdAt }
        .map { DigitalDocument("investment-${it.id}", account.id, "Liquidación inversión · ${HistoryDateFormat.format(it.createdAt.atZone(ZoneId.systemDefault()))}", DocumentKind.InvestmentLiquidation) }
    return (statementDocs + vatDocs + taxDocs + laborDocs + payslipDocs + investmentDocs).distinctBy { it.id }
}

private fun AccountType.label(): String = when (this) {
    AccountType.Current -> "Corriente"
    AccountType.Savings -> "Ahorro"
    AccountType.Child -> "Infantil"
    AccountType.Business -> "Empresa"
    AccountType.Investment -> "Inversión"
    AccountType.State -> "Estado"
}

private fun accountTypeAccountLimit(config: TreasuryConfig, type: AccountType): Int = when (type) {
    AccountType.Current -> config.maxCurrentAccounts
    AccountType.Savings -> config.maxSavingsAccounts
    AccountType.Child -> config.maxChildAccounts
    AccountType.Business -> config.maxBusinessAccounts
    AccountType.Investment -> config.maxInvestmentAccounts
    AccountType.State -> Int.MAX_VALUE
}

private fun accountTypeBalanceLimit(config: TreasuryConfig, type: AccountType): Long = when (type) {
    AccountType.Current -> config.maxCurrentBalancePz
    AccountType.Savings -> config.maxSavingsBalancePz
    AccountType.Child -> config.maxChildBalancePz
    AccountType.Business -> config.maxBusinessBalancePz
    AccountType.Investment -> config.maxInvestmentBalancePz
    AccountType.State -> Long.MAX_VALUE / 4
}

private fun transactionColor(transaction: LedgerTransaction): Color = when (transaction.kind) {
    TransactionKind.Fine, TransactionKind.Tax, TransactionKind.InvestmentTax, TransactionKind.ExternalBlocked, TransactionKind.ForcedVatRegularization, TransactionKind.CapitaliaServiceFee -> ErrorRed
    TransactionKind.Rbu, TransactionKind.PayrollLoan, TransactionKind.WelcomeBonus, TransactionKind.Dividend, TransactionKind.InvestmentSell, TransactionKind.SavingsInterest, TransactionKind.Allowance, TransactionKind.Gift -> IncomeGreen
    TransactionKind.Consumption, TransactionKind.Placezum, TransactionKind.Donation, TransactionKind.InvestmentBuy, TransactionKind.InvestmentCommission -> SoftPurple
    else -> Color(0xFFCBB6E6)
}

private fun LedgerTransaction.fiscalCollectedAmount(): Long = when (kind) {
    TransactionKind.Tax,
    TransactionKind.OperationalFee,
    TransactionKind.InvestmentTax,
    TransactionKind.ForcedVatRegularization,
    TransactionKind.LateTaxInterest -> amountPz
    else -> taxAmount.coerceAtLeast(ivaPz).takeIf { status == TransactionStatus.Settled } ?: 0L
}

private fun LedgerTransaction.signedAmountFor(accountId: String): Long = when (accountId) {
    toAccountId -> amountPz
    fromAccountId -> -amountPz
    else -> amountPz
}

private fun LedgerTransaction.signedFiscalAmountFor(accountId: String): Long {
    val fiscalAmount = fiscalCollectedAmount()
    if (fiscalAmount <= 0L) return 0L
    return when {
        accountId == TGLP_ID && toAccountId == TGLP_ID -> fiscalAmount
        accountId == fromAccountId -> -fiscalAmount
        else -> 0L
    }
}

private fun transactionsForAccount(transactions: List<LedgerTransaction>, accountId: String): List<LedgerTransaction> =
    transactions.filter { it.fromAccountId == accountId || it.toAccountId == accountId }

private fun Account.isOwnedBy(user: UserProfile): Boolean {
    val ownerId = placetaId?.normalizedOwnerId().orEmpty()
    val userPlacetaId = user.placetaId.normalizedOwnerId()
    val userDip = user.dip.normalizedOwnerId()
    return id == user.primaryAccountId || ownerId == userPlacetaId || ownerId == userDip
}

private fun Account.isActiveAccount(): Boolean = closedAt == null

private fun Account.mutableTypes(): List<AccountType> {
    if (!isActiveAccount() || kind != AccountKind.CITIZEN || role != Role.Citizen) return emptyList()
    if (type == AccountType.Child || type == AccountType.Business || type == AccountType.State) return emptyList()
    return listOf(AccountType.Current, AccountType.Savings, AccountType.Investment)
}

private fun String.normalizedOwnerId(): String =
    trim().uppercase().replace(Regex("[\\s-]+"), "")

private fun investmentResultRowsFor(
    accountId: String,
    transactions: List<LedgerTransaction>,
    accounts: Map<String, Account>
): List<InvestmentResultRow> {
    val buys = transactions
        .filter { it.fromAccountId == accountId && it.kind == TransactionKind.InvestmentBuy }
        .sortedBy { it.createdAt }
    return transactions
        .filter { it.toAccountId == accountId && it.kind == TransactionKind.InvestmentSell }
        .sortedByDescending { it.createdAt }
        .map { sell ->
            val assetName = accounts[sell.fromAccountId]?.displayName
                ?: sell.note.substringAfter(" en ", "").substringBefore(" (").ifBlank { "Inversión GDLP" }
            val buy = buys.lastOrNull { candidate ->
                candidate.toAccountId == sell.fromAccountId &&
                    candidate.createdAt.isBefore(sell.createdAt) &&
                    (sell.note.contains(assetName, ignoreCase = true) || assetName == "Inversión GDLP")
            } ?: buys.lastOrNull { it.createdAt.isBefore(sell.createdAt) }
            val principal = buy?.amountPz ?: estimateInvestmentPrincipal(sell)
            val movement = Regex("([+-]?\\d+)%").find(sell.note)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { kotlin.math.abs(it) } ?: 0
            val won = sell.note.contains("gana usuario", ignoreCase = true) || sell.amountPz >= principal
            InvestmentResultRow(
                sell = sell,
                assetName = assetName,
                principalPz = principal,
                returnedPz = sell.amountPz,
                netResultPz = sell.amountPz - principal,
                movementPercent = movement,
                won = won
            )
        }
}

private fun estimateInvestmentPrincipal(transaction: LedgerTransaction): Long {
    val movement = Regex("([+-]?\\d+)%").find(transaction.note)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { kotlin.math.abs(it) } ?: 0L
    if (movement <= 0L) return transaction.amountPz
    val denominator = if (transaction.note.contains("gana usuario", true)) 100L + movement else (100L - movement).coerceAtLeast(1L)
    return (transaction.amountPz * 100L / denominator).coerceAtLeast(0)
}

private fun Context.hasNetworkConnection(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun LedgerTransaction.publicDescription(): String = when (kind) {
    TransactionKind.Consumption -> note.ifBlank { "Pago en comercio" }
    TransactionKind.Allowance -> note.ifBlank { "Paga junior" }
    TransactionKind.Gift -> note.ifBlank { "Regalo supervisado" }
    TransactionKind.Rbu -> "Renta Básica Universal"
    TransactionKind.Donation -> note.ifBlank { "Donación" }
    TransactionKind.WelcomeBonus -> "Bono de bienvenida Banco de La Placeta"
    TransactionKind.OperationalFee -> "Tasa de transferencia"
    TransactionKind.CapitaliaServiceFee -> "Comisión de servicio junior"
    TransactionKind.ForcedVatRegularization, TransactionKind.IrmCharge -> "Ajuste Agencia Tributaria"
    TransactionKind.InvestmentBuy -> "Inversión iniciada"
    TransactionKind.InvestmentSell -> "Inversión liquidada"
    TransactionKind.InvestmentTax -> "Impuesto de inversión"
    TransactionKind.InvestmentCommission -> "Comisión inversión"
    TransactionKind.LotteryPrize -> "Premio de lotería"
    else -> note.ifBlank { kind.name }
}

private fun LedgerTransaction.fiscalSubtitle(): String = when (kind) {
    TransactionKind.Consumption -> "IVA 12% incl. · Impuesto ${formatMoneyPz(taxAmount)} Pz"
    TransactionKind.Allowance -> "Cuenta junior · paga autorizada por tutor"
    TransactionKind.Gift -> "Cuenta junior · regalo supervisado"
    TransactionKind.Rbu -> "Concepto: RBU"
    TransactionKind.Donation -> "Fundación Banco de La Placeta"
    TransactionKind.WelcomeBonus -> "Alta de cliente"
    TransactionKind.OperationalFee -> "Tasa de transferencia"
    TransactionKind.CapitaliaServiceFee -> "Liquidación de infraestructura junior al Banco de La Placeta"
    TransactionKind.InvestmentCommission -> "Comisión sobre ganancia de capital"
    TransactionKind.InvestmentTax -> "Impuesto sobre ganancia de capital"
    TransactionKind.InvestmentBuy, TransactionKind.InvestmentSell -> "Rendimiento capital · impuesto si hay beneficio"
    TransactionKind.IrmCharge -> "IRM · Índice de Acumulación"
    else -> concept
}

private fun publishWidgetSnapshot(context: Context, accounts: Map<String, Account>, placetaId: String) {
    val primary = accounts.values.firstOrNull { it.placetaId?.equals(placetaId, ignoreCase = true) == true }
    WidgetDataManager.updateFromState(context, primary, null)
    PlacezumWidgetProvider.notifyDataChanged(context)
}

private fun formatPz(amount: Long): String {
    val sign = if (amount < 0) "-" else ""
    return "$sign${"%,d".format(kotlin.math.abs(amount)).replace(",", ".")},00"
}

private fun formatMoneyPz(amount: Long): String = formatPz(amount)

private fun formatSignedPz(amount: Long): String {
    val sign = if (amount >= 0) "+" else "-"
    return "$sign${formatPz(kotlin.math.abs(amount))}"
}

private fun sanitizeMoneyInput(value: String, maxLength: Int = 12): String {
    val normalized = value.replace(",", ".")
    val builder = StringBuilder()
    var hasSeparator = false
    var decimals = 0
    normalized.forEach { char ->
        when {
            char.isDigit() && !hasSeparator -> builder.append(char)
            char.isDigit() && decimals < 2 -> {
                builder.append(char)
                decimals += 1
            }
            char == '.' && !hasSeparator -> {
                builder.append('.')
                hasSeparator = true
            }
        }
    }
    return builder.toString().take(maxLength)
}

private fun parseMoneyInput(value: String): Long =
    runCatching {
        val clean = value.replace(",", ".").ifBlank { "0" }
        val bd = BigDecimal(clean)
        // If user entered decimals (e.g. "100.50"), discard them — we store whole Pz
        bd.setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrDefault(0L)

private fun formatTimestamp(instant: Instant): String {
    val now = Instant.now()
    val duration = java.time.Duration.between(instant, now)
    return when {
        duration.toMinutes() < 1 -> "Ahora"
        duration.toHours() < 1 -> "Hace ${duration.toMinutes()} min"
        duration.toDays() < 1 -> "Hace ${duration.toHours()} h"
        duration.toDays() < 7 -> "Hace ${duration.toDays()} d"
        else -> {
            val zdt = instant.atZone(ZoneId.systemDefault())
            "${zdt.dayOfMonth}/${zdt.monthValue}/${zdt.year % 100}"
        }
    }
}

private fun generatePkceVerifier(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

private fun pkceChallenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

private fun Uri.callbackParams(): Map<String, String> {
    val params = linkedMapOf<String, String>()
    queryParameterNames.forEach { key ->
        getQueryParameter(key)?.let { params[key] = it }
    }
    val fragment = encodedFragment.orEmpty().trimStart('?')
    if (fragment.contains("=")) {
        val fragmentUri = Uri.parse("bancoplaceta://callback?$fragment")
        fragmentUri.queryParameterNames.forEach { key ->
            fragmentUri.getQueryParameter(key)?.let { params.putIfAbsent(key, it) }
        }
    }
    return params
}

private fun Map<String, String>.placetaToken(): String =
    listOf("placetaid_token", "access_token", "id_token", "token", "jwt")
        .firstNotNullOfOrNull { key -> this[key]?.takeIf(String::isNotBlank) }
        .orEmpty()

private fun Map<String, String>.placetaUserJson(): String =
    listOf("user", "profile", "payload", "claims")
        .firstNotNullOfOrNull { key -> this[key]?.takeIf(String::isNotBlank) }
        .orEmpty()
        .let { value ->
            if (value.startsWith("{")) value else decodeBase64Json(value).orEmpty()
        }

private fun exchangePlacetaIdCode(code: String, codeVerifier: String): Pair<String, String>? {
    val endpoints = listOf("/api/auth/token", "/auth/token", "/api/placetaid/token")
    val form = mapOf(
        "grant_type" to "authorization_code",
        "code" to code,
        "client_id" to BuildConfig.PLACETA_ID_CLIENT_ID,
        "redirect_uri" to BuildConfig.PLACETA_ID_CALLBACK_URL,
        "code_verifier" to codeVerifier
    ).toFormBody()

    endpoints.forEach { path ->
        val response = runCatching {
            val base = BuildConfig.PLACETA_ID_BASE_URL.trimEnd('/')
            val connection = (URL("$base$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrNull()

        val json = runCatching { JSONObject(response.orEmpty()) }.getOrNull() ?: return@forEach
        val token = listOf("placetaid_token", "access_token", "id_token", "token", "jwt")
            .firstNotNullOfOrNull { key -> json.optString(key).takeIf(String::isNotBlank) }
            .orEmpty()
        val user = listOf("user", "profile", "payload", "claims")
            .firstNotNullOfOrNull { key ->
                json.opt(key)?.let { value ->
                    when (value) {
                        is JSONObject -> value.toString()
                        is String -> value.takeIf(String::isNotBlank)?.let { if (it.startsWith("{")) it else decodeBase64Json(it) }
                        else -> null
                    }
                }
            }
            .orEmpty()
            .ifBlank { jwtPayloadJson(token).orEmpty() }
        if (token.isNotBlank() && user.isNotBlank()) return token to user
    }
    return null
}

private fun Map<String, String>.toFormBody(): String =
    entries.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }

private fun decodeBase64Json(value: String): String? = runCatching {
    val normalized = value.trim()
    String(Base64.decode(normalized, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
        .takeIf { it.trimStart().startsWith("{") }
}.getOrNull()

private fun jwtPayloadJson(token: String): String? = runCatching {
    val payload = token.split('.').getOrNull(1)?.takeIf { it.isNotBlank() } ?: return@runCatching null
    decodeBase64Json(payload)
}.getOrNull()

private fun Account.employeeDip(users: List<UserProfile>): String =
    users.firstOrNull { it.primaryAccountId == id || it.placetaId == placetaId }?.dip
        ?: placetaId?.takeIf { Regex("^\\d{8}[A-Z]$").matches(it.trim().uppercase()) }
        ?: "00000000X"

private fun placetaDisplayName(userJson: String): String =
    runCatching {
        val json = JSONObject(userJson)
        listOf(
            json.optString("nombreCompleto"),
            json.optString("displayName"),
            json.optString("name"),
            "${json.optString("nombre")} ${json.optString("apellidos")}".trim(),
            json.optString("dip")
        ).firstOrNull { it.isNotBlank() }
    }.getOrNull() ?: "PlacetaID"

private fun PayrollFrequency.label(): String = when (this) {
    PayrollFrequency.Weekly -> "Semanal"
    PayrollFrequency.Biweekly -> "Quincenal"
    PayrollFrequency.Monthly -> "Mensual"
}

private fun PayrollFrequency.periodDays(): Long = when (this) {
    PayrollFrequency.Weekly -> 7L
    PayrollFrequency.Biweekly -> 14L
    PayrollFrequency.Monthly -> 30L
}

private fun PayrollContractStatus.statusColor(): Color = when (this) {
    PayrollContractStatus.Active -> IncomeGreen
    PayrollContractStatus.Paused -> Gold
    PayrollContractStatus.Draft -> SoftPurple
    PayrollContractStatus.Ended -> ErrorRed
}

private fun PayrollContract.seniorityDays(): Long {
    val start = startDate
    return java.time.temporal.ChronoUnit.DAYS.between(start, java.time.LocalDate.now()).coerceAtLeast(0)
}

private fun formatPayrollPaymentDate(date: LocalDate): String =
    "${date.format(DateTimeFormatter.ofPattern("EEE dd/MM"))} 09:00"

private fun payrollTax(amount: Long, percent: Int): Long =
    if (amount <= 0L || percent <= 0) 0L else (amount * percent + 99) / 100

private fun randomSixDigits(): String = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')

private fun randomFourDigits(): String = Random.nextInt(0, 10_000).toString().padStart(4, '0')

private enum class WalletSection(val label: String, val icon: ImageVector) {
    Summary("Resumen", Icons.Default.Home),
    Accounts("Cuentas", Icons.Default.AccountBalanceWallet),
    Card("Tarjeta", Icons.Default.CreditCard),
    Activity("Movimientos", Icons.Default.Payments)
}

private enum class TributosSection(val label: String) {
    Overview("Control"),
    Audit("Auditoría")
}

/** Módulo Beta: Escuela de Capacitación GDLP */
private val BONUS_QUIZ_PZ = 5L
private val BONUS_COMPLETE_PZ = 15L
private val SANDBOX_LEDGER = "[BETA_ACADEMIA_2026]"

private val trainingModules = listOf(
    TrainingModule(
        id = TrainingModuleId.Module1,
        title = "Identidad Digital y Economía de La Placeta",
        lessons = listOf(
            "1.1. El Grupo de La Placeta (GDLP) es un entorno de simulación organizativa sin ánimo de lucro ni circulación de dinero real.",
            "1.2. El DIP es tu certificado oficial. La pasarela PlacetaID verifica tu edad antes de acceder a banca, loterías o inversiones. La falsificación acarrea expulsión definitiva.",
            "1.3. Cuentas bancarias por edad: Junior Básica (<16) saldo máx 500 Pz, transferencias 50 Pz/día. Junior Senior (16-17) saldo máx 1.000 Pz. Ciudadana Plena (18+) saldo máx 500.000 Pz. Institucional: 10.000.000 Pz.",
            "1.4. La emisión de Pz está centralizada en la Junta. Límite ordinario: 7.500 Pz/usuario. Emisiones excepcionales: 60% Tesoro, 20% Administración, 20% Banco. La quema destruye circulante por expulsión o cancelación."
        ),
        quiz = listOf(
            TrainingQuestion("¿Qué verifica PlacetaID?", listOf("La edad del integrante", "El saldo bancario", "Las inversiones"), 0),
            TrainingQuestion("¿Cuál es el saldo máximo para una cuenta Junior Básica?", listOf("100 Pz", "500 Pz", "1.000 Pz"), 1),
            TrainingQuestion("¿Qué pasa si falsificas tu DIP?", listOf("Multa de 50 Pz", "Expulsión definitiva", "Cuenta bloqueada 7 días"), 1),
            TrainingQuestion("¿A qué edad se tiene una cuenta Ciudadana Plena?", listOf("16 años", "18 años", "21 años"), 1),
            TrainingQuestion("¿Cuál es el límite de transferencia diaria para Junior Básica?", listOf("25 Pz", "50 Pz", "100 Pz"), 1),
            TrainingQuestion("¿Cuánto Pz se puede emitir por usuario de forma ordinaria?", listOf("5.000 Pz", "7.500 Pz", "10.000 Pz"), 1)
        )
    ),
    TrainingModule(
        id = TrainingModuleId.Module2,
        title = "Marco Impositivo, Laboral y RBU",
        lessons = listOf(
            "2.1. RBU: bono semanal de 5 Pz para activos. Debes reclamarlo manualmente cada semana. No se acumula. Es inembargable.",
            "2.2. SMI: 150 Pz semanales mínimo para contratos laborales. Se revisa semestralmente.",
            "2.3. IVA del 12% en productos/servicios GDLP. Tasas de transferencia interna no superan el 12%. El impago genera intereses por segundo.",
            "2.4. IRM: impuesto progresivo hasta 9% que grava la acumulación inactiva. Cotizaciones laborales: 7,5%-35% entre empresa y trabajador."
        ),
        quiz = listOf(
            TrainingQuestion("¿Cada cuánto debes reclamar la RBU?", listOf("Cada día", "Cada semana", "Cada mes"), 1),
            TrainingQuestion("¿Qué porcentaje de IVA se aplica en el GDLP?", listOf("7%", "10%", "12%"), 2),
            TrainingQuestion("¿Cuál es el SMI semanal en el GDLP?", listOf("100 Pz", "150 Pz", "200 Pz"), 1),
            TrainingQuestion("¿Es embargable la RBU?", listOf("Sí, totalmente", "No, es inembargable", "Solo si se acumula"), 1),
            TrainingQuestion("¿Qué grava el IRM?", listOf("Los ingresos semanales", "La acumulación inactiva", "Las transferencias"), 1),
            TrainingQuestion("¿Qué genera el impago del IVA?", listOf("Aviso por email", "Intereses por segundo", "Bloqueo permanente"), 1)
        )
    ),
    TrainingModule(
        id = TrainingModuleId.Module3,
        title = "Cumplimiento en Protección de Datos (RGPD)",
        lessons = listOf(
            "3.1. El tratamiento de datos reales cumple con RGPD y LOPDGDD. Las organizaciones deben tener política de privacidad, RAT y DPD.",
            "3.2. Derechos ARCO+: acceso, rectificación, supresión, oposición, limitación y portabilidad. Plazo máximo: 30 días.",
            "3.3. Bases legales: consentimiento (loterías, boletines), ejecución de contrato (laboral, bancario), obligación legal (verificación edad, IVA/IRM, sanciones).",
            "3.4. Menores: <14 requiere tutor. 14-17 pueden consentir con notificación al tutor. Brechas: contención inmediata, notificación a Junta <24h, a AEPD <72h, informe final 15 días."
        ),
        quiz = listOf(
            TrainingQuestion("¿Cuál es el plazo máximo para ejercer derechos ARCO+?", listOf("15 días", "30 días", "60 días"), 1),
            TrainingQuestion("¿Qué debe hacerse ante una brecha de seguridad?", listOf("Ignorarla", "Notificar a la Junta en 24h", "Esperar 1 semana"), 1),
            TrainingQuestion("¿Cuál NO es un derecho ARCO+?", listOf("Acceso", "Supresión", "Reembolso"), 2),
            TrainingQuestion("¿Desde qué edad puede un menor consentir con notificación al tutor?", listOf("12 años", "14 años", "16 años"), 1),
            TrainingQuestion("¿En qué plazo debe notificarse a la AEPD una brecha?", listOf("24 horas", "72 horas", "7 días"), 1),
            TrainingQuestion("¿Qué documento debe tener una organización según RGPD?", listOf("Plan de negocio", "Política de privacidad", "Certificado bancario"), 1)
        )
    )
)

@Composable
private fun TrainingSchoolScreen(
    activeUser: UserProfile,
    users: List<UserProfile>,
    show: (String) -> Unit,
    apply: (EconomyResult<*>) -> Unit
) {
    val user = users.firstOrNull { it.dip == activeUser.dip } ?: activeUser
    val isJunior = user.birthDate != null && runCatching {
        val age = java.time.Period.between(java.time.LocalDate.parse(user.birthDate), java.time.LocalDate.now()).years
        age < 16
    }.getOrDefault(false)
    var currentModuleIdx by remember { mutableStateOf(-1) }
    var showQuiz by remember { mutableStateOf(false) }
    var quizResults by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var quizSubmitted by remember { mutableStateOf(false) }
    var completedModules by remember { mutableStateOf(setOf<TrainingModuleId>()) }
    var rewards by remember { mutableStateOf(0L) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E), contentColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                    Column {
                        Text("Escuela de Capacitación GDLP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("Módulo Beta · Bonificaciones en sandbox", color = Color(0xFFB0B0FF), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (rewards > 0) {
                    Text("Bonificación acumulada: $rewards Pz [$SANDBOX_LEDGER]", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (currentModuleIdx == -1) {
            trainingModules.forEachIndexed { idx, mod ->
                val done = mod.id in completedModules
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { currentModuleIdx = idx },
                    colors = CardDefaults.cardColors(containerColor = if (done) Color(0xFF1B3A1B) else Color.White, contentColor = if (done) Color.White else MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(mod.title, fontWeight = FontWeight.Black)
                            Text(if (done) "Completado · ${BONUS_QUIZ_PZ} Pz" else "${mod.quiz.size} preguntas · ${BONUS_QUIZ_PZ} Pz por acierto", color = if (done) Color(0xFF00E676) else Color(0xFF6C5878))
                        }
                        Icon(if (done) Icons.Default.CheckCircle else Icons.Default.Bolt, contentDescription = null, tint = if (done) Color(0xFF00E676) else PremiumPurple)
                    }
                }
            }
            if (completedModules.size == trainingModules.size) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color(0xFFFFD700))
                        Column(Modifier.weight(1f)) {
                            Text("¡Formación completa!", fontWeight = FontWeight.Black)
                            Text("Bonificación total: ${rewards}Pz · Recompensa especial próx. Disponible", color = Color(0xFF6C5878))
                        }
                    }
                }
            }
        } else {
            val mod = trainingModules[currentModuleIdx]
            if (!showQuiz) {
                Text(mod.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                mod.lessons.forEach { lesson ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F3FF)), shape = RoundedCornerShape(14.dp)) {
                        Text(lesson, modifier = Modifier.padding(14.dp), color = Color(0xFF2C2C3A))
                    }
                }
                Button(
                    onClick = {
                        if (isJunior && currentModuleIdx in listOf(0, 2)) {
                            show("Los módulos 1 y 3 contienen información restringida para tu franja de edad.")
                            return@Button
                        }
                        showQuiz = true
                        quizResults = mod.quiz.indices.associateWith { -1 }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Comenzar cuestionario", fontWeight = FontWeight.Black) }
                TextButton(onClick = { currentModuleIdx = -1; showQuiz = false }) { Text("Volver", color = PremiumPurple) }
            } else {
                Text("Cuestionario: ${mod.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                mod.quiz.forEachIndexed { qIdx, question ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F3FF)), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${qIdx + 1}. ${question.question}", fontWeight = FontWeight.Bold)
                            question.options.forEachIndexed { oIdx, option ->
                                val selected = quizResults[qIdx] == oIdx
                                Row(
                                    Modifier.fillMaxWidth().clickable { if (!quizSubmitted) quizResults = quizResults + (qIdx to oIdx) }.background(if (selected) SurfacePurple else Color.Transparent, RoundedCornerShape(10.dp)).padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.Add, contentDescription = null, tint = if (selected) PremiumPurple else Color(0xFF6C5878), modifier = Modifier.size(20.dp))
                                    Text(option, color = Ink)
                                }
                            }
                            if (quizSubmitted) {
                                val correct = question.correctIndex
                                Text(if (quizResults[qIdx] == correct) "✓ Correcto" else "✗ Incorrecto (respuesta: ${question.options[correct]})",
                                    color = if (quizResults[qIdx] == correct) IncomeGreen else ErrorRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (!quizSubmitted) {
                    Button(
                        onClick = {
                            val answers = quizResults.values
                            if (answers.any { it == -1 }) { show("Responde todas las preguntas antes de enviar."); return@Button }
                            quizSubmitted = true
                            val correct = mod.quiz.indices.count { quizResults[it] == mod.quiz[it].correctIndex }
                            val score = (correct * 100) / mod.quiz.size
                            if (score == 100 && mod.id !in completedModules) {
                                completedModules = completedModules + mod.id
                                rewards += BONUS_QUIZ_PZ
                                show("✅ Cuestionario superado: +${BONUS_QUIZ_PZ} Pz a [$SANDBOX_LEDGER]")
                                if (completedModules.size == trainingModules.size) {
                                    rewards += BONUS_COMPLETE_PZ
                                    show("🏆 ¡Módulos completados! +${BONUS_COMPLETE_PZ} Pz extra. Recompensa pendiente próx. disponible.")
                                }
                            } else {
                                show("Calificación: $score%. Debes acertar todo para obtener la bonificación.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Enviar respuestas", fontWeight = FontWeight.Black) }
                } else {
                    Button(
                        onClick = { currentModuleIdx = -1; showQuiz = false; quizSubmitted = false; quizResults = emptyMap() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B69)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Volver a módulos", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

private fun transactionsForCapitaliaApprovals(transactions: List<LedgerTransaction>): List<LedgerTransaction> =
    transactions.filter { it.status == TransactionStatus.Pending && it.concept == "CAPITALIA_PARENT_APPROVAL" }

private fun String.toGdlpIbanInput(): String {
    val clean = uppercase().filter { it.isLetterOrDigit() }.removePrefix("GDLP")
    if (clean.startsWith("AP")) {
        val digits = clean.removePrefix("AP").filter(Char::isDigit).take(5)
        val control = digits.take(2)
        val body = digits.drop(2).take(3)
        return buildString {
            append("GDLP-AP")
            append(control)
            if (body.isNotEmpty()) append("-").append(body)
        }.take(13)
    }
    val digits = clean.filter(Char::isDigit).take(8)
    val control = digits.take(4)
    val body = digits.drop(4).take(4)
    return buildString {
        append("GDLP-")
        append(control)
        if (body.isNotEmpty()) append("-").append(body)
    }.take(14)
}

private enum class AppTab(val title: String, val icon: ImageVector) {
    Home("Cartera", Icons.Default.Home),
    Placezum("Placezum", Icons.Default.Contactless),
    Inversiones("Inversiones", Icons.Default.ShowChart),
    Sociedades("Sociedades", Icons.Default.AccountBalance),
    Estatal("Estado", Icons.Default.AssuredWorkload),
    Hub("Más", Icons.Default.MoreHoriz),
    Tributos("TGLP", Icons.Default.AssuredWorkload),
    Escuela("Escuela", Icons.Default.Info);

    companion object {
        fun fromKey(key: String): AppTab = entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Home
    }
}

// ── Tributos Censo Popup (bloqueante, no descartable) ─────────────────────
@Composable
private fun TributosCensoPopup(
    accounts: List<Account>,
    activeUser: UserProfile,
    onRegister: (eip: String?) -> Unit
) {
    val isBusiness = accounts.any { it.placetaId == activeUser.placetaId && it.type == AccountType.Business }
    var eipValue by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isBusiness) "Vincula tu EIP empresarial" else "Censo obligatorio de Tributos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C005F)
                )

                Text(
                    text = if (isBusiness)
                        "Tu empresa ya está registrada en Tributos de La Placeta. Introduce tu EIP (Entidad Identificada de La Placeta) para vincularla a tu cuenta bancaria."
                    else
                        "Para operar con tu cuenta bancaria, es necesario que te des de alta en el censo de contribuyentes de Tributos de La Placeta (TLP).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                if (isBusiness) {
                    OutlinedTextField(
                        value = eipValue,
                        onValueChange = { eipValue -> eipValue.take(12).uppercase() },
                        label = { Text("EIP (EIP-XXXXXX)") },
                        placeholder = { Text("EIP-XXXXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1C005F),
                            unfocusedBorderColor = Color(0xFFD4C8F0)
                        )
                    )
                }

                Button(
                    onClick = {
                        loading = true
                        onRegister(if (isBusiness) eipValue.takeIf { it.isNotBlank() } else null)
                    },
                    enabled = !loading && (!isBusiness || eipValue.isNotBlank()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F00D8))
                ) {
                    Text(
                        if (loading) "Registrando…" else if (isBusiness) "🔗 Vincular EIP y continuar" else "✅ Darme de alta ahora",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
