package com.placeta.banco.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.placeta.banco.BuildConfig
import com.placeta.banco.core.Account
import com.placeta.banco.core.AccountKind
import com.placeta.banco.core.AccountType
import com.placeta.banco.core.AGLDP_ID
import com.placeta.banco.core.CAPITALIA_BANK_ID
import com.placeta.banco.core.CitizenshipTier
import com.placeta.banco.core.ComplianceFlag
import com.placeta.banco.core.ComplianceStatus
import com.placeta.banco.core.DipIdentity
import com.placeta.banco.core.DigitalCard
import com.placeta.banco.core.DonationReward
import com.placeta.banco.core.DonationRewardDestination
import com.placeta.banco.core.DonationRewardStatus
import com.placeta.banco.core.EconomyResult
import com.placeta.banco.core.FOUNDATION_RBU_ID
import com.placeta.banco.core.IbanGdlp
import com.placeta.banco.core.InvestmentHolding
import com.placeta.banco.core.InvestmentOperation
import com.placeta.banco.core.LedgerDebt
import com.placeta.banco.core.LedgerDebtStatus
import com.placeta.banco.core.LedgerDebtType
import com.placeta.banco.core.LedgerTransaction
import com.placeta.banco.core.MemberTier
import com.placeta.banco.core.PromoAction
import com.placeta.banco.core.PromoSlide
import com.placeta.banco.core.Role
import com.placeta.banco.core.SavedContact
import com.placeta.banco.core.SubsidyRequest
import com.placeta.banco.core.TGLP_ID
import com.placeta.banco.core.TreasuryConfig
import com.placeta.banco.core.TransactionKind
import com.placeta.banco.core.TransactionStatus
import com.placeta.banco.core.UserProfile
import com.placeta.banco.core.PayrollContract
import com.placeta.banco.core.PayrollPeriod
import com.placeta.banco.core.PayrollFrequency
import com.placeta.banco.core.PayrollContractStatus
import com.placeta.banco.core.PayrollPeriodStatus
import com.placeta.banco.core.PayrollSalaryChange
import com.placeta.banco.core.AccountHolder
import com.placeta.banco.core.ProductContractTemplate
import com.placeta.banco.core.SignedProductContract
import com.placeta.banco.core.SupportTicket
import com.placeta.banco.core.SupportTicketResponse
import com.placeta.banco.core.UserModulePreferences
import com.placeta.banco.core.GuardianRenewalDecision
import com.placeta.banco.core.ExecutionCode
import com.placeta.banco.core.VAULT_ADMIN_GENERAL
import com.placeta.banco.core.VAULT_EMISION
import com.placeta.banco.core.VAULT_TRIBUTOS_IVA
import com.placeta.banco.notifications.AppNotificationState
import com.placeta.banco.work.BankWorkScheduler
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

enum class ConnectionState { Loading, Online, Offline }

private const val SESSION_PREFS = "placeta_session"
private const val KEY_ACTIVE_DIP = "active_dip"

interface BankRepository {
    val users: State<List<UserProfile>>
    val activeUser: State<UserProfile?>
    val accounts: State<Map<String, Account>>
    val transactions: State<List<LedgerTransaction>>
    val ledgerDebts: State<List<LedgerDebt>>
    val subsidyRequests: State<List<SubsidyRequest>>
    val investmentHoldings: State<List<InvestmentHolding>>
    val investmentOperations: State<List<InvestmentOperation>>
    val digitalCards: State<List<DigitalCard>>
    val savedContacts: State<List<SavedContact>>
    val donationRewards: State<List<DonationReward>>
    val promoSlides: State<List<PromoSlide>>
    val treasuryConfig: State<TreasuryConfig>
    val complianceFlags: State<List<ComplianceFlag>>
    val connection: State<ConnectionState>
    val connectionMessage: State<String>

    fun refresh()
    fun run(result: EconomyResult<*>): String?
    fun upsertAccount(account: Account)
    fun upsertRequest(request: SubsidyRequest)
    fun denyRequest(request: SubsidyRequest)
    fun upsertInvestmentHolding(holding: InvestmentHolding)
    fun upsertInvestmentOperation(operation: InvestmentOperation)
    fun upsertDigitalCard(card: DigitalCard)
    fun upsertSavedContact(contact: SavedContact)
    fun upsertDonationReward(reward: DonationReward)
    fun upsertPromoSlide(slide: PromoSlide)
    fun updateTreasuryConfig(config: TreasuryConfig)
    fun upsertComplianceFlag(flag: ComplianceFlag)
    fun upsertLedgerDebt(debt: LedgerDebt)
    fun upsertUserModulePreferences(prefs: UserModulePreferences)
    fun upsertPayrollContract(contract: PayrollContract)
    fun upsertPayrollPeriod(period: PayrollPeriod)
    fun upsertAccountHolder(holder: AccountHolder)
    fun removeAccountHolder(holderId: String)
    fun upsertSignedContract(contract: SignedProductContract)
    fun createPaymentLink(kind: String, accountId: String, amount: Long, concept: String, callback: (url: String?, error: String?) -> Unit)
    val payrollContracts: State<List<PayrollContract>>
    val payrollPeriods: State<List<PayrollPeriod>>
    val userModulePreferences: State<List<UserModulePreferences>>
    val productContractTemplates: State<List<ProductContractTemplate>>
    val signedProductContracts: State<List<SignedProductContract>>
    val accountHolders: State<List<AccountHolder>>
    val guardianRenewalDecisions: State<List<GuardianRenewalDecision>>
    val executionCodes: State<List<ExecutionCode>>
    val supportTickets: State<List<SupportTicket>>

    fun login(dip: String, pin: String): String?
    fun register(displayName: String, dip: String, pin: String): String?
    fun loginWithPlacetaId(token: String, userJson: String): String?
    fun registerTributosContribuyente(eip: String? = null)
    fun logout()
    fun resetConnectionState()
    fun createSupportTicket(ticket: SupportTicket)
}

