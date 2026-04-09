package com.nexa.offlineai.coreui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexa.offlineai.coreui.R
import com.nexa.offlineai.domain.model.AccentOption

@Immutable
data class NexaExtraColors(
    val gradient: Brush,
    val cardGradient: Brush,
    val success: Color,
    val warning: Color,
)

val LocalNexaExtraColors = staticCompositionLocalOf {
    NexaExtraColors(
        gradient = Brush.linearGradient(listOf(Color(0xFF0E172B), Color(0xFF182B45))),
        cardGradient = Brush.linearGradient(listOf(Color(0x991E3556), Color(0x99233146))),
        success = Color(0xFF78F2C0),
        warning = Color(0xFFFFC86E),
    )
}

private val shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(40.dp),
)

private fun googleProvider(context: Context) = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun typography(context: Context): Typography {
    val titleFamily = FontFamily(
        Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = googleProvider(context), weight = FontWeight.SemiBold),
        Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = googleProvider(context), weight = FontWeight.Bold),
    )
    val bodyFamily = FontFamily(
        Font(googleFont = GoogleFont("Manrope"), fontProvider = googleProvider(context), weight = FontWeight.Medium),
        Font(googleFont = GoogleFont("Manrope"), fontProvider = googleProvider(context), weight = FontWeight.Normal),
    )
    return Typography(
        headlineLarge = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = titleFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
        bodyLarge = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    )
}

private fun accentColors(accentOption: AccentOption, darkTheme: Boolean): Pair<ColorScheme, NexaExtraColors> {
    val accent = when (accentOption) {
        AccentOption.AURORA -> Color(0xFF22C7D7)
        AccentOption.AMBER -> Color(0xFFFFB454)
        AccentOption.TIDAL -> Color(0xFF67E8F9)
        AccentOption.ROSEWOOD -> Color(0xFFF07892)
    }
    val baseScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            secondary = accent.copy(alpha = 0.82f),
            tertiary = Color(0xFF8AA8FF),
            background = Color(0xFF09101C),
            surface = Color(0xFF0F1726),
            surfaceVariant = Color(0xFF17243A),
            onPrimary = Color(0xFF031419),
            onBackground = Color(0xFFF4F7FB),
            onSurface = Color(0xFFF4F7FB),
            outline = Color(0xFF50617D),
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = accent.copy(alpha = 0.82f),
            tertiary = Color(0xFF3C63D2),
            background = Color(0xFFF2F6FB),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFDCE7F4),
            onPrimary = Color.White,
            onBackground = Color(0xFF0B1324),
            onSurface = Color(0xFF0B1324),
            outline = Color(0xFF7788A3),
        )
    }
    val extras = NexaExtraColors(
        gradient = Brush.linearGradient(
            listOf(
                if (darkTheme) Color(0xFF08111F) else Color(0xFFEAF2FB),
                if (darkTheme) accent.copy(alpha = 0.22f) else accent.copy(alpha = 0.18f),
                if (darkTheme) Color(0xFF162743) else Color(0xFFF8FBFF),
            ),
        ),
        cardGradient = Brush.linearGradient(
            listOf(
                accent.copy(alpha = if (darkTheme) 0.14f else 0.18f),
                baseScheme.surfaceVariant.copy(alpha = 0.88f),
            ),
        ),
        success = Color(0xFF7DECB5),
        warning = Color(0xFFFFC76D),
    )
    return baseScheme to extras
}

@Composable
fun NexaTheme(
    accentOption: AccentOption = AccentOption.AURORA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val (scheme, extras) = accentColors(accentOption, darkTheme)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalNexaExtraColors provides extras,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography(context),
            shapes = shapes,
            content = content,
        )
    }
}
