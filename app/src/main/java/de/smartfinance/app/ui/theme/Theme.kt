package de.smartfinance.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

val FinanceEmerald = Color(0xFF10B981)
val FinanceCyan = Color(0xFF06B6D4)
val FinanceAmber = Color(0xFFF59E0B)
val FinanceCoral = Color(0xFFEF4444)
val FinanceDeepNavy = Color(0xFF0B192C)
val FinanceDarkSurface = Color(0xFF13243A)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF032B1E),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF06283D),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3B2500),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = FinanceDeepNavy,
    onBackground = Color(0xFFE2E8F0),
    surface = FinanceDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E3452),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFF87171)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    error = Color(0xFFDC2626)
)

@Composable
fun SmartFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

object CategoryIconCatalog {
    val availableIcons: List<Pair<String, String>> = listOf(
        "shield" to "🛡️ Versicherung",
        "phone" to "📱 Handy / Mobilfunk",
        "car" to "🚗 Kfz & Mobilität",
        "cart" to "🛒 Lebensmittel & Einkauf",
        "home" to "🏠 Wohnen & Fixkosten",
        "savings" to "💎 Sparen & Etappen",
        "income" to "💶 Gehalt & Einnahmen",
        "bolt" to "⚡ Energie & Verträge",
        "medical" to "🩺 Gesundheit",
        "leisure" to "🎮 Freizeit & Streaming",
        "pet" to "🐾 Haustier",
        "star" to "⭐ Sonstiges"
    )

    fun iconForKey(key: String): ImageVector = when (key.lowercase()) {
        "shield" -> Icons.Default.Shield
        "phone" -> Icons.Default.PhoneAndroid
        "car" -> Icons.Default.DirectionsCar
        "cart" -> Icons.Default.ShoppingCart
        "home" -> Icons.Default.Home
        "savings" -> Icons.Default.Savings
        "income" -> Icons.Default.Payments
        "bolt" -> Icons.Default.Bolt
        "medical" -> Icons.Default.LocalHospital
        "leisure" -> Icons.Default.SportsEsports
        "pet" -> Icons.Default.Pets
        "star" -> Icons.Default.Star
        else -> Icons.Default.AccountBalanceWallet
    }
}