class FirebaseBankRepository(
    private val context: Context? = null
) : BankRepository {
    private val mongoBaseUrl = BuildConfig.PLACETA_API_BASE_URL.trimEnd('/')
    private val mongoStateUrl = "$mongoBaseUrl/api/state"
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private var lastRemoteRefreshAt: Instant = Instant.EPOCH
    private var localWriteInFlightUntil: Instant = Instant.EPOCH
    private var lastLocalWriteAt: Instant = Instant.EPOCH
    private var pendingLocalTransactionIds: Set<String> = emptySet()
    private var pendingPromoSignature: String? = null
    private var forceNextRemoteRefresh = true
    private val remoteRefreshIntervalSeconds = 5L
    private val pollExecutor = Executors.newSingleThreadScheduledExecutor()
    private val connectTimeoutMs = 15_000
    private val readTimeoutMs = 25_000

    override val users = mutableStateOf<List<UserProfile>>(emptyList())
    override val activeUser = mutableStateOf<UserProfile?>(null)
    override val accounts = mutableStateOf<Map<String, Account>>(emptyMap())
    override val transactions = mutableStateOf<List<LedgerTransaction>>(emptyList())
    override val ledgerDebts = mutableStateOf<List<LedgerDebt>>(emptyList())
    override val subsidyRequests = mutableStateOf<List<SubsidyRequest>>(emptyList())
    override val investmentHoldings = mutableStateOf<List<InvestmentHolding>>(emptyList())
    override val investmentOperations = mutableStateOf<List<InvestmentOperation>>(emptyList())
    override val digitalCards = mutableStateOf<List<DigitalCard>>(emptyList())
    override val savedContacts = mutableStateOf<List<SavedContact>>(emptyList())
    override val donationRewards = mutableStateOf<List<DonationReward>>(emptyList())
    override val promoSlides = mutableStateOf<List<PromoSlide>>(defaultPromoSlides())
    override val treasuryConfig = mutableStateOf(TreasuryConfig())
    override val complianceFlags = mutableStateOf<List<ComplianceFlag>>(emptyList())
    override val connection = mutableStateOf(ConnectionState.Loading)
    override val connectionMessage = mutableStateOf("Conectando con la base de datos...")

    override val payrollContracts = mutableStateOf<List<PayrollContract>>(emptyList())
    override val payrollPeriods = mutableStateOf<List<PayrollPeriod>>(emptyList())
    override val userModulePreferences = mutableStateOf<List<UserModulePreferences>>(emptyList())
    override val productContractTemplates = mutableStateOf<List<ProductContractTemplate>>(emptyList())
    override val signedProductContracts = mutableStateOf<List<SignedProductContract>>(emptyList())
    override val accountHolders = mutableStateOf<List<AccountHolder>>(emptyList())
    override val guardianRenewalDecisions = mutableStateOf<List<GuardianRenewalDecision>>(emptyList())
    override val executionCodes = mutableStateOf<List<ExecutionCode>>(emptyList())
    override val supportTickets = mutableStateOf<List<SupportTicket>>(emptyList())

    init {
        connect()
        startSilentPolling()
    }

    override fun refresh() {
        forceNextRemoteRefresh = true
        connect()
    }

    private fun connect() {
        if (connection.value != ConnectionState.Online) {
            connection.value = ConnectionState.Loading
            connectionMessage.value = "Conectando con la base de datos..."
        }
        listenToBankState()
    }

    private fun listenToBankState() {
        ioExecutor.execute {
            val result = runCatching { mongoGetState() }
            result.onSuccess { remote ->
                if (remote == null) {
                    if (accounts.value.isNotEmpty()) {
                        connection.value = ConnectionState.Online
                        connectionMessage.value = "Base de datos sin estado remoto; publicando estado local"
                        persistCurrent()
                    } else {
                        seedFirebase()
                    }
                    return@onSuccess
                }
                applyRemoteState(remote)
            }.onFailure {
                connection.value = ConnectionState.Offline
                connectionMessage.value = "Base de datos no disponible: ${it.message}"
            }
        }
    }

    private fun startSilentPolling() {
        pollExecutor.scheduleWithFixedDelay(
            { refreshSilently() },
            remoteRefreshIntervalSeconds,
            remoteRefreshIntervalSeconds,
            TimeUnit.SECONDS
        )
    }

    private fun refreshSilently() {
        ioExecutor.execute {
            runCatching { mongoGetState() }
                .onSuccess { remote ->
                    if (remote != null) {
                        applyRemoteState(remote)
                    } else if (accounts.value.isEmpty()) {
                        seedFirebase()
                    }
                }
                .onFailure {
                    if (accounts.value.isEmpty()) {
                        connection.value = ConnectionState.Offline
                        connectionMessage.value = "Base de datos no disponible: ${it.message}"
                    }
                }
        }
    }

    private fun shouldApplyRemoteSnapshot(hasPendingWrites: Boolean): Boolean {
        if (hasPendingWrites) return false
        if (accounts.value.isNotEmpty() && Instant.now().isBefore(localWriteInFlightUntil)) return false
        if (forceNextRemoteRefresh || accounts.value.isEmpty()) {
            forceNextRemoteRefresh = false
            lastRemoteRefreshAt = Instant.now()
            return true
        }
        val now = Instant.now()
        if (now.epochSecond - lastRemoteRefreshAt.epochSecond < remoteRefreshIntervalSeconds) {
            return false
        }
        lastRemoteRefreshAt = now
        return true
    }

    private fun applyRemoteState(remote: Map<String, Any?>) {
        if (!shouldApplyRemoteSnapshot(pendingLocalTransactionIds.isNotEmpty() || pendingPromoSignature != null)) return
        var nextAccounts = remote.list("accounts").mapNotNull { it.toAccountOrNull() }.associateBy { it.id }
        val nextPromos = remote.list("promoSlides").mapNotNull { it.toPromoSlideOrNull() }.sortedBy { it.id }
        if (nextPromos.isNotEmpty()) {
            promoSlides.value = nextPromos
            pendingPromoSignature = null
        }
        val nextTransactions = remote.list("transactions").mapNotNull { it.toTransactionOrNull() }.sortedByDescending { it.createdAt }
        val remoteTransactionIds = nextTransactions.map { it.id }.toSet()
        if (pendingLocalTransactionIds.isNotEmpty() && !remoteTransactionIds.containsAll(pendingLocalTransactionIds)) {
            connectionMessage.value = "Base de datos pendiente de confirmar movimientos locales"
            return
        }
        pendingLocalTransactionIds = emptySet()
        if (transactions.value.isNotEmpty() && nextTransactions.size < transactions.value.size) return

        val seedFallback = demoSeed()
        if (!nextAccounts.containsKey(FOUNDATION_RBU_ID)) {
            seedFallback.accounts[FOUNDATION_RBU_ID]?.let { nextAccounts = nextAccounts + (FOUNDATION_RBU_ID to it) }
        }
        if (!nextAccounts.containsKey(CAPITALIA_BANK_ID)) {
            seedFallback.accounts[CAPITALIA_BANK_ID]?.let { nextAccounts = nextAccounts + (CAPITALIA_BANK_ID to it) }
        }
        if (!nextAccounts.containsKey(VAULT_EMISION)) {
            seedFallback.accounts[VAULT_EMISION]?.let { nextAccounts = nextAccounts + (VAULT_EMISION to it) }
        }
        val missingMarketAccounts = seedFallback.accounts
            .filterKeys { it.startsWith("biz-market-") }
            .filterKeys { !nextAccounts.containsKey(it) }
        if (missingMarketAccounts.isNotEmpty()) nextAccounts = nextAccounts + missingMarketAccounts
        if (nextAccounts.isEmpty() && accounts.value.isNotEmpty()) return

        users.value = remote.list("users").mapNotNull { it.toUserProfileOrNull() }.sortedBy { it.displayName }.ifEmpty { users.value }
        accounts.value = nextAccounts
        transactions.value = mergeTransactions(transactions.value, nextTransactions)
        val nextLedgerDebts = remote.list("ledgerDebts")
            .mapNotNull { value: Any? -> value.toLedgerDebtOrNull() }
            .sortedByDescending { debt: LedgerDebt -> debt.createdAt }
        ledgerDebts.value = mergeDebts(ledgerDebts.value, nextLedgerDebts)
        subsidyRequests.value = remote.list("subsidyRequests").mapNotNull { it.toRequestOrNull() }.sortedByDescending { it.createdAt }
        investmentHoldings.value = remote.list("investmentHoldings").mapNotNull { it.toHoldingOrNull() }.sortedBy { it.assetName }
        investmentOperations.value = remote.list("investmentOperations").mapNotNull { it.toInvestmentOperationOrNull() }.sortedByDescending { it.createdAt }
        digitalCards.value = remote.list("digitalCards").mapNotNull { it.toDigitalCardOrNull() }.sortedBy { it.alias }
        savedContacts.value = remote.list("savedContacts").mapNotNull { it.toSavedContactOrNull() }.sortedByDescending { it.createdAt }
        donationRewards.value = remote.list("donationRewards").mapNotNull { it.toDonationRewardOrNull() }.sortedByDescending { it.updatedAt }
        promoSlides.value = nextPromos.ifEmpty { promoSlides.value.ifEmpty { defaultPromoSlides() } }
        treasuryConfig.value = remote["treasuryConfig"].toTreasuryConfigOrNull() ?: TreasuryConfig()
        complianceFlags.value = remote.list("complianceFlags").mapNotNull { it.toComplianceFlagOrNull() }.sortedByDescending { it.createdAt }
        connection.value = ConnectionState.Online
        connectionMessage.value = "Base de datos conectada"
        restoreSavedSession()
        applyDailySavingsInterestIfDue()
    }

    private fun applyDailySavingsInterestIfDue() {
        val today = LocalDate.now().toString()
        if (connection.value != ConnectionState.Online || accounts.value.isEmpty()) return
        if (treasuryConfig.value.lastSavingsInterestDate == today) return
        val result = com.placeta.banco.core.EconomyEngine(treasuryConfig.value).accrueDailySavingsInterest(accounts.value)
        if (result is EconomyResult.Success) {
            val nextTransactions = mergeTransactions(transactions.value, result.transactions)
            val nextDebts = mergeDebts(ledgerDebts.value, result.debts)
            val nextConfig = treasuryConfig.value.copy(lastSavingsInterestDate = today).normalized()
            accounts.value = result.accounts
            transactions.value = nextTransactions
            ledgerDebts.value = nextDebts
            treasuryConfig.value = nextConfig
            persistCurrent(nextAccounts = result.accounts, nextTransactions = nextTransactions, nextDebts = nextDebts, nextConfig = nextConfig)
        } else {
            treasuryConfig.value = treasuryConfig.value.copy(lastSavingsInterestDate = today).normalized()
            persistCurrent(nextConfig = treasuryConfig.value)
        }
    }

    override fun run(result: EconomyResult<*>): String? {
        return when (result) {
            is EconomyResult.Failure -> result.message
            is EconomyResult.Success -> {
                val nextAccounts = if (result.accounts.isNotEmpty()) result.accounts else accounts.value
                val nextTransactions = if (result.transactions.isNotEmpty()) {
                    mergeTransactions(transactions.value, result.transactions)
                } else {
                    transactions.value
                }
                val nextDebts = if (result.debts.isNotEmpty()) mergeDebts(ledgerDebts.value, result.debts) else ledgerDebts.value
                accounts.value = nextAccounts
                transactions.value = nextTransactions
                ledgerDebts.value = nextDebts
                complianceFlags.value = buildAuditFlags(nextAccounts, nextTransactions, complianceFlags.value, treasuryConfig.value)
                persistCurrent(nextAccounts = nextAccounts, nextTransactions = nextTransactions, nextDebts = nextDebts, nextFlags = complianceFlags.value)
                if (connection.value == ConnectionState.Online) null else "Operación guardada en este dispositivo; se sincronizará al reconectar"
            }
        }
    }

    override fun upsertAccount(account: Account) {
        val next = accounts.value + (account.id to account)
        accounts.value = next
        complianceFlags.value = buildAuditFlags(next, transactions.value, complianceFlags.value, treasuryConfig.value)
        persistCurrent(nextAccounts = next, nextFlags = complianceFlags.value)
    }

    override fun upsertRequest(request: SubsidyRequest) {
        val next = subsidyRequests.value.filterNot { it.id == request.id } + request
        subsidyRequests.value = next.sortedByDescending { it.createdAt }
        persistCurrent(nextRequests = next)
    }

    override fun denyRequest(request: SubsidyRequest) {
        upsertRequest(request.copy(status = TransactionStatus.Denied))
    }

    override fun upsertInvestmentHolding(holding: InvestmentHolding) {
        val next = (investmentHoldings.value.filterNot { it.id == holding.id } + holding)
            .filter { it.units > 0 || it.currentValuePz > 0 }
            .sortedBy { it.assetName }
        investmentHoldings.value = next
        persistCurrent(nextHoldings = next)
    }

    override fun upsertInvestmentOperation(operation: InvestmentOperation) {
        val next = (investmentOperations.value.filterNot { it.id == operation.id } + operation)
            .sortedByDescending { it.createdAt }
        investmentOperations.value = next
        persistCurrent(nextOperations = next)
    }

    override fun upsertDigitalCard(card: DigitalCard) {
        val next = (digitalCards.value.filterNot { it.id == card.id } + card).sortedBy { it.alias }
        digitalCards.value = next
        persistCurrent(nextCards = next)
    }

    override fun upsertSavedContact(contact: SavedContact) {
        val next = (savedContacts.value.filterNot { it.id == contact.id || (it.ownerPlacetaId == contact.ownerPlacetaId && it.accountId == contact.accountId) } + contact)
            .sortedByDescending { it.createdAt }
        savedContacts.value = next
        persistCurrent(nextContacts = next)
    }

    override fun upsertDonationReward(reward: DonationReward) {
        val next = (donationRewards.value.filterNot { it.id == reward.id } + reward).sortedByDescending { it.updatedAt }
        donationRewards.value = next
        persistCurrent(nextDonationRewards = next)
    }

    override fun upsertPromoSlide(slide: PromoSlide) {
        Log.d("BancoPlacetaPromos", "Guardar promo pulsado id=${slide.id} title=${slide.title} imageKey=${slide.imageKey} asset=${slide.assetPath} url=${slide.imageUrl}")
        val base = promoSlides.value.ifEmpty { defaultPromoSlides() }
        val next = (base.filterNot { it.id == slide.id } + slide).sortedBy { it.id }
        promoSlides.value = next
        lastLocalWriteAt = Instant.now()
        localWriteInFlightUntil = lastLocalWriteAt.plusSeconds(20)
        pendingPromoSignature = next.promoSignature()
        val promoPayload = bankStatePayload(
            nextUsers = users.value,
            nextAccounts = accounts.value,
            nextTransactions = transactions.value,
            nextDebts = ledgerDebts.value,
            nextRequests = subsidyRequests.value,
            nextHoldings = investmentHoldings.value,
            nextOperations = investmentOperations.value,
            nextCards = digitalCards.value,
            nextContacts = savedContacts.value,
            nextDonationRewards = donationRewards.value,
            nextPromos = next,
            nextConfig = treasuryConfig.value,
            nextFlags = complianceFlags.value,
            nextSignedContracts = signedProductContracts.value,
            nextAccountHolders = accountHolders.value,
            nextPayrollContracts = payrollContracts.value,
            nextPayrollPeriods = payrollPeriods.value,
            nextModulePrefs = userModulePreferences.value,
            updatedAt = lastLocalWriteAt
        )
        ioExecutor.execute {
            runCatching {
                retryRemote {
                    mongoPutState(promoPayload)
                    val confirmedPromos = mongoGetState()?.list("promoSlides")?.mapNotNull { it.toPromoSlideOrNull() }?.sortedBy { it.id }.orEmpty()
                    if (confirmedPromos.isEmpty()) {
                        mongoGetEntityCollection("promoSlides").mapNotNull { it.toPromoSlideOrNull() }.sortedBy { it.id }
                    } else {
                        confirmedPromos
                    }
                }.also { confirmedPromos ->
                if (confirmedPromos.none { it.id == slide.id && it == slide }) {
                    error("La base de datos no devolvió la promo editada")
                }
                promoSlides.value = confirmedPromos
                }
                Log.d("BancoPlacetaPromos", "Promo confirmada en Mongo id=${slide.id}")
            }.onSuccess {
                pendingPromoSignature = null
                forceNextRemoteRefresh = true
                connection.value = ConnectionState.Online
                connectionMessage.value = "Promo guardada y confirmada en la base de datos"
            }.onFailure {
                Log.e("BancoPlacetaPromos", "No se pudo guardar promo ${slide.id}", it)
                connection.value = ConnectionState.Offline
                connectionMessage.value = "No se pudo guardar la promo en la base de datos: ${it.message}"
            }
        }
    }

    override fun updateTreasuryConfig(config: TreasuryConfig) {
        treasuryConfig.value = config.normalized()
        persistCurrent(nextConfig = treasuryConfig.value)
    }

    override fun upsertComplianceFlag(flag: ComplianceFlag) {
        val next = (complianceFlags.value.filterNot { it.id == flag.id } + flag).sortedByDescending { it.createdAt }
        complianceFlags.value = next
        persistCurrent(nextFlags = next)
    }

    override fun upsertUserModulePreferences(prefs: UserModulePreferences) {
        val next = userModulePreferences.value.toMutableList()
        val idx = next.indexOfFirst { it.placetaId == prefs.placetaId }
        if (idx >= 0) next[idx] = prefs else next.add(prefs)
        userModulePreferences.value = next
    }

    override fun upsertPayrollContract(contract: PayrollContract) {
        val next = payrollContracts.value.toMutableList()
        val idx = next.indexOfFirst { it.id == contract.id }
        if (idx >= 0) next[idx] = contract else next.add(contract)
        payrollContracts.value = next
    }

    override fun upsertPayrollPeriod(period: PayrollPeriod) {
        val next = payrollPeriods.value.toMutableList()
        val idx = next.indexOfFirst { it.id == period.id }
        if (idx >= 0) next[idx] = period else next.add(period)
        payrollPeriods.value = next
    }

    override fun createPaymentLink(kind: String, accountId: String, amount: Long, concept: String, callback: (url: String?, error: String?) -> Unit) {
        callback(null, "createPaymentLink no implementado offline")
    }

    override fun upsertAccountHolder(holder: AccountHolder) {
        val next = accountHolders.value.toMutableList()
        val idx = next.indexOfFirst { it.id == holder.id }
        if (idx >= 0) next[idx] = holder else next.add(holder)
        accountHolders.value = next
    }

    override fun removeAccountHolder(holderId: String) {
        accountHolders.value = accountHolders.value.filter { it.id != holderId }
    }

    override fun upsertSignedContract(contract: SignedProductContract) {
        val next = signedProductContracts.value.toMutableList()
        val idx = next.indexOfFirst { it.id == contract.id }
        if (idx >= 0) next[idx] = contract else next.add(contract)
        signedProductContracts.value = next
        persistCurrent()
    }

    override fun createSupportTicket(ticket: SupportTicket) {
        val next = supportTickets.value.toMutableList()
        next.add(0, ticket)
        supportTickets.value = next
        persistCurrent()
    }

    override fun upsertLedgerDebt(debt: LedgerDebt) {
        val next = mergeDebts(ledgerDebts.value, listOf(debt))
        ledgerDebts.value = next
        persistCurrent(nextDebts = next)
    }

    override fun login(dip: String, pin: String): String? {
        val normalizedDip = dip.trim().uppercase()
        if (normalizedDip == "DIP-A001" && users.value.none { it.dip == normalizedDip }) {
            seedFirebase()
        }
        val user = users.value.firstOrNull { it.dip == normalizedDip } ?: return "DIP no registrado"
        if (user.pinHash != pin.sha256()) return "PIN incorrecto"
        if (!accounts.value.containsKey(user.primaryAccountId)) return "El DIP no tiene cuenta bancaria activa"
        activeUser.value = user
        context?.let {
            saveSessionDip(user.dip)
            AppNotificationState.saveAccountCookie(it, user, accounts.value)
            BankWorkScheduler.checkNow(it)
        }
        return null
    }

    override fun loginWithPlacetaId(token: String, userJson: String): String? {
        val json = runCatching { org.json.JSONObject(userJson) }.getOrNull() ?: return "Error al leer datos de PlacetaID"
        val dip = json.optString("dip").trim().uppercase().ifBlank {
            json.optString("sub").trim().uppercase().ifBlank {
                return "PlacetaID no incluyó DIP en la respuesta"
            }
        }
        val user = users.value.firstOrNull { it.dip == dip }
            ?: runCatching {
                val placetaId = json.optString("placetaId").ifBlank { json.optString("sub") }
                users.value.firstOrNull { it.placetaId == placetaId }
            }.getOrNull()
            ?: return "DIP $dip no registrado en el banco. Debes darte de alta primero."
        if (!accounts.value.containsKey(user.primaryAccountId)) return "El DIP no tiene cuenta bancaria activa"
        activeUser.value = user
        runCatching {
            savePlacetaApiToken(context, token)
        }
        context?.let {
            saveSessionDip(user.dip)
            AppNotificationState.saveAccountCookie(it, user, accounts.value)
            BankWorkScheduler.checkNow(it)
        }
        connection.value = ConnectionState.Online
        return null
    }

    override fun registerTributosContribuyente(eip: String?) {
        val user = activeUser.value ?: return
        val now = Instant.now().toString()
        val nextUsers = users.value.map { u ->
            if (u.dip == user.dip) u.copy(tributosCensusDate = now, eip = eip?.takeIf { it.isNotBlank() } ?: u.eip)
            else u
        }
        val nextAccounts = accounts.value.mapValues { (_, a) ->
            if (a.placetaId == user.placetaId) a.copy(complianceStatus = ComplianceStatus.Clear)
            else a
        }
        users.value = nextUsers
        accounts.value = nextAccounts
        activeUser.value = nextUsers.firstOrNull { it.dip == user.dip }
        persistCurrent(nextUsers = nextUsers, nextAccounts = nextAccounts)
    }

    override fun register(displayName: String, dip: String, pin: String): String? {
        val normalizedDip = dip.trim().uppercase()
        val cleanName = displayName.trim()
        if (cleanName.length < 2) return "Nombre demasiado corto"
        if (!DipIdentity.isOfficialDip(normalizedDip)) return "Formato DIP inválido. Usa DIP-XXXX"
        if (pin.length < 4) return "PIN mínimo de 4 dígitos"
        if (users.value.any { it.dip == normalizedDip }) return "Ese DIP ya existe"

        val placetaId = normalizedDip.removePrefix("DIP-")
        val accountId = "acct-${UUID.randomUUID()}"
        val accountIban = uniqueGdlpIban(accounts.value, accountId)
        val account = Account(
            id = accountId,
            displayName = "Cuenta personal",
            kind = AccountKind.CITIZEN,
            balancePz = 500,
            placetaId = placetaId,
            type = AccountType.Current,
            iban = accountIban,
            citizenshipTier = CitizenshipTier.CiudadaniaPlena
        )
        val user = UserProfile(
            dip = normalizedDip,
            displayName = cleanName,
            placetaId = placetaId,
            pinHash = pin.sha256(),
            primaryAccountId = accountId
        )
        val card = DigitalCard("card-${UUID.randomUUID()}", accountId, "Placeta Black", MemberTier.Standard)
        val agldp = accounts.value[AGLDP_ID]
        val welcome = LedgerTransaction(
            id = "welcome-${UUID.randomUUID()}",
            kind = TransactionKind.WelcomeBonus,
            fromAccountId = AGLDP_ID,
            toAccountId = accountId,
            amountPz = 500,
            note = "Bono de bienvenida Banco Placeta",
            concept = "WELCOME_BONUS",
            IBAN_Origin = agldp?.iban ?: AGLDP_ID
        )
        users.value = (users.value + user).sortedBy { it.displayName }
        accounts.value = accounts.value + mapOf(accountId to account) + if (agldp != null) {
            mapOf(AGLDP_ID to agldp.copy(balancePz = (agldp.balancePz - 500).coerceAtLeast(0)))
        } else {
            emptyMap<String, Account>()
        }
        transactions.value = mergeTransactions(transactions.value, listOf(welcome))
        digitalCards.value = digitalCards.value + card
        activeUser.value = user
        context?.let {
            saveSessionDip(user.dip)
            AppNotificationState.saveAccountCookie(it, user, accounts.value)
            BankWorkScheduler.checkNow(it)
        }
        persistCurrent(nextUsers = users.value, nextAccounts = accounts.value, nextTransactions = transactions.value, nextCards = digitalCards.value)
        return null
    }

    override fun resetConnectionState() {
        connection.value = ConnectionState.Loading
        connectionMessage.value = "Conectando con la base de datos..."
    }

    override fun logout() {
        activeUser.value = null
        context?.let {
            clearSessionDip()
            AppNotificationState.clearAccountCookie(it)
        }
    }

    private fun restoreSavedSession() {
        if (activeUser.value != null) return
        val savedDip = context
            ?.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            ?.getString(KEY_ACTIVE_DIP, null)
            ?.trim()
            ?.uppercase()
            ?: return
        val user = users.value.firstOrNull { it.dip == savedDip } ?: return
        if (!accounts.value.containsKey(user.primaryAccountId)) return
        activeUser.value = user
        context?.let {
            AppNotificationState.saveAccountCookie(it, user, accounts.value)
            BankWorkScheduler.checkNow(it)
        }
    }

    private fun saveSessionDip(dip: String) {
        context
            ?.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_ACTIVE_DIP, dip)
            ?.apply()
    }

    private fun clearSessionDip() {
        context
            ?.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_ACTIVE_DIP)
            ?.apply()
    }

    private fun persistCurrent(
        nextUsers: List<UserProfile> = users.value,
        nextAccounts: Map<String, Account> = accounts.value,
        nextTransactions: List<LedgerTransaction> = transactions.value,
        nextDebts: List<LedgerDebt> = ledgerDebts.value,
        nextRequests: List<SubsidyRequest> = subsidyRequests.value,
        nextHoldings: List<InvestmentHolding> = investmentHoldings.value,
        nextOperations: List<InvestmentOperation> = investmentOperations.value,
        nextCards: List<DigitalCard> = digitalCards.value,
        nextContacts: List<SavedContact> = savedContacts.value,
        nextDonationRewards: List<DonationReward> = donationRewards.value,
        nextPromos: List<PromoSlide> = promoSlides.value,
        nextConfig: TreasuryConfig = treasuryConfig.value,
        nextFlags: List<ComplianceFlag> = complianceFlags.value,
        nextSignedContracts: List<SignedProductContract> = signedProductContracts.value,
        nextAccountHolders: List<AccountHolder> = accountHolders.value,
        nextPayrollContracts: List<PayrollContract> = payrollContracts.value,
        nextPayrollPeriods: List<PayrollPeriod> = payrollPeriods.value,
        nextModulePrefs: List<UserModulePreferences> = userModulePreferences.value
    ) {
        persist(nextUsers, nextAccounts, nextTransactions, nextDebts, nextRequests, nextHoldings, nextOperations, nextCards, nextContacts, nextDonationRewards, nextPromos, nextConfig, nextFlags, nextSignedContracts, nextAccountHolders, nextPayrollContracts, nextPayrollPeriods, nextModulePrefs)
    }

    private fun persist(
        nextUsers: List<UserProfile>,
        nextAccounts: Map<String, Account>,
        nextTransactions: List<LedgerTransaction>,
        nextDebts: List<LedgerDebt>,
        nextRequests: List<SubsidyRequest>,
        nextHoldings: List<InvestmentHolding>,
        nextOperations: List<InvestmentOperation>,
        nextCards: List<DigitalCard>,
        nextContacts: List<SavedContact>,
        nextDonationRewards: List<DonationReward>,
        nextPromos: List<PromoSlide>,
        nextConfig: TreasuryConfig,
        nextFlags: List<ComplianceFlag>,
        nextSignedContracts: List<SignedProductContract>,
        nextAccountHolders: List<AccountHolder>,
        nextPayrollContracts: List<PayrollContract>,
        nextPayrollPeriods: List<PayrollPeriod>,
        nextModulePrefs: List<UserModulePreferences>
    ) {
        lastLocalWriteAt = Instant.now()
        localWriteInFlightUntil = lastLocalWriteAt.plusSeconds(20)
        if (nextTransactions.isNotEmpty()) {
            pendingLocalTransactionIds = nextTransactions.map { it.id }.toSet()
        }
        pendingPromoSignature = nextPromos.promoSignature()
        val payload = bankStatePayload(
            nextUsers = nextUsers,
            nextAccounts = nextAccounts,
            nextTransactions = nextTransactions,
            nextDebts = nextDebts,
            nextRequests = nextRequests,
            nextHoldings = nextHoldings,
            nextOperations = nextOperations,
            nextCards = nextCards,
            nextContacts = nextContacts,
            nextDonationRewards = nextDonationRewards,
            nextPromos = nextPromos,
            nextConfig = nextConfig,
            nextFlags = nextFlags,
            nextSignedContracts = nextSignedContracts,
            nextAccountHolders = nextAccountHolders,
            nextPayrollContracts = nextPayrollContracts,
            nextPayrollPeriods = nextPayrollPeriods,
            nextModulePrefs = nextModulePrefs,
            updatedAt = lastLocalWriteAt
        )
        ioExecutor.execute {
            runCatching { retryRemote { mongoPutState(payload) } }
                .onSuccess {
                    pendingLocalTransactionIds = emptySet()
                    pendingPromoSignature = null
                    connection.value = ConnectionState.Online
                    connectionMessage.value = "Estado guardado en la base de datos"
                }
                .onFailure {
                    connection.value = ConnectionState.Offline
                    connectionMessage.value = "No se pudo guardar en la base de datos: ${it.message}"
                }
        }
    }

    private fun mongoGetState(): Map<String, Any?>? {
        val body = ""
        val connection = (URL(mongoStateUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            useCaches = false
            setRequestProperty("Cache-Control", "no-store")
            addPlacetaApiSignature("GET", "/api/state", body)
        }
        val code = connection.responseCode
        if (code == 404) return null
        if (code !in 200..299) error("HTTP $code ${connection.readResponseBody()}")
        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
        if (responseBody.isBlank()) return null
        return JSONObject(responseBody).toPlainMap()
    }

    private fun mongoGetEntityCollection(collection: String): List<Any?> {
        val body = ""
        val encodedCollection = URLEncoder.encode(collection, Charsets.UTF_8.name())
        val connection = (URL("$mongoBaseUrl/api/entity?collection=$encodedCollection").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            useCaches = false
            setRequestProperty("Cache-Control", "no-store")
            addPlacetaApiSignature("GET", "/api/entity", body)
        }
        val code = connection.responseCode
        if (code !in 200..299) error("HTTP $code ${connection.readResponseBody()}")
        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
        if (responseBody.isBlank()) return emptyList()
        return JSONObject(responseBody).toPlainMap().list("items")
    }

    private fun mongoPutState(payload: Map<String, Any?>) {
        val bodyText = JSONObject(payload.toJsonCompatibleMap()).toString()
        val body = bodyText.toByteArray(Charsets.UTF_8)
        val connection = (URL(mongoStateUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            useCaches = false
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Cache-Control", "no-store")
            addPlacetaApiSignature("PUT", "/api/state", bodyText)
        }
        connection.outputStream.use { it.write(body) }
        val code = connection.responseCode
        if (code !in 200..299) error("HTTP $code ${connection.readResponseBody()}")
    }

    private fun mongoPutStateAsEntities(payload: Map<String, Any?>) {
        val collectionSpecs = listOf(
            "users" to "dip",
            "accounts" to "id",
            "transactions" to "id",
            "ledgerDebts" to "id",
            "subsidyRequests" to "id",
            "investmentHoldings" to "id",
            "investmentOperations" to "id",
            "digitalCards" to "id",
            "savedContacts" to "id",
            "donationRewards" to "id",
            "promoSlides" to "id",
            "complianceFlags" to "id"
        )
        collectionSpecs.forEach { (collection, idKey) ->
            payload.list(collection)
                .mapNotNull { it.toStringKeyMapOrNull() }
                .forEach { item ->
                    val id = item.string(idKey)
                    if (id.isNotBlank()) mongoPutEntity(collection, id, item)
                }
        }
        payload["treasuryConfig"].toStringKeyMapOrNull()?.let {
            mongoPutEntity("treasuryConfig", "treasuryConfig", it)
        }
    }

    private fun mongoPutEntity(collection: String, id: String, payload: Map<String, Any?>) {
        val bodyText = JSONObject(payload.toJsonCompatibleMap()).toString()
        val body = bodyText.toByteArray(Charsets.UTF_8)
        val encodedCollection = URLEncoder.encode(collection, Charsets.UTF_8.name())
        val encodedId = URLEncoder.encode(id, Charsets.UTF_8.name())
        val connection = (URL("$mongoBaseUrl/api/entity?collection=$encodedCollection&id=$encodedId").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            useCaches = false
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Cache-Control", "no-store")
            addPlacetaApiSignature("PUT", "/api/entity", bodyText, context)
        }
        connection.outputStream.use { it.write(body) }
        val code = connection.responseCode
        if (code !in 200..299) error("HTTP $code ${connection.readResponseBody()}")
    }

    private fun HttpURLConnection.readResponseBody(): String =
        runCatching {
            (errorStream ?: inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")

    private fun <T> retryRemote(block: () -> T): T {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                return block()
            } catch (error: Throwable) {
                lastError = error
                if (attempt == 0) Thread.sleep(650)
            }
        }
        throw lastError ?: IllegalStateException("remote_retry_failed")
    }

    private fun HttpURLConnection.addPlacetaApiSignature(method: String, path: String, body: String) {
        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val bodyHash = body.sha256()
        val payload = listOf(method, path, timestamp, nonce, bodyHash).joinToString("\n")
        val signature = hmacSha256Hex(BuildConfig.PLACETA_API_SECRET, payload)
        setRequestProperty("x-placeta-app-id", BuildConfig.PLACETA_API_APP_ID)
        setRequestProperty("x-placeta-timestamp", timestamp)
        setRequestProperty("x-placeta-nonce", nonce)
        setRequestProperty("x-placeta-signature", signature)
    }

    private fun bankStatePayload(
        nextUsers: List<UserProfile>,
        nextAccounts: Map<String, Account>,
        nextTransactions: List<LedgerTransaction>,
        nextDebts: List<LedgerDebt>,
        nextRequests: List<SubsidyRequest>,
        nextHoldings: List<InvestmentHolding>,
        nextOperations: List<InvestmentOperation>,
        nextCards: List<DigitalCard>,
        nextContacts: List<SavedContact>,
        nextDonationRewards: List<DonationReward>,
        nextPromos: List<PromoSlide>,
        nextConfig: TreasuryConfig,
        nextFlags: List<ComplianceFlag>,
        nextSignedContracts: List<SignedProductContract>,
        nextAccountHolders: List<AccountHolder>,
        nextPayrollContracts: List<PayrollContract>,
        nextPayrollPeriods: List<PayrollPeriod>,
        nextModulePrefs: List<UserModulePreferences>,
        updatedAt: Instant
    ) = mapOf(
        "schemaSeedVersion" to 2,
        "users" to nextUsers.map(UserProfile::toFirebaseMap),
        "accounts" to nextAccounts.values.map(Account::toFirebaseMap),
        "transactions" to nextTransactions.map(LedgerTransaction::toFirebaseMap),
        "ledgerDebts" to nextDebts.map(LedgerDebt::toFirebaseMap),
        "subsidyRequests" to nextRequests.map(SubsidyRequest::toFirebaseMap),
        "investmentHoldings" to nextHoldings.map(InvestmentHolding::toFirebaseMap),
        "investmentOperations" to nextOperations.map(InvestmentOperation::toFirebaseMap),
        "digitalCards" to nextCards.map(DigitalCard::toFirebaseMap),
        "savedContacts" to nextContacts.map(SavedContact::toFirebaseMap),
        "donationRewards" to nextDonationRewards.map(DonationReward::toFirebaseMap),
        "promoSlides" to nextPromos.map(PromoSlide::toFirebaseMap),
        "treasuryConfig" to nextConfig.normalized().toFirebaseMap(),
        "complianceFlags" to nextFlags.map(ComplianceFlag::toFirebaseMap),
        "signedProductContracts" to nextSignedContracts.map(SignedProductContract::toFirebaseMap),
        "accountHolders" to nextAccountHolders.map(AccountHolder::toFirebaseMap),
        "payrollContracts" to nextPayrollContracts.map(PayrollContract::toFirebaseMap),
        "payrollPeriods" to nextPayrollPeriods.map(PayrollPeriod::toFirebaseMap),
        "userModulePreferences" to nextModulePrefs.map(UserModulePreferences::toFirebaseMap),
        "supportTickets" to supportTickets.value.map { t ->
            mapOf(
                "id" to t.id, "category" to t.category, "priority" to t.priority,
                "subject" to t.subject, "message" to t.message, "dip" to t.dip,
                "name" to t.name, "accountId" to t.accountId, "status" to t.status,
                "createdAt" to t.createdAt.toString(),
                "responses" to t.responses.map { r ->
                    mapOf("adminDip" to r.adminDip, "text" to r.text, "createdAt" to r.createdAt.toString())
                }
            )
        },
        "updatedAt" to updatedAt.toString()
    )

    private fun seedFirebase() {
        val seed = demoSeed()
        users.value = seed.users
        accounts.value = seed.accounts
        transactions.value = seed.transactions.sortedByDescending { it.createdAt }
        ledgerDebts.value = emptyList()
        subsidyRequests.value = emptyList()
        investmentHoldings.value = seed.investmentHoldings
        digitalCards.value = seed.digitalCards
        savedContacts.value = emptyList()
        donationRewards.value = emptyList()
        promoSlides.value = seed.promoSlides
        treasuryConfig.value = TreasuryConfig()
        complianceFlags.value = emptyList()
        connection.value = ConnectionState.Online
        connectionMessage.value = "Base de datos conectada y datos iniciales creados"
        persist(seed.users, seed.accounts, seed.transactions, emptyList(), emptyList(), seed.investmentHoldings, emptyList(), seed.digitalCards, emptyList(), emptyList(), seed.promoSlides, treasuryConfig.value, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

private data class SeedState(
    val users: List<UserProfile>,
    val accounts: Map<String, Account>,
    val transactions: List<LedgerTransaction>,
    val investmentHoldings: List<InvestmentHolding>,
    val digitalCards: List<DigitalCard>,
    val promoSlides: List<PromoSlide>
)

private fun demoSeed(): SeedState {
    val accounts = mapOf(
        TGLP_ID to Account(TGLP_ID, "TGLP Tributos", AccountKind.TGLP, 8_500, role = Role.Tributos),
        AGLDP_ID to Account(AGLDP_ID, "AGLDP Administración", AccountKind.AGLDP, 94_000, role = Role.Administracion),
        FOUNDATION_RBU_ID to Account(FOUNDATION_RBU_ID, "Fundación Banco de La Placeta", AccountKind.AGLDP, 12_000, role = Role.Administracion),
        CAPITALIA_BANK_ID to Account(CAPITALIA_BANK_ID, "Capitália Empresa", AccountKind.CITIZEN, 18_000, "CAPITALIA-BANK", type = AccountType.Business, citizenshipTier = CitizenshipTier.Institucion, fundsJustificationApproved = true),
        VAULT_EMISION to Account(VAULT_EMISION, "Vault Emisión", AccountKind.AGLDP, 0, role = Role.Administracion),
        "u-alba" to Account("u-alba", "Personal", AccountKind.CITIZEN, 12_400, "ALBA-001", type = AccountType.Current),
        "u-alba-save" to Account("u-alba-save", "Ahorro · Hucha", AccountKind.CITIZEN, 42_000, "ALBA-001", type = AccountType.Savings, huchaLocked = true),
        "u-alba-biz" to Account("u-alba-biz", "Empresa Alba & Co", AccountKind.CITIZEN, 87_500, "ALBA-001", type = AccountType.Business, citizenshipTier = CitizenshipTier.Institucion),
        "u-alba-child" to Account("u-alba-child", "Capitália Junior", AccountKind.CITIZEN, 1_200, "ALBA-001", type = AccountType.Child, iban = IbanGdlp.generateCapitalia("u-alba-child"), reservedGdlpIban = IbanGdlp.generate("u-alba-child"), parentAccountId = "u-alba", sendLimitPz = 50, citizenshipTier = CitizenshipTier.JuniorBasica),
        "u-dario" to Account("u-dario", "Darío Vega", AccountKind.CITIZEN, 6_800, "DARIO-014"),
        "u-lia" to Account("u-lia", "Lía Montes", AccountKind.CITIZEN, 31_500, "LIA-022"),
        "biz-market-dario" to Account("biz-market-dario", "Taller Dario SA", AccountKind.CITIZEN, 64_000, "DARIO-SA", type = AccountType.Business, citizenshipTier = CitizenshipTier.Institucion),
        "biz-market-cristal" to Account("biz-market-cristal", "Cristal Escaso", AccountKind.CITIZEN, 113_000, "CRISTAL-ESC", type = AccountType.Business, citizenshipTier = CitizenshipTier.Institucion),
        "biz-market-propiedad" to Account("biz-market-propiedad", "Propiedad La Placeta #042", AccountKind.CITIZEN, 146_500, "PROP-042", type = AccountType.Business, citizenshipTier = CitizenshipTier.Institucion)
    )
    return SeedState(
        users = listOf(
            UserProfile(
                dip = "DIP-A001",
                displayName = "Alba Cid",
                placetaId = "ALBA-001",
                pinHash = "1234".sha256(),
                primaryAccountId = "u-alba"
            )
        ),
        accounts = accounts,
        transactions = listOf(
            LedgerTransaction("seed-1", TransactionKind.Rbu, AGLDP_ID, "u-alba", 250, note = "Renta Básica Universal"),
            LedgerTransaction("seed-2", TransactionKind.Consumption, "u-alba", "u-dario", 600, ivaPz = 72, note = "Compra de suministros")
        ),
        investmentHoldings = emptyList(),
        digitalCards = listOf(
            DigitalCard("card-main", "u-alba", "Placeta Black", MemberTier.Premium),
            DigitalCard("card-capitalia", "u-alba-child", "Capitália Neon", MemberTier.Child, released = true)
        ),
        promoSlides = defaultPromoSlides()
    )
}

private fun defaultPromoSlides() = listOf(
    PromoSlide(
        "promo-1",
        "BANCO DE LA PLACETA",
        "Tu centro financiero seguro, claro y siempre a mano.",
        PromoAction.Login,
        "bank",
        assetPath = "promos/promo1.png"
    ),
    PromoSlide(
        "promo-2",
        "PLACETAID",
        "Identidad segura para acceder al banco.",
        PromoAction.Login,
        "placezum",
        assetPath = "promos/promo2.png"
    ),
    PromoSlide(
        "promo-3",
        "GOBERNANZA GDLP",
        "Sociedades, cuentas de estado y participación ciudadana.",
        PromoAction.Login,
        "market",
        assetPath = "promos/promo3.png"
    )
)

private fun List<PromoSlide>.promoSignature(): String =
    sortedBy { it.id }.joinToString("|") {
        listOf(it.id, it.title, it.subtitle, it.action.name, it.imageKey, it.imageUrl.orEmpty(), it.assetPath.orEmpty()).joinToString("~")
    }

private fun mergeTransactions(old: List<LedgerTransaction>, newTransactions: List<LedgerTransaction>): List<LedgerTransaction> {
    val withoutReplaced = old.filter { existing -> newTransactions.none { it.id == existing.id } }
    return (newTransactions + withoutReplaced).sortedByDescending { it.createdAt }
}

private fun mergeDebts(old: List<LedgerDebt>, newDebts: List<LedgerDebt>): List<LedgerDebt> {
    val withoutReplaced = old.filter { existing -> newDebts.none { it.id == existing.id } }
    return (newDebts + withoutReplaced).sortedByDescending { it.createdAt }
}

private fun uniqueGdlpIban(accounts: Map<String, Account>, seed: String): String {
    var attempt = 0
    while (attempt < 2_000) {
        val candidate = IbanGdlp.generate(if (attempt == 0) seed else "$seed-$attempt")
        val inUse = accounts.values.any {
            it.iban.equals(candidate, ignoreCase = true) ||
                it.reservedGdlpIban.equals(candidate, ignoreCase = true) ||
                (it.iban.startsWith("CAPI-") && IbanGdlp.toReservedGdlpIban(it.iban).equals(candidate, ignoreCase = true))
        }
        if (!inUse) return candidate
        attempt += 1
    }
    return IbanGdlp.generate("$seed-${UUID.randomUUID()}")
}

private fun Account.toFirebaseMap() = mapOf(
    "id" to id,
    "displayName" to displayName,
    "kind" to kind.name,
    "balancePz" to balancePz,
    "placetaId" to placetaId,
    "role" to role.name,
    "lastRbuClaim" to lastRbuClaim?.toString(),
    "type" to type.name,
    "iban" to iban,
    "reservedGdlpIban" to reservedGdlpIban,
    "parentAccountId" to parentAccountId,
    "huchaLocked" to huchaLocked,
    "sendLimitPz" to sendLimitPz,
    "citizenshipTier" to citizenshipTier.name,
    "complianceStatus" to complianceStatus.name,
    "fundsJustificationApproved" to fundsJustificationApproved,
    "listedInvestmentFund" to listedInvestmentFund,
    "investmentRiskLevel" to investmentRiskLevel,
    "closedAt" to closedAt?.toString()
)

private fun UserProfile.toFirebaseMap() = mapOf(
    "dip" to dip,
    "displayName" to displayName,
    "placetaId" to placetaId,
    "pinHash" to pinHash,
    "primaryAccountId" to primaryAccountId,
    "birthDate" to birthDate,
    "verifiedAge" to verifiedAge,
    "banned" to banned,
    "createdAt" to createdAt.toString()
)

private fun LedgerTransaction.toFirebaseMap() = mapOf(
    "id" to id,
    "kind" to kind.name,
    "fromAccountId" to fromAccountId,
    "toAccountId" to toAccountId,
    "amountPz" to amountPz,
    "ivaPz" to ivaPz,
    "note" to note,
    "status" to status.name,
    "createdAt" to createdAt.toString(),
    "originalTransactionId" to originalTransactionId,
    "netAmount" to netAmount,
    "taxAmount" to taxAmount,
    "concept" to concept,
    "IBAN_Origin" to IBAN_Origin
)

private fun LedgerDebt.toFirebaseMap() = mapOf(
    "id" to id,
    "debt_id" to id,
    "originJuniorId" to originJuniorId,
    "origin_junior_id" to originJuniorId,
    "amountPz" to amountPz,
    "amount" to amountPz,
    "type" to type.name,
    "targetVault" to targetVault,
    "target_vault" to targetVault,
    "status" to status.name,
    "timestamp" to createdAt.toString(),
    "createdAt" to createdAt.toString(),
    "paidAt" to paidAt?.toString(),
    "sourceTransactionId" to sourceTransactionId,
    "note" to note
)

private fun SubsidyRequest.toFirebaseMap() = mapOf(
    "id" to id,
    "requestedBy" to requestedBy,
    "targetAccountId" to targetAccountId,
    "sourceAccountId" to sourceAccountId,
    "amountPz" to amountPz,
    "reason" to reason,
    "status" to status.name,
    "createdAt" to createdAt.toString()
)

private fun InvestmentHolding.toFirebaseMap() = mapOf(
    "id" to id,
    "accountId" to accountId,
    "assetName" to assetName,
    "units" to units,
    "currentValuePz" to currentValuePz,
    "performance" to performance
)

private fun InvestmentOperation.toFirebaseMap() = mapOf(
    "id" to id,
    "accountId" to accountId,
    "companyId" to companyId,
    "assetName" to assetName,
    "amountPz" to amountPz,
    "createdAt" to createdAt.toString(),
    "readyAt" to readyAt.toString(),
    "settledAt" to settledAt?.toString()
)

private fun SavedContact.toFirebaseMap() = mapOf(
    "id" to id,
    "ownerPlacetaId" to ownerPlacetaId,
    "accountId" to accountId,
    "createdAt" to createdAt.toString()
)

private fun DigitalCard.toFirebaseMap() = mapOf(
    "id" to id,
    "accountId" to accountId,
    "alias" to alias,
    "tier" to tier.name,
    "frozen" to frozen,
    "cardNumber" to cardNumber,
    "pin" to pin,
    "promoPhysical" to promoPhysical,
    "released" to released
)

private fun DonationReward.toFirebaseMap() = mapOf(
    "id" to id,
    "dip" to dip,
    "placetaId" to placetaId,
    "amountCents" to amountCents,
    "currency" to currency,
    "points" to points,
    "status" to status.name,
    "destination" to destination.name,
    "merchSku" to merchSku,
    "shippingCountry" to shippingCountry,
    "shippingPostalCode" to shippingPostalCode,
    "shippingRegion" to shippingRegion,
    "stripePaymentIntentId" to stripePaymentIntentId,
    "createdAt" to createdAt.toString(),
    "updatedAt" to updatedAt.toString()
)

private fun PromoSlide.toFirebaseMap() = mapOf(
    "id" to id,
    "title" to title,
    "subtitle" to subtitle,
    "action" to action.name,
    "imageKey" to imageKey,
    "imageUrl" to imageUrl,
    "assetPath" to assetPath
)

private fun TreasuryConfig.toFirebaseMap() = mapOf(
    "operationalTransferTaxPercent" to operationalTransferTaxPercent,
    "webBridgeCommissionPercent" to webBridgeCommissionPercent,
    "contactlessLimitPz" to contactlessLimitPz,
    "placezumWeeklyLimitPz" to placezumWeeklyLimitPz,
    "weeklyTaxPercent" to weeklyTaxPercent,
    "weeklyDeveloperApiFeePercent" to weeklyDeveloperApiFeePercent,
    "weeklyPaymentLinkFeePercent" to weeklyPaymentLinkFeePercent,
    "minimumWeeklySalaryPz" to minimumWeeklySalaryPz,
    "payrollWorkerTaxPercent" to payrollWorkerTaxPercent,
    "payrollEmployerTaxPercent" to payrollEmployerTaxPercent,
    "cardIssueFeePz" to cardIssueFeePz,
    "businessRegistrationFeePz" to businessRegistrationFeePz,
    "auditDailyTransferLimitPz" to auditDailyTransferLimitPz,
    "personalDeclarationThresholdPz" to personalDeclarationThresholdPz,
    "institutionalDeclarationThresholdPz" to institutionalDeclarationThresholdPz,
    "maxCurrentAccounts" to maxCurrentAccounts,
    "maxSavingsAccounts" to maxSavingsAccounts,
    "maxChildAccounts" to maxChildAccounts,
    "maxBusinessAccounts" to maxBusinessAccounts,
    "maxInvestmentAccounts" to maxInvestmentAccounts,
    "maxCurrentBalancePz" to maxCurrentBalancePz,
    "maxSavingsBalancePz" to maxSavingsBalancePz,
    "maxChildBalancePz" to maxChildBalancePz,
    "maxBusinessBalancePz" to maxBusinessBalancePz,
    "maxInvestmentBalancePz" to maxInvestmentBalancePz,
    "savingsInterestAnnualPercent" to savingsInterestAnnualPercent,
    "juniorSavingsInterestAnnualPercent" to juniorSavingsInterestAnnualPercent,
    "lateTaxInterestAnnualPercent" to lateTaxInterestAnnualPercent,
    "irmPersonalPercent" to irmPersonalPercent,
    "irmSharedPercent" to irmSharedPercent,
    "irmBusinessPercent" to irmBusinessPercent,
    "accumulationIndexThreshold" to accumulationIndexThreshold,
    "lotteryTaxPercent" to lotteryTaxPercent,
    "lotteryTaxThresholdPz" to lotteryTaxThresholdPz,
    "investmentProfitTaxPercent" to investmentProfitTaxPercent,
    "investmentGainCommissionPercent" to investmentGainCommissionPercent,
    "maxInvestmentAmountPz" to maxInvestmentAmountPz,
    "dailyInvestmentLimit" to dailyInvestmentLimit,
    "minSupportedVersionCode" to minSupportedVersionCode,
    "capitaliaBankCommissionPercent" to capitaliaBankCommissionPercent,
    "capitaliaBankBaseFeePz" to capitaliaBankBaseFeePz,
    "lastSavingsInterestDate" to lastSavingsInterestDate
)

private fun ComplianceFlag.toFirebaseMap() = mapOf(
    "id" to id,
    "accountId" to accountId,
    "reason" to reason,
    "amountPz" to amountPz,
    "status" to status.name,
    "createdAt" to createdAt.toString()
)

private fun SignedProductContract.toFirebaseMap() = mapOf(
    "id" to id,
    "accountId" to accountId,
    "placetaId" to placetaId,
    "templateId" to templateId,
    "templateVersion" to templateVersion,
    "signedAt" to signedAt,
    "documentId" to documentId,
    "supersededBy" to supersededBy
)

private fun AccountHolder.toFirebaseMap() = mapOf(
    "id" to id,
    "accountId" to accountId,
    "placetaId" to placetaId,
    "role" to role,
    "ownershipPercent" to ownershipPercent,
    "validUntil" to validUntil,
    "linkedAt" to linkedAt
)

private fun PayrollContract.toFirebaseMap() = mapOf(
    "id" to id,
    "companyAccountId" to companyAccountId,
    "employeeAccountId" to employeeAccountId,
    "employeeDip" to employeeDip,
    "employeeName" to employeeName,
    "roleTitle" to roleTitle,
    "grossSalaryPz" to grossSalaryPz,
    "frequency" to frequency.name,
    "status" to status.name,
    "startDate" to startDate.toString(),
    "endDate" to endDate?.toString(),
    "salaryHistory" to salaryHistory.map {
        mapOf("changedAt" to it.changedAt, "previousGrossSalaryPz" to it.previousGrossSalaryPz, "newGrossSalaryPz" to it.newGrossSalaryPz, "reason" to it.reason)
    },
    "createdAt" to createdAt.toString(),
    "updatedAt" to updatedAt.toString()
)

private fun PayrollPeriod.toFirebaseMap() = mapOf(
    "id" to id,
    "contractId" to contractId,
    "companyAccountId" to companyAccountId,
    "employeeAccountId" to employeeAccountId,
    "employeeDip" to employeeDip,
    "label" to label,
    "periodStart" to periodStart.toString(),
    "periodEnd" to periodEnd.toString(),
    "grossSalaryPz" to grossSalaryPz,
    "workerTaxPz" to workerTaxPz,
    "employerTaxPz" to employerTaxPz,
    "netSalaryPz" to netSalaryPz,
    "status" to status.name,
    "paidAt" to paidAt?.toString(),
    "transactionId" to transactionId,
    "createdAt" to createdAt.toString()
)

private fun UserModulePreferences.toFirebaseMap() = mapOf(
    "placetaId" to placetaId,
    "hiddenModules" to hiddenModules
)

private fun Any?.toMapOrNull(): Map<*, *>? = this as? Map<*, *>

private fun Any?.toStringKeyMapOrNull(): Map<String, Any?>? =
    (this as? Map<*, *>)?.entries?.associate { (key, value) -> key.toString() to value }

private fun Any?.toAccountOrNull(): Account? {
    val map = toMapOrNull() ?: return null
    val id = map.string("id")
    val type = map.string("type")
        .ifBlank { AccountType.Current.name }
        .let { if (it.equals("State", ignoreCase = true)) AccountType.Business.name else it }
        .let { enumValueOrDefault(it, AccountType.Current) }
    val kind = map.string("kind")
        .ifBlank { AccountKind.CITIZEN.name }
        .let { raw ->
            when {
                raw.equals("BUSINESS", ignoreCase = true) -> AccountKind.CITIZEN
                raw.equals("COMPANY", ignoreCase = true) -> AccountKind.CITIZEN
                else -> enumValueOrDefault(raw, AccountKind.CITIZEN)
            }
        }
    val role = map.string("role")
        .ifBlank { Role.Citizen.name }
        .let { raw ->
            when {
                raw.equals("BUSINESS", ignoreCase = true) -> Role.Citizen
                raw.equals("COMPANY", ignoreCase = true) -> Role.Citizen
                raw.equals("ADMIN", ignoreCase = true) -> Role.Administracion
                raw.equals("ADMINISTRATION", ignoreCase = true) -> Role.Administracion
                raw.equals("TAX", ignoreCase = true) -> Role.Tributos
                else -> enumValueOrDefault(raw, Role.Citizen)
            }
        }
    val storedIban = map.string("iban").takeIf { IbanGdlp.isAppIban(it) }
    val storedReserved = map.string("reservedGdlpIban").takeIf { IbanGdlp.isAppIban(it) && it.startsWith("GDLP-", ignoreCase = true) }
    val fallbackGdlpIban = IbanGdlp.generate(id)
    val reservedGdlpIban = when {
        type == AccountType.Child && storedReserved != null -> storedReserved
        type == AccountType.Child && storedIban != null -> IbanGdlp.toReservedGdlpIban(storedIban)
        type == AccountType.Child -> fallbackGdlpIban
        else -> null
    }
    val visibleIban = when {
        type == AccountType.Child && reservedGdlpIban != null -> IbanGdlp.toCapitaliaIban(reservedGdlpIban)
        storedIban != null -> storedIban
        else -> fallbackGdlpIban
    }
    return Account(
        id = id,
        displayName = map.string("displayName"),
        kind = kind,
        balancePz = map.long("balancePz"),
        placetaId = map["placetaId"] as? String,
        role = role,
        lastRbuClaim = (map["lastRbuClaim"] as? String)?.let(LocalDate::parse),
        type = type,
        iban = visibleIban,
        reservedGdlpIban = reservedGdlpIban,
        parentAccountId = map["parentAccountId"] as? String,
        huchaLocked = map.boolean("huchaLocked"),
        sendLimitPz = map["sendLimitPz"]?.let { map.long("sendLimitPz") },
        citizenshipTier = enumValueOrDefault(map.string("citizenshipTier"), CitizenshipTier.CiudadaniaPlena),
        complianceStatus = enumValueOrDefault(map.string("complianceStatus"), ComplianceStatus.Clear),
        fundsJustificationApproved = map.boolean("fundsJustificationApproved"),
        listedInvestmentFund = map.boolean("listedInvestmentFund"),
        investmentRiskLevel = map.int("investmentRiskLevel").takeIf { it != 0 }?.coerceIn(1, 7) ?: 3,
        closedAt = map.string("closedAt").takeIf { it.isNotBlank() }?.let(Instant::parse)
    )
}

private fun Any?.toUserProfileOrNull(): UserProfile? {
    val map = toMapOrNull() ?: return null
    val dip = map.string("dip").trim().uppercase()
    if (dip.isBlank()) return null
    return UserProfile(
        dip = dip,
        displayName = map.string("displayName"),
        placetaId = map.string("placetaId"),
        pinHash = map.string("pinHash"),
        primaryAccountId = map.string("primaryAccountId"),
        birthDate = map.string("birthDate").takeIf { it.isNotBlank() },
        verifiedAge = map.int("verifiedAge").takeIf { it != 0 },
        banned = map.boolean("banned"),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { Instant.now().toString() }),
        tributosCensusDate = map.string("tributosCensusDate").takeIf { it.isNotBlank() },
        eip = map.string("eip").takeIf { it.isNotBlank() }
    )
}

private fun Any?.toTransactionOrNull(): LedgerTransaction? {
    val map = toMapOrNull() ?: return null
    val kind = enumValueOrDefault(map.string("kind"), TransactionKind.Placezum)
    val amount = map.long("amountPz")
    val iva = map.long("ivaPz")
    return LedgerTransaction(
        id = map.string("id").ifBlank { "txn-${UUID.randomUUID()}" },
        kind = kind,
        fromAccountId = map.string("fromAccountId"),
        toAccountId = map.string("toAccountId"),
        amountPz = amount,
        ivaPz = iva,
        note = map.string("note"),
        status = enumValueOrDefault(map.string("status"), TransactionStatus.Settled),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { Instant.now().toString() }),
        originalTransactionId = map["originalTransactionId"] as? String,
        netAmount = map.long("netAmount").takeIf { it != 0L } ?: amount,
        taxAmount = map.long("taxAmount").takeIf { it != 0L } ?: iva,
        concept = map.string("concept").ifBlank { kind.name },
        IBAN_Origin = map.string("IBAN_Origin").ifBlank { map.string("fromAccountId") }
    )
}

private fun Any?.toLedgerDebtOrNull(): LedgerDebt? {
    val map = toMapOrNull() ?: return null
    val id = map.string("id").ifBlank { map.string("debt_id") }
    if (id.isBlank()) return null
    val type = enumValueOrDefault(map.string("type"), LedgerDebtType.OPERATIONAL_FEE)
    return LedgerDebt(
        id = id,
        originJuniorId = map.string("originJuniorId").ifBlank { map.string("origin_junior_id") },
        amountPz = map.long("amountPz").takeIf { it != 0L } ?: map.long("amount"),
        type = type,
        targetVault = map.string("targetVault").ifBlank { map.string("target_vault") }.ifBlank {
            if (type == LedgerDebtType.IVA_CONSUMO) VAULT_TRIBUTOS_IVA else VAULT_ADMIN_GENERAL
        },
        status = enumValueOrDefault(map.string("status"), LedgerDebtStatus.PENDING),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { map.string("timestamp").ifBlank { Instant.now().toString() } }),
        paidAt = map.string("paidAt").takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() },
        sourceTransactionId = map.string("sourceTransactionId").takeIf { it.isNotBlank() },
        note = map.string("note")
    )
}

