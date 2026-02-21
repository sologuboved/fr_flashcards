package org.sologuboved.frflashcards.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MatrixDarkScheme = darkColorScheme(
    primary = MatrixGreen,
    secondary = DarkGreen,
    tertiary = GreenGlow,
    background = DarkBackgroundColor,
    surface = DarkGrey,
    onPrimary = DarkBackgroundColor,
    onSecondary = DarkBackgroundColor,
    onBackground = MatrixGreen,
    onSurface = MatrixGreen
)

@Composable
fun FrflashcardsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MatrixDarkScheme,
        typography = Typography,
        content = content
    )
}
