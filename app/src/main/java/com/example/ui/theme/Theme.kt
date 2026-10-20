package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SapphirePrimaryDark,
    onPrimary = SapphireOnPrimaryDark,
    primaryContainer = SapphireContainerDark,
    onPrimaryContainer = SapphireOnContainerDark,
    secondary = VioletSecondaryDark,
    secondaryContainer = VioletContainerDark,
    tertiary = AmberTertiaryDark,
    tertiaryContainer = AmberContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = SapphirePrimaryLight,
    onPrimary = SapphireOnPrimaryLight,
    primaryContainer = SapphireContainerLight,
    onPrimaryContainer = SapphireOnContainerLight,
    secondary = VioletSecondaryLight,
    secondaryContainer = VioletContainerLight,
    tertiary = AmberTertiaryLight,
    tertiaryContainer = AmberContainerLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