private fun Any?.toRequestOrNull(): SubsidyRequest? {
    val map = toMapOrNull() ?: return null
    return SubsidyRequest(
        id = map.string("id").ifBlank { "sub-${UUID.randomUUID()}" },
        requestedBy = map.string("requestedBy").ifBlank { TGLP_ID },
        targetAccountId = map.string("targetAccountId").ifBlank { TGLP_ID },
        sourceAccountId = map.string("sourceAccountId").ifBlank { AGLDP_ID },
        amountPz = map.long("amountPz"),
        reason = map.string("reason"),
        status = enumValueOrDefault(map.string("status"), TransactionStatus.Pending),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { Instant.now().toString() })
    )
}

private fun Any?.toHoldingOrNull(): InvestmentHolding? {
    val map = toMapOrNull() ?: return null
    return InvestmentHolding(
        id = map.string("id").ifBlank { "hold-${UUID.randomUUID()}" },
        accountId = map.string("accountId").ifBlank { "u-alba-invest" },
        assetName = map.string("assetName"),
        units = map.int("units"),
        currentValuePz = map.long("currentValuePz"),
        performance = map.longList("performance")
    )
}

private fun Any?.toInvestmentOperationOrNull(): InvestmentOperation? {
    val map = toMapOrNull() ?: return null
    return InvestmentOperation(
        id = map.string("id").ifBlank { "op-${UUID.randomUUID()}" },
        accountId = map.string("accountId"),
        companyId = map.string("companyId"),
        assetName = map.string("assetName"),
        amountPz = map.long("amountPz"),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { Instant.now().toString() }),
        readyAt = Instant.parse(map.string("readyAt").ifBlank { Instant.now().toString() }),
        settledAt = map.string("settledAt").takeIf { it.isNotBlank() }?.letCatchingInstant()
    )
}

