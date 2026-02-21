package org.sologuboved.frflashcards

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sologuboved.frflashcards.ui.theme.BlackRussian
import org.sologuboved.frflashcards.ui.theme.FrflashcardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    fun applyDefaultModeToAll(currentCards: List<Card>): List<Card> {
        // Set every card’s showingFrench to match the global mode
        return currentCards.map { it.copy(showingFrench = showFrenchDefault) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackRussian)   // full-screen black, no frame
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()      // Clears status bar (time, battery)
            .navigationBarsPadding()  // Clears bottom nav/gesture area
            .padding(horizontal = 16.dp)  // Side padding only
        ) {
            // Top buttons
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
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

                Button(
                    onClick = {
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
                        onClick = {
                            // Toggle only this car]]d’s state
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
fun CardItem(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}
