package com.placeta.banco.core

import java.time.Instant
import java.time.LocalDate

const val TGLP_ID = "TGLP"
const val AGLDP_ID = "AGLDP"
const val FOUNDATION_RBU_ID = "FOUNDATION_RBU"
const val CAPITALIA_BANK_ID = "CAPITALIA_BANK"
const val VAULT_TRIBUTOS_IVA = TGLP_ID
const val VAULT_ADMIN_GENERAL = AGLDP_ID
const val VAULT_EMISION = "VAULT_EMISION"
const val VAT_PERCENT = 12
const val IRM_PERCENT = 9
const val MINIMUM_INCOME_SHIELD_PZ = 5L
const val RBU_COOLDOWN_DAYS = 7L
const val OFFICIAL_IBAN_PREFIX = "GDLP"
const val CAPITALIA_IBAN_PREFIX = "CAPI"

enum class AccountKind { TGLP, AGLDP, CITIZEN }

enum class AccountType { Current, Savings, Child, Business, Investment, State }

enum class Role { Citizen, Tributos, Administracion }

enum class CitizenshipTier { JuniorBasica, JuniorSenior, CiudadaniaPlena, Institucion }

enum class ComplianceStatus { Clear, PendingReview, DeclarationRequired, Approved }

enum class TransactionKind {
    Consumption,
    Gift,
    Allowance,
    PayrollLoan,
    Fine,
    Tax,
    Subsidy,
    Donation,
    WelcomeBonus,
    Rbu,
    Reversal,
    IvaAdjustment,
    Placezum,
    Dividend,
    ExternalBlocked,
    MonetaryEmission,
    IrmCharge,
    ForcedVatRegularization,
    InvestmentBuy,
    InvestmentSell,
    OperationalFee,
    LotteryPrize,
    InvestmentTax,
    InvestmentCommission,
    LateTaxInterest,
    SavingsInterest,
    CardIssueFee,
    BusinessRegistrationFee,
    CapitaliaServiceFee
}

enum class TransactionStatus { Settled, Reversed, Pending, Denied }

enum class LedgerDebtType { IVA_CONSUMO, IRM_PATRIMONIO, OPERATIONAL_FEE }

enum class LedgerDebtStatus { PENDING, PAID, AUDIT_REQUIRED }

data class LedgerDebt(
    val id: String,
    val originJuniorId: String,
    val amountPz: Long,
    val type: LedgerDebtType,
    val targetVault: String,
    val status: LedgerDebtStatus = LedgerDebtStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val paidAt: Instant? = null,
    val sourceTransactionId: String? = null,
    val note: String = ""
)

data class Account(
    val id: String,
    val displayName: String,
    val kind: AccountKind,
    val balancePz: Long,
    val placetaId: String? = null,
    val role: Role = Role.Citizen,
    val lastRbuClaim: LocalDate? = null,
    val type: AccountType = AccountType.Current,
    val iban: String = IbanGdlp.generate(id),
    val reservedGdlpIban: String? = null,
    val parentAccountId: String? = null,
    val huchaLocked: Boolean = false,
    val sendLimitPz: Long? = null,
    val citizenshipTier: CitizenshipTier = CitizenshipTier.CiudadaniaPlena,
    val complianceStatus: ComplianceStatus = ComplianceStatus.Clear,
    val fundsJustificationApproved: Boolean = false,
    val listedInvestmentFund: Boolean = false,
    val investmentRiskLevel: Int = 3,
    val irmOptIn: Boolean = false,
    val irmDueDate: String? = null,
    val payrollDay: Int = 1,
    val closedAt: Instant? = null
)

data class LedgerTransaction(
    val id: String,
    val kind: TransactionKind,
    val fromAccountId: String,
    val toAccountId: String,
    val amountPz: Long,
    val ivaPz: Long = 0,
    val note: String,
    val status: TransactionStatus = TransactionStatus.Settled,
    val createdAt: Instant = Instant.now(),
    val originalTransactionId: String? = null,
    val netAmount: Long = amountPz,
    val taxAmount: Long = ivaPz,
    val concept: String = kind.name,
    val IBAN_Origin: String = fromAccountId
)