private fun Any?.toDigitalCardOrNull(): DigitalCard? {
    val map = toMapOrNull() ?: return null
    return DigitalCard(
        id = map.string("id").ifBlank { "card-${UUID.randomUUID()}" },
        accountId = map.string("accountId").ifBlank { "u-alba" },
        alias = map.string("alias").ifBlank { "Placeta Black" },
        tier = enumValueOrDefault(map.string("tier"), MemberTier.Standard),
        frozen = map.boolean("frozen"),
        cardNumber = map.string("cardNumber").filter(Char::isDigit).padEnd(6, '0').takeLast(6),
        pin = map.string("pin").filter(Char::isDigit).padEnd(4, '0').take(4),
        promoPhysical = map.boolean("promoPhysical"),
        released = map.boolean("released")
    )
}

private fun Any?.toSavedContactOrNull(): SavedContact? {
    val map = toMapOrNull() ?: return null
    return SavedContact(
        id = map.string("id").ifBlank { "contact-${UUID.randomUUID()}" },
        ownerPlacetaId = map.string("ownerPlacetaId"),
        accountId = map.string("accountId"),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { Instant.now().toString() })
    )
}

private fun Any?.toDonationRewardOrNull(): DonationReward? {
    val map = toMapOrNull() ?: return null
    val id = map.string("id")
    if (id.isBlank()) return null
    return DonationReward(
        id = id,
        dip = map.string("dip").uppercase(),
        placetaId = map.string("placetaId").uppercase(),
        amountCents = map.long("amountCents"),
        currency = map.string("currency").ifBlank { "EUR" }.uppercase(),
        points = map.long("points"),
        status = enumValueOrDefault(map.string("status"), DonationRewardStatus.Available),
        destination = enumValueOrDefault(map.string("destination"), DonationRewardDestination.Wallet),
        merchSku = map.string("merchSku").takeIf { it.isNotBlank() },
        shippingCountry = map.string("shippingCountry").takeIf { it.isNotBlank() },
        shippingPostalCode = map.string("shippingPostalCode").takeIf { it.isNotBlank() },
        shippingRegion = map.string("shippingRegion").takeIf { it.isNotBlank() },
        stripePaymentIntentId = map.string("stripePaymentIntentId"),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { Instant.now().toString() }),
        updatedAt = Instant.parse(map.string("updatedAt").ifBlank { map.string("createdAt").ifBlank { Instant.now().toString() } })
    )
}

