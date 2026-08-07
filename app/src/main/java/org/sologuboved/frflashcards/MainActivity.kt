package org.sologuboved.frflashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.sologuboved.frflashcards.ui.theme.DarkBackgroundColor
import org.sologuboved.frflashcards.ui.theme.FrflashcardsTheme
import org.sologuboved.frflashcards.ui.theme.MatrixGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.navigationBarColor = DarkBackgroundColor.toArgb()
        window.statusBarColor = DarkBackgroundColor.toArgb()

        setContent {
            FrflashcardsTheme {
                AppPager()
            }
        }
    }
}

@Composable
fun AppPager() {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Box(modifier = Modifier.fillMaxSize().background(DarkBackgroundColor)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> FlashcardScreen(
                    jsonUrl = AppConfig.MAIN_JSON_URL,
                    parseJson = ::parseMotTradJson,
                    showLanguageToggle = true
                )
                1 -> FlashcardScreen(
                    jsonUrl = AppConfig.COI_JSON_URL,
                    parseJson = ::parseVerbePrepositionJson,
                    showLanguageToggle = false
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            repeat(2) { index ->
                Text(
                    text = if (pagerState.currentPage == index) "●" else "○",
                    color = MatrixGreen,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