data class Transaction(
    val id: String,
    val netAmount: Long,
    val taxAmount: Long,
    val concept: String,
    val IBAN_Origin: String,
    val toIban: String,
    val status: TransactionStatus = TransactionStatus.Pending,
    val createdAt: Instant = Instant.now()
)

data class SubsidyRequest(
    val id: String,
    val requestedBy: String = TGLP_ID,
    val targetAccountId: String = TGLP_ID,
    val sourceAccountId: String = AGLDP_ID,
    val amountPz: Long,
    val reason: String,
    val status: TransactionStatus = TransactionStatus.Pending,
    val createdAt: Instant = Instant.now()
)

data class PlacezumCode(
    val code: String,
    val accountId: String,
    val iban: String,
    val expiresAt: Instant
)

enum class PromoAction { Login, Register, Demo }

data class PromoSlide(
    val id: String,
    val title: String,
    val subtitle: String,
    val action: PromoAction = PromoAction.Login,
    val imageKey: String = "bank",
    val imageUrl: String? = null,
    val assetPath: String? = null
)

data class InvestmentHolding(
    val id: String,
    val accountId: String,
    val assetName: String,
    val units: Int,
    val currentValuePz: Long,
    val performance: List<Long>
)

data class InvestmentOperation(
    val id: String,
    val accountId: String,
    val companyId: String,
    val assetName: String,
    val amountPz: Long,
    val createdAt: Instant = Instant.now(),
    val readyAt: Instant,
    val settledAt: Instant? = null
)

data class SavedContact(
    val id: String,
    val ownerPlacetaId: String,
    val accountId: String,
    val createdAt: Instant = Instant.now()
)

enum class MemberTier { Standard, Premium, Child }

data class DigitalCard(
    val id: String,
    val accountId: String,
    val alias: String,
    val tier: MemberTier,
    val frozen: Boolean = false,
    val cardNumber: String = id.filter(Char::isDigit).padEnd(6, '0').takeLast(6),
    val pin: String = "0000",
    val promoPhysical: Boolean = false,
    val released: Boolean = false
)

enum class DocumentKind {
    VatReceipt,
    LoanContract,
    SolvencyCertificate,
    MonthlyStatement,
    WeeklyTaxReport,
    PaymentReceipt,
    FineReceipt,
    BusinessStatement,
    FiscalRequirement,
    InvestmentLiquidation,
    LaborContract,
    PayrollPayslip
}

data class DigitalDocument(
    val id: String,
    val accountId: String,
    val title: String,
    val kind: DocumentKind,
    val issuedAt: Instant = Instant.now()
)

enum class DonationRewardStatus { Available, Redeemed, Donated }

enum class DonationRewardDestination { Wallet, Foundation, Merch }

data class DonationReward(
    val id: String,
    val dip: String,
    val placetaId: String,
    val amountCents: Long,
    val currency: String = "EUR",
    val points: Long,
    val status: DonationRewardStatus = DonationRewardStatus.Available,
    val destination: DonationRewardDestination = DonationRewardDestination.Wallet,
    val merchSku: String? = null,
    val shippingCountry: String? = null,
    val shippingPostalCode: String? = null,
    val shippingRegion: String? = null,
    val stripePaymentIntentId: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt
)

data class SupportTicket(
    val id: String,
    val category: String = "General",
    val priority: String = "Media",
    val subject: String = "",
    val message: String = "",
    val dip: String = "",
    val name: String = "",
    val accountId: String = "",
    val status: String = "Abierto",
    val createdAt: Instant = Instant.now(),
    val responses: List<SupportTicketResponse> = emptyList()
)

data class SupportTicketResponse(
    val adminDip: String = "",
    val text: String = "",
    val createdAt: Instant = Instant.now()
)