private fun Any?.toPromoSlideOrNull(): PromoSlide? {
    val map = toMapOrNull() ?: return null
    return PromoSlide(
        id = map.string("id").ifBlank { "promo-${UUID.randomUUID()}" },
        title = map.string("title").ifBlank { "BANCO PLACETA" },
        subtitle = map.string("subtitle").ifBlank { "Servicios GDLP disponibles" },
        action = enumValueOrDefault(map.string("action"), PromoAction.Login),
        imageKey = map.string("imageKey").ifBlank { "bank" },
        imageUrl = map.string("imageUrl").takeIf { it.isNotBlank() },
        assetPath = map.string("assetPath").takeIf { it.isNotBlank() }
    )
}

private fun Any?.toTreasuryConfigOrNull(): TreasuryConfig? {
    val map = toMapOrNull() ?: return null
    return TreasuryConfig(
        operationalTransferTaxPercent = map.int("operationalTransferTaxPercent").takeIf { it != 0 } ?: 7,
        webBridgeCommissionPercent = (map["webBridgeCommissionPercent"] as? Number)?.toInt() ?: 3,
        contactlessLimitPz = map.long("contactlessLimitPz").takeIf { it != 0L } ?: 500,
        placezumWeeklyLimitPz = map.long("placezumWeeklyLimitPz").takeIf { it != 0L } ?: 1_000,
        weeklyTaxPercent = map.int("weeklyTaxPercent").takeIf { it != 0 } ?: 2,
        weeklyDeveloperApiFeePercent = (map["weeklyDeveloperApiFeePercent"] as? Number)?.toInt() ?: 1,
        weeklyPaymentLinkFeePercent = (map["weeklyPaymentLinkFeePercent"] as? Number)?.toInt() ?: 1,
        minimumWeeklySalaryPz = map.long("minimumWeeklySalaryPz").takeIf { it != 0L } ?: 150,
        payrollWorkerTaxPercent = (map["payrollWorkerTaxPercent"] as? Number)?.toInt() ?: 10,
        payrollEmployerTaxPercent = (map["payrollEmployerTaxPercent"] as? Number)?.toInt() ?: 10,
        cardIssueFeePz = map.long("cardIssueFeePz").takeIf { it != 0L } ?: 25,
        businessRegistrationFeePz = map.long("businessRegistrationFeePz").takeIf { it != 0L } ?: 250,
        auditDailyTransferLimitPz = map.long("auditDailyTransferLimitPz").takeIf { it != 0L } ?: 5_000,
        personalDeclarationThresholdPz = map.long("personalDeclarationThresholdPz").takeIf { it != 0L } ?: 500_000,
        institutionalDeclarationThresholdPz = map.long("institutionalDeclarationThresholdPz").takeIf { it != 0L } ?: 10_000_000,
        maxCurrentAccounts = map.int("maxCurrentAccounts").takeIf { it != 0 } ?: 4,
        maxSavingsAccounts = map.int("maxSavingsAccounts").takeIf { it != 0 } ?: 3,
        maxChildAccounts = map.int("maxChildAccounts").takeIf { it != 0 } ?: 4,
        maxBusinessAccounts = map.int("maxBusinessAccounts").takeIf { it != 0 } ?: 2,
        maxInvestmentAccounts = map.int("maxInvestmentAccounts").takeIf { it != 0 } ?: 2,
        maxCurrentBalancePz = map.long("maxCurrentBalancePz").takeIf { it != 0L } ?: 500_000,
        maxSavingsBalancePz = map.long("maxSavingsBalancePz").takeIf { it != 0L } ?: 1_000_000,
        maxChildBalancePz = map.long("maxChildBalancePz").takeIf { it != 0L } ?: 5_000,
        maxBusinessBalancePz = map.long("maxBusinessBalancePz").takeIf { it != 0L } ?: 10_000_000,
        maxInvestmentBalancePz = map.long("maxInvestmentBalancePz").takeIf { it != 0L } ?: 250_000,
        savingsInterestAnnualPercent = map.int("savingsInterestAnnualPercent").takeIf { it != 0 } ?: 2,
        juniorSavingsInterestAnnualPercent = map.int("juniorSavingsInterestAnnualPercent").takeIf { it != 0 } ?: 3,
        lateTaxInterestAnnualPercent = map.int("lateTaxInterestAnnualPercent").takeIf { it != 0 } ?: 12,
        irmPersonalPercent = map.int("irmPersonalPercent").takeIf { it != 0 } ?: 5,
        irmSharedPercent = map.int("irmSharedPercent").takeIf { it != 0 } ?: 6,
        irmBusinessPercent = map.int("irmBusinessPercent").takeIf { it != 0 } ?: 9,
        accumulationIndexThreshold = map.double("accumulationIndexThreshold").takeIf { it != 0.0 } ?: 0.30,
        lotteryTaxPercent = map.int("lotteryTaxPercent").takeIf { it != 0 } ?: 20,
        lotteryTaxThresholdPz = map.long("lotteryTaxThresholdPz").takeIf { it != 0L } ?: 1_000,
        investmentProfitTaxPercent = map.int("investmentProfitTaxPercent").takeIf { it != 0 } ?: 10,
        investmentGainCommissionPercent = map.int("investmentGainCommissionPercent").takeIf { it != 0 } ?: 4,
        maxInvestmentAmountPz = map.long("maxInvestmentAmountPz").takeIf { it != 0L } ?: 1_200,
        dailyInvestmentLimit = map.int("dailyInvestmentLimit").takeIf { it != 0 } ?: 15,
        minSupportedVersionCode = map.int("minSupportedVersionCode").takeIf { it != 0 } ?: 1,
        capitaliaBankCommissionPercent = map.int("capitaliaBankCommissionPercent").takeIf { it != 0 } ?: 2,
        capitaliaBankBaseFeePz = map.long("capitaliaBankBaseFeePz").takeIf { it != 0L } ?: 35,
        lastSavingsInterestDate = map["lastSavingsInterestDate"] as? String
    ).normalized()
}

