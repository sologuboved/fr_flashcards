package org.sologuboved.frflashcards

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URL
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sologuboved.frflashcards.ui.theme.DarkBackgroundColor
import org.sologuboved.frflashcards.ui.theme.DarkGrey
import org.sologuboved.frflashcards.ui.theme.FrenchColor
import org.sologuboved.frflashcards.ui.theme.FrflashcardsTheme
import org.sologuboved.frflashcards.ui.theme.MatrixGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OxygenOS 15 + OnePlus fix
        window.navigationBarColor = DarkBackgroundColor.toArgb()  // Ignore deprecation warning
        window.statusBarColor = DarkBackgroundColor.toArgb()

        setContent {
            FrflashcardsTheme {
                FlashcardApp()
            }
        }
    }
}

@Composable
fun FlashcardApp() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // List of cards loaded from GitHub
    var cards by remember { mutableStateOf(listOf<Card>()) }

    // false -> default side is translation, true -> default is French
    var showFrenchDefault by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val json = withContext(Dispatchers.IO) {
                    URL("${AppConfig.JSON_URL}?t=${System.currentTimeMillis()}&_=${System.nanoTime()}").readText()
                }
                val gson = Gson()
                val type = object : TypeToken<List<Card>>() {}.type
                val loaded: List<Card> = gson.fromJson(json, type) ?: emptyList()

                showFrenchDefault = false
                cards = loaded.map { it.copy(showingFrench = false) }
            } catch (e: Exception) {
                // Silent fail on launch (user can tap Actualiser)
            } finally {
                isLoading = false
            }
        }
    }

    fun applyDefaultModeToAll(currentCards: List<Card>): List<Card> {
        // Set every card’s showingFrench to match the global mode
        return currentCards.map { it.copy(showingFrench = showFrenchDefault) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundColor)  // Black behind nav bar
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()      // Only ONCE, here
                .navigationBarsPadding()  // Only ONCE, here
        ) {
            // Top buttons
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(  // Français/Traductions
                    onClick = {
                        // Toggle global mode and apply to all
                        showFrenchDefault = !showFrenchDefault
                        cards = applyDefaultModeToAll(cards)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (showFrenchDefault) "Traductions" else "Français",
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(  // ← NEW Mélanger
                    onClick = {
                        cards = cards.shuffled()  // Randomize order instantly!
                    }
                ) {
                    Text("Mélanger")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {  // Actualiser
                        scope.launch {
                            isLoading = true
                            try {
                                val json = withContext(Dispatchers.IO) {
                                    URL("${AppConfig.JSON_URL}?t=${System.currentTimeMillis()}&_=${System.nanoTime()}").readText()

                                }
                                val gson = Gson()
                                val type = object : TypeToken<List<Card>>() {}.type
                                val loaded: List<Card> = gson.fromJson(json, type)

                                // After refresh: show translations by default
                                showFrenchDefault = false
                                cards = loaded.map { it.copy(showingFrench = false) }

                                Toast.makeText(
                                    context,
                                    "Chargé: ${cards.size} cartes",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Erreur: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Actualiser")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cards list
            LazyColumn {
                itemsIndexed(cards) { index, card ->
                    CardItem(
                        text = if (card.showingFrench) card.mot else card.trad,
                        isShowingFrench = card.showingFrench,
                        onClick = {
                            // Toggle only this card’s state
                            cards = cards.toMutableList().also { list ->
                                val current = list[index]
                                list[index] = current.copy(showingFrench = !current.showingFrench)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CardItem(
    text: String,
    isShowingFrench: Boolean,  // NEW PARAMETER
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isShowingFrench) FrenchColor else DarkGrey,  // ← mot=FrenchColor, trad=DarkGrey
            contentColor = MatrixGreen
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp,
            textAlign = TextAlign.Start,
            color = MatrixGreen
        )
    }
}