data class TreasuryConfig(
    val operationalTransferTaxPercent: Int = 7,
    val webBridgeCommissionPercent: Int = 3,
    val contactlessLimitPz: Long = 500,
    val placezumWeeklyLimitPz: Long = 1_000,
    val weeklyTaxPercent: Int = 2,
    val weeklyDeveloperApiFeePercent: Int = 1,
    val weeklyPaymentLinkFeePercent: Int = 1,
    val minimumWeeklySalaryPz: Long = 150,
    val payrollWorkerTaxPercent: Int = 10,
    val payrollEmployerTaxPercent: Int = 10,
    val cardIssueFeePz: Long = 25,
    val businessRegistrationFeePz: Long = 250,
    val auditDailyTransferLimitPz: Long = 5_000,
    val personalDeclarationThresholdPz: Long = 500_000,
    val institutionalDeclarationThresholdPz: Long = 10_000_000,
    val maxCurrentAccounts: Int = 4,
    val maxSavingsAccounts: Int = 3,
    val maxChildAccounts: Int = 4,
    val maxBusinessAccounts: Int = 2,
    val maxInvestmentAccounts: Int = 2,
    val maxCurrentBalancePz: Long = 500_000,
    val maxSavingsBalancePz: Long = 1_000_000,
    val maxChildBalancePz: Long = 5_000,
    val maxBusinessBalancePz: Long = 10_000_000,
    val maxInvestmentBalancePz: Long = 250_000,
    val savingsInterestAnnualPercent: Int = 2,
    val juniorSavingsInterestAnnualPercent: Int = 3,
    val lateTaxInterestAnnualPercent: Int = 12,
    val irmPersonalPercent: Int = 5,
    val irmSharedPercent: Int = 6,
    val irmBusinessPercent: Int = 9,
    val accumulationIndexThreshold: Double = 0.30,
    val lotteryTaxPercent: Int = 20,
    val lotteryTaxThresholdPz: Long = 1_000,
    val investmentProfitTaxPercent: Int = 10,
    val investmentGainCommissionPercent: Int = 4,
    val maxInvestmentAmountPz: Long = 1_200,
    val dailyInvestmentLimit: Int = 15,
    val minSupportedVersionCode: Int = 4,
    val capitaliaBankCommissionPercent: Int = 2,
    val capitaliaBankBaseFeePz: Long = 35,
    val lastSavingsInterestDate: String? = null,
    val configRevision: Int = 1,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "El sistema está en mantenimiento temporal.",
    val availableModules: List<String> = emptyList()
) {
    fun normalized(): TreasuryConfig = copy(
        operationalTransferTaxPercent = operationalTransferTaxPercent.coerceIn(0, VAT_PERCENT),
        webBridgeCommissionPercent = webBridgeCommissionPercent.coerceIn(0, VAT_PERCENT),
        placezumWeeklyLimitPz = placezumWeeklyLimitPz.coerceIn(0, 1_000_000),
        weeklyTaxPercent = weeklyTaxPercent.coerceIn(0, 25),
        weeklyDeveloperApiFeePercent = weeklyDeveloperApiFeePercent.coerceIn(0, 25),
        weeklyPaymentLinkFeePercent = weeklyPaymentLinkFeePercent.coerceIn(0, 25),
        minimumWeeklySalaryPz = minimumWeeklySalaryPz.coerceIn(1, 10_000),
        payrollWorkerTaxPercent = payrollWorkerTaxPercent.coerceIn(0, 35),
        payrollEmployerTaxPercent = payrollEmployerTaxPercent.coerceIn(0, 35),
        maxCurrentAccounts = maxCurrentAccounts.coerceIn(0, 25),
        maxSavingsAccounts = maxSavingsAccounts.coerceIn(0, 25),
        maxChildAccounts = maxChildAccounts.coerceIn(0, 25),
        maxBusinessAccounts = maxBusinessAccounts.coerceIn(0, 25),
        maxInvestmentAccounts = maxInvestmentAccounts.coerceIn(0, 25),
        maxCurrentBalancePz = maxCurrentBalancePz.coerceIn(0, 100_000_000),
        maxSavingsBalancePz = maxSavingsBalancePz.coerceIn(0, 100_000_000),
        maxChildBalancePz = maxChildBalancePz.coerceIn(0, 100_000_000),
        maxBusinessBalancePz = maxBusinessBalancePz.coerceIn(0, 100_000_000),
        maxInvestmentBalancePz = maxInvestmentBalancePz.coerceIn(0, 100_000_000),
        savingsInterestAnnualPercent = savingsInterestAnnualPercent.coerceIn(0, 12),
        juniorSavingsInterestAnnualPercent = juniorSavingsInterestAnnualPercent.coerceIn(0, 12),
        lateTaxInterestAnnualPercent = lateTaxInterestAnnualPercent.coerceIn(0, 100),
        lotteryTaxPercent = lotteryTaxPercent.coerceIn(0, 100),
        investmentProfitTaxPercent = investmentProfitTaxPercent.coerceIn(0, 100),
        investmentGainCommissionPercent = investmentGainCommissionPercent.coerceIn(0, 100),
        maxInvestmentAmountPz = maxInvestmentAmountPz.coerceIn(1, 1_200),
        dailyInvestmentLimit = dailyInvestmentLimit.coerceIn(1, 250),
        minSupportedVersionCode = minSupportedVersionCode.coerceAtLeast(1),
        capitaliaBankCommissionPercent = capitaliaBankCommissionPercent.coerceIn(0, 25),
        capitaliaBankBaseFeePz = capitaliaBankBaseFeePz.coerceIn(0, 100_000)
    )
}

