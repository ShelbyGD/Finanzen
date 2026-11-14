package de.smartfinance.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Clean-Banking Farbpalette (Finanzguru-Style)
val HeroVioletStart = Color(0xFF4A25AA)
val HeroVioletEnd = Color(0xFF311B92)
val HeroTurquoiseAccent = Color(0xFF00E5FF)
val CleanTurquoise = Color(0xFF00ACC1)
val BudgetRingGreen = Color(0xFF10B981)
val BudgetRingOrange = Color(0xFFF59E0B)
val BudgetRingRed = Color(0xFFEF4444)

// Edle Kategorie-Akzentfarben für die runden Icon-Kreise in den Listen
val CategoryColorEmerald = Color(0xFF10B981)   // Lebensmittel & Einnahmen
val CategoryColorBlue = Color(0xFF3B82F6)      // Handyvertrag & Kommunikation
val CategoryColorAmber = Color(0xFFF59E0B)     // Strom & Energie
val CategoryColorCoralRed = Color(0xFFEF4444)  // Kfz & Tanken
val CategoryColorIndigo = Color(0xFF6366F1)    // Versicherung
val CategoryColorTurquoise = Color(0xFF06B6D4) // Sparen & Rücklagen
val CategoryColorRose = Color(0xFFEC4899)      // Freizeit & Lifestyle
val CategoryColorPurple = Color(0xFF8B5CF6)    // Wohnen & Warmmiete

private val CleanLightColors = lightColorScheme(
    primary = Color(0xFF4A25AA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF311B92),
    secondary = Color(0xFF00ACC1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF3F5F9),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE5E9F2),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFEF4444)
)

private val CleanDarkColors = darkColorScheme(
    primary = Color(0xFFB388FF),
    onPrimary = Color(0xFF1E0E4B),
    primaryContainer = Color(0xFF311B92),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3B2500),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF151C2C),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFF87171)
)

@Composable
fun SmartFinanceTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CleanDarkColors else CleanLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

object CategoryIconCatalog {
    val availableIcons: List<Pair<String, String>> = listOf(
        "shield" to "Versicherung",
        "phone" to "Handyvertrag",
        "car" to "Kfz & Mobilität",
        "cart" to "Lebensmittel",
        "home" to "Wohnen & Fixkosten",
        "savings" to "Sparen & Etappen",
        "income" to "Gehalt & Einnahmen",
        "bolt" to "Strom & Energie",
        "leisure" to "Freizeit & Gaming",
        "food" to "Restaurant",
        "cafe" to "Bäcker & Café",
        "fuel" to "Tanken",
        "health" to "Gesundheit",
        "pets" to "Haustiere",
        "star" to "Sonstiges"
    )

    fun iconForKey(key: String): ImageVector = when (key.lowercase()) {
        "shield" -> Icons.Default.Shield
        "phone" -> Icons.Default.PhoneAndroid
        "car" -> Icons.Default.DirectionsCar
        "cart" -> Icons.Default.ShoppingCart
        "home" -> Icons.Default.Home
        "savings" -> Icons.Default.Savings
        "income", "salary" -> Icons.Default.Payments
        "bolt" -> Icons.Default.Bolt
        "leisure" -> Icons.Default.SportsEsports
        "food" -> Icons.Default.Restaurant
        "cafe" -> Icons.Default.LocalCafe
        "fuel" -> Icons.Default.LocalGasStation
        "health" -> Icons.Default.LocalHospital
        "pets" -> Icons.Default.Pets
        "star" -> Icons.Default.Star
        else -> Icons.Default.AccountBalanceWallet
    }

    /**
     * Liefert für jede Kategorie / Buchung eine eigene, edle Akzentfarbe für den runden Icon-Kreis:
     * - Lebensmittel / Supermarkt / Bäcker = Smaragdgrün
     * - Handyvertrag = Blau
     * - Strom & Energie = Orange/Amber
     * - Kfz / Tanken = Korallenrot
     * - Versicherung = Indigo
     * - Sparen = Türkis
     * - Freizeit = Rose/Pink
     * - Wohnen / Miete = Violett
     */
    fun accentColorFor(iconKey: String, titleOrCategory: String = ""): Color {
        val lowerText = titleOrCategory.lowercase()
        if (lowerText.contains("strom") || lowerText.contains("energie") || lowerText.contains("glasfaser")) {
            return CategoryColorAmber
        }
        if (lowerText.contains("handy") || lowerText.contains("mobilfunk") || lowerText.contains("telekom") || lowerText.contains("vodafone")) {
            return CategoryColorBlue
        }
        if (lowerText.contains("kfz") || lowerText.contains("tank") || lowerText.contains("auto")) {
            return CategoryColorCoralRed
        }
        if (lowerText.contains("versicherung") || lowerText.contains("haftpflicht") || lowerText.contains("hausrat")) {
            return CategoryColorIndigo
        }
        if (lowerText.contains("sparen") || lowerText.contains("sparrate") || lowerText.contains("sparbooster")) {
            return CategoryColorTurquoise
        }
        if (lowerText.contains("lebensmittel") || lowerText.contains("supermarkt") || lowerText.contains("einkauf")) {
            return CategoryColorEmerald
        }
        if (lowerText.contains("freizeit") || lowerText.contains("kino") || lowerText.contains("restaurant")) {
            return CategoryColorRose
        }

        return when (iconKey.lowercase()) {
            "cart", "cafe", "income", "salary" -> CategoryColorEmerald
            "phone" -> CategoryColorBlue
            "bolt" -> CategoryColorAmber
            "car", "fuel" -> CategoryColorCoralRed
            "shield" -> CategoryColorIndigo
            "savings" -> CategoryColorTurquoise
            "leisure", "food" -> CategoryColorRose
            "home" -> CategoryColorPurple
            else -> CategoryColorIndigo
        }
    }
}