private fun Any?.toComplianceFlagOrNull(): ComplianceFlag? {
    val map = toMapOrNull() ?: return null
    return ComplianceFlag(
        id = map.string("id").ifBlank { "flag-${UUID.randomUUID()}" },
        accountId = map.string("accountId"),
        reason = map.string("reason"),
        amountPz = map.long("amountPz"),
        status = enumValueOrDefault(map.string("status"), ComplianceStatus.PendingReview),
        createdAt = Instant.parse(map.string("createdAt").ifBlank { Instant.now().toString() })
    )
}

private fun Map<*, *>.string(key: String): String = this[key]?.toString().orEmpty()

private fun Map<*, *>.int(key: String): Int = long(key).toInt()

private fun Map<*, *>.long(key: String): Long = when (val value = this[key]) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull() ?: 0L
    else -> 0L
}

private fun Map<*, *>.boolean(key: String): Boolean = when (val value = this[key]) {
    is Boolean -> value
    is String -> value.toBoolean()
    else -> false
}

private fun Map<*, *>.double(key: String): Double = when (val value = this[key]) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull() ?: 0.0
    else -> 0.0
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default

private fun Map<*, *>.longList(key: String): List<Long> =
    (this[key] as? List<*>)?.mapNotNull {
        when (it) {
            is Number -> it.toLong()
            is String -> it.toLongOrNull()
            else -> null
        }
    } ?: emptyList()