data class ComplianceFlag(
    val id: String,
    val accountId: String,
    val reason: String,
    val amountPz: Long,
    val status: ComplianceStatus = ComplianceStatus.PendingReview,
    val createdAt: Instant = Instant.now()
)

data class DipIdentity(
    val dip: String,
    val accountIds: List<String>,
    val lastLoginIpHash: String? = null,
    val unusualIpAlert: Boolean = false,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun isOfficialDip(value: String): Boolean =
            Regex("^DIP-[A-Z0-9]{4}$").matches(value.uppercase())
    }
}

data class UserProfile(
    val dip: String,
    val displayName: String,
    val placetaId: String,
    val pinHash: String,
    val primaryAccountId: String,
    val birthDate: String? = null,
    val verifiedAge: Int? = null,
    val banned: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val tributosCensusDate: String? = null,
    val eip: String? = null
)

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val color: String,
    val unlockedAt: Instant? = null
)

data class RankingEntry(
    val placetaId: String,
    val displayName: String,
    val score: Long,
    val rank: Int,
    val avatarUrl: String? = null
)

object IbanGdlp {
    fun generate(seed: String): String {
        val normalized = seed.uppercase().filter { it.isLetterOrDigit() }.ifBlank { "0000" }
        val body = normalized.fold(17) { acc, char -> (acc * 31 + char.code) % 1_000 }
        val control = controlDigits(body)
        return "$OFFICIAL_IBAN_PREFIX-AP${control.toString().padStart(2, '0')}-${body.toString().padStart(3, '0')}"
    }

    fun generateCapitalia(seed: String): String = toCapitaliaIban(generate(seed))

    fun toCapitaliaIban(gdlpIban: String): String =
        gdlpIban.uppercase().replaceFirst("$OFFICIAL_IBAN_PREFIX-", "$CAPITALIA_IBAN_PREFIX-")

    fun toReservedGdlpIban(iban: String): String =
        iban.uppercase().replaceFirst("$CAPITALIA_IBAN_PREFIX-", "$OFFICIAL_IBAN_PREFIX-")

    fun isOfficial(iban: String): Boolean {
        val parts = iban.uppercase().split("-")
        if (parts.size != 3 || parts[0] !in setOf(OFFICIAL_IBAN_PREFIX, CAPITALIA_IBAN_PREFIX)) return false
        if (!parts[1].startsWith("AP") && !parts[1].startsWith("W")) return false
        if (parts[0] == CAPITALIA_IBAN_PREFIX && parts[1].startsWith("W")) return false
        if (parts[1].startsWith("W")) return Regex("^GDLP-W\\d{3}-\\d{4}$").matches(iban.uppercase())
        val control = parts[1].removePrefix("AP").toIntOrNull() ?: return false
        val body = parts[2].toIntOrNull() ?: return false
        return control == controlDigits(body)
    }

    fun isAppIban(iban: String): Boolean = Regex("^(GDLP|CAPI)-AP\\d{2}-\\d{3}$").matches(iban.uppercase()) && isOfficial(iban)

    private fun controlDigits(body: Int): Int = ((body * 97) + 13) % 100
}

