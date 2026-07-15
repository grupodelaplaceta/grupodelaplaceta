package org.laplaceta.placetajunior.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.laplaceta.placetajunior.R

// ══════════════════════════════════════════════════════════════════
//  PALETA PLACETA JUNIOR — Colores planos P-J-U-N-I-O-R
// ══════════════════════════════════════════════════════════════════
//  P (Negro):     #000000
//  J (Rojo):      #FF3333
//  U (Naranja):   #FF6600
//  N (Amarillo):  #D6CE52
//  I (Verde):     #336E45
//  O (Azul):      #3A00E1
//  R (Púrpura):   #4E3B70
// ══════════════════════════════════════════════════════════════════

// Colores planos del logo
val PJBlack = Color(0xFF000000)
val PJRed = Color(0xFFFF3333)
val PJOrange = Color(0xFFFF6600)
val PJYellow = Color(0xFFD6CE52)
val PJGreen = Color(0xFF336E45)
val PJBlue = Color(0xFF3A00E1)
val PJPurple = Color(0xFF4E3B70)

// Variantes claras (con alpha 12%)
val PJRedLight = Color(0xFFFF3333).copy(alpha = 0.12f)
val PJOrangeLight = Color(0xFFFF6600).copy(alpha = 0.12f)
val PJYellowLight = Color(0xFFD6CE52).copy(alpha = 0.20f)
val PJGreenLight = Color(0xFF336E45).copy(alpha = 0.12f)
val PJBlueLight = Color(0xFF3A00E1).copy(alpha = 0.12f)
val PJPurpleLight = Color(0xFF4E3B70).copy(alpha = 0.12f)

// Fondos
val PJWhite = Color(0xFFFFFFFF)
val PJGray50 = Color(0xFFF9FAFB)
val PJGray100 = Color(0xFFF3F4F6)
val PJGray200 = Color(0xFFE5E7EB)
val PJGray300 = Color(0xFFD1D5DB)
val PJGray400 = Color(0xFF9CA3AF)
val PJGray500 = Color(0xFF6B7280)
val PJGray600 = Color(0xFF4B5563)
val PJGray700 = Color(0xFF374151)
val PJGray800 = Color(0xFF1F2937)
val PJGray900 = Color(0xFF111827)

val PJShadow = Color(0x1A000000)
val PJShadowMd = Color(0x33000000)

// Parental control colors (flat)
val PJParentalWarning = PJRedLight
val PJParentalBorder = PJRed.copy(alpha = 0.3f)

// Fondos planos
val PJOrangeBg = PJWhite
val PJYellowBg = PJYellowLight
val PJPurpleBg = PJPurpleLight
val PJGreenBg = PJGreenLight
val PJBlueBg = PJBlueLight
val PJRedBg = PJRedLight

// ══════════════════════════════════════════════════════════════════
//  TIPOGRAFÍA — Outfit + Handly Casual
// ══════════════════════════════════════════════════════════════════

val OutfitRegular = FontFamily(Font(R.font.outfit_regular, FontWeight.Normal))
val OutfitMedium = FontFamily(Font(R.font.outfit_medium, FontWeight.Medium))
val OutfitBold = FontFamily(Font(R.font.outfit_bold, FontWeight.Bold))
val HandlyCasual = FontFamily(Font(R.font.handly_casual, FontWeight.Normal))

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = OutfitBold, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = PJBlack),
    displayMedium = TextStyle(fontFamily = OutfitBold, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = PJBlack),
    headlineLarge = TextStyle(fontFamily = OutfitBold, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = PJBlack),
    headlineMedium = TextStyle(fontFamily = OutfitBold, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = PJBlack),
    headlineSmall = TextStyle(fontFamily = OutfitBold, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = PJBlack),
    titleLarge = TextStyle(fontFamily = OutfitBold, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = PJBlack),
    titleMedium = TextStyle(fontFamily = OutfitMedium, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = PJBlack),
    titleSmall = TextStyle(fontFamily = OutfitMedium, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = PJBlack),
    bodyLarge = TextStyle(fontFamily = OutfitRegular, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = PJBlack),
    bodyMedium = TextStyle(fontFamily = OutfitRegular, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = PJGray700),
    bodySmall = TextStyle(fontFamily = OutfitRegular, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = PJGray500),
    labelLarge = TextStyle(fontFamily = OutfitMedium, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = PJBlack),
    labelMedium = TextStyle(fontFamily = OutfitMedium, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = PJGray600),
    labelSmall = TextStyle(fontFamily = OutfitRegular, fontWeight = FontWeight.Normal, fontSize = 10.sp, color = PJGray400)
)