private fun Map<*, *>.list(key: String): List<Any?> =
    (this[key] as? List<*>) ?: emptyList<Any?>()

private fun Map<String, Any?>.toJsonCompatibleMap(): Map<String, Any?> =
    mapValues { (_, value) -> value.toJsonCompatibleValue() }

private fun Any?.toJsonCompatibleValue(): Any? = when (this) {
    null -> JSONObject.NULL
    is Map<*, *> -> JSONObject(this.entries.associate { it.key.toString() to it.value.toJsonCompatibleValue() })
    is List<*> -> JSONArray().also { array -> this.forEach { array.put(it.toJsonCompatibleValue()) } }
    else -> this
}

private fun JSONObject.toPlainMap(): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    keys().forEach { key ->
        result[key] = get(key).toPlainValue()
    }
    return result
}

private fun JSONArray.toPlainList(): List<Any?> =
    List(length()) { index -> get(index).toPlainValue() }

private fun Any?.toPlainValue(): Any? = when (this) {
    JSONObject.NULL -> null
    is JSONObject -> toPlainMap()
    is JSONArray -> toPlainList()
    else -> this
}

private fun String.letCatchingInstant(): Instant? =
    runCatching { Instant.parse(this) }.getOrNull()

private fun hmacSha256Hex(secret: String, value: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return mac.doFinal(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

private fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

private fun buildAuditFlags(
    accounts: Map<String, Account>,
    transactions: List<LedgerTransaction>,
    existing: List<ComplianceFlag>,
    config: TreasuryConfig
): List<ComplianceFlag> {
    val since = Instant.now().minusSeconds(24 * 60 * 60)
    val existingKeys = existing.map { "${it.accountId}:${it.reason}" }.toSet()
    val volumeFlags = transactions
        .filter { it.createdAt.isAfter(since) && it.status == TransactionStatus.Settled }
        .groupBy { it.fromAccountId }
        .mapValues { (_, txns) -> txns.sumOf { it.amountPz } }
        .filterValues { it > config.auditDailyTransferLimitPz }
        .mapNotNull { (accountId, amount) ->
            val key = "$accountId:Movimiento 24h superior a ${config.auditDailyTransferLimitPz} Pz"
            if (key in existingKeys) null else ComplianceFlag(
                id = "flag-${UUID.randomUUID()}",
                accountId = accountId,
                reason = "Movimiento 24h superior a ${config.auditDailyTransferLimitPz} Pz",
                amountPz = amount
            )
        }
    val declarationFlags = accounts.values.mapNotNull { account ->
        val threshold = if (account.kind == AccountKind.CITIZEN && account.type != AccountType.Business) {
            config.personalDeclarationThresholdPz
        } else {
            config.institutionalDeclarationThresholdPz
        }
        val key = "${account.id}:Declaración de fondos obligatoria"
        if (account.balancePz >= threshold && !account.fundsJustificationApproved && key !in existingKeys) {
            ComplianceFlag(
                id = "flag-${UUID.randomUUID()}",
                accountId = account.id,
                reason = "Declaración de fondos obligatoria",
                amountPz = account.balancePz,
                status = ComplianceStatus.DeclarationRequired
            )
        } else null
    }
    return (volumeFlags + declarationFlags + existing).sortedByDescending { it.createdAt }
}

fun seedRegularization(repository: BankRepository): String? {
    val tglp = repository.accounts.value[TGLP_ID] ?: return "Cuenta TGLP no encontrada"
    val adjustment = LedgerTransaction(
        id = "iva-${UUID.randomUUID()}",
        kind = TransactionKind.IvaAdjustment,
        fromAccountId = TGLP_ID,
        toAccountId = TGLP_ID,
        amountPz = 0,
        note = "Regularización IVA ejecutada con saldo ${tglp.balancePz} Pz"
    )
    return repository.run(EconomyResult.Success(adjustment, repository.accounts.value, listOf(adjustment)))
}