sealed interface EconomyResult<out T> {
    data class Success<T>(
        val value: T,
        val accounts: Map<String, Account>,
        val transactions: List<LedgerTransaction> = emptyList(),
        val debts: List<LedgerDebt> = emptyList()
    ) : EconomyResult<T>

    data class Failure(val message: String) : EconomyResult<Nothing>
}

enum class PayrollFrequency { Weekly, Biweekly, Monthly }
enum class PayrollContractStatus { Draft, Active, Paused, Ended }
enum class PayrollPeriodStatus { Pending, Paid, Cancelled }
data class PayrollSalaryChange(
    val changedAt: String = "",
    val previousGrossSalaryPz: Long,
    val newGrossSalaryPz: Long,
    val reason: String = ""
)

data class PayrollContract(
    val id: String,
    val companyAccountId: String,
    val employeeAccountId: String,
    val employeeDip: String,
    val employeeName: String,
    val roleTitle: String,
    val grossSalaryPz: Long,
    val frequency: PayrollFrequency = PayrollFrequency.Weekly,
    val status: PayrollContractStatus = PayrollContractStatus.Active,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val salaryHistory: List<PayrollSalaryChange> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class PayrollPeriod(
    val id: String,
    val contractId: String,
    val companyAccountId: String,
    val employeeAccountId: String,
    val employeeDip: String,
    val label: String,
    val periodStart: LocalDate = LocalDate.now(),
    val periodEnd: LocalDate = LocalDate.now(),
    val grossSalaryPz: Long,
    val workerTaxPz: Long = 0,
    val employerTaxPz: Long = 0,
    val netSalaryPz: Long = 0,
    val status: PayrollPeriodStatus = PayrollPeriodStatus.Pending,
    val paidAt: Instant? = null,
    val transactionId: String? = null,
    val createdAt: Instant = Instant.now()
)

data class PayrollPayslip(
    val id: String,
    val periodId: String,
    val employeeDip: String,
    val employeeName: String,
    val periodLabel: String,
    val grossSalaryPz: Long,
    val workerTaxPz: Long = 0,
    val employerTaxPz: Long = 0,
    val netSalaryPz: Long,
    val issuedAt: String = "",
    val companyName: String = ""
)

data class AccountHolder(
    val id: String,
    val accountId: String,
    val placetaId: String,
    val role: String = "CoOwner",
    val ownershipPercent: Int = 0,
    val validUntil: String? = null,
    val linkedAt: String = ""
)

data class ProductContractTemplate(
    val id: String,
    val productType: String,
    val version: Int = 1,
    val clausesHash: String = "",
    val effectiveFrom: String = ""
)

data class SignedProductContract(
    val id: String,
    val accountId: String,
    val placetaId: String,
    val templateId: String,
    val templateVersion: Int = 1,
    val signedAt: String = "",
    val documentId: String = "",
    val supersededBy: String? = null
)

data class UserModulePreferences(
    val placetaId: String,
    val hiddenModules: List<String> = emptyList()
)

data class GuardianRenewalDecision(
    val id: String,
    val childAccountId: String,
    val expiresAt: String = "",
    val status: String = "Pending"
)

data class ExecutionCode(
    val id: String,
    val code: String = "",
    val issuedByAccountId: String = "",
    val targetAccountId: String = "",
    val amountPz: Long = 0,
    val paymentMode: String = "Immediate",
    val expiresAt: String = "",
    val usedAt: String? = null,
    val transactionId: String? = null,
    val issuedByAdminRef: String? = null
)

/** Módulo Beta: Escuela de Capacitación */
enum class TrainingModuleId { Module1, Module2, Module3 }

enum class TrainingQuizStatus { Locked, Available, Completed }

data class TrainingQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class TrainingModule(
    val id: TrainingModuleId,
    val title: String,
    val lessons: List<String>,
    val quiz: List<TrainingQuestion>
)

data class TrainingProgress(
    val dip: String,
    val moduleId: TrainingModuleId,
    val status: TrainingQuizStatus = TrainingQuizStatus.Available,
    val quizCompleted: Boolean = false,
    val quizScore: Int = 0,
    val rewardClaimed: Boolean = false,
    val completedAt: String? = null
)
