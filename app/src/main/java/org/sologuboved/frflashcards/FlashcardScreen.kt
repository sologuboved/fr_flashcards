package org.sologuboved.frflashcards

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sologuboved.frflashcards.ui.theme.DarkBackgroundColor
import org.sologuboved.frflashcards.ui.theme.DarkGrey
import org.sologuboved.frflashcards.ui.theme.FrenchColor
import org.sologuboved.frflashcards.ui.theme.MatrixGreen
import java.net.URL

@Composable
fun FlashcardScreen(
    jsonUrl: String,
    parseJson: (String) -> List<Card>,
    showLanguageToggle: Boolean
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var cards by remember { mutableStateOf(listOf<Card>()) }
    var showFrontDefault by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    suspend fun load(showToast: Boolean) {
        isLoading = true
        try {
            val json = withContext(Dispatchers.IO) {
                URL("$jsonUrl?t=${System.currentTimeMillis()}&_=${System.nanoTime()}").readText()
            }
            val loaded = parseJson(json)
            showFrontDefault = false
            cards = loaded.map { it.copy(showingFront = false) }
            if (showToast) {
                Toast.makeText(context, "Chargé: ${cards.size} cartes", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            if (showToast) {
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(jsonUrl) {
        load(showToast = false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showLanguageToggle) {
                    Button(
                        onClick = {
                            showFrontDefault = !showFrontDefault
                            cards = cards.map { it.copy(showingFront = showFrontDefault) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (showFrontDefault) "Traductions" else "Français",
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = { cards = cards.shuffled() },
                    modifier = if (!showLanguageToggle) Modifier.weight(1f) else Modifier
                ) {
                    Text("Mélanger")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { scope.launch { load(showToast = true) } },
                    enabled = !isLoading
                ) {
                    Text("Actualiser")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                itemsIndexed(cards) { index, card ->
                    CardItem(
                        text = if (card.showingFront) card.front else card.back,
                        isShowingFront = card.showingFront,
                        onClick = {
                            cards = cards.toMutableList().also { list ->
                                val current = list[index]
                                list[index] = current.copy(showingFront = !current.showingFront)
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
    isShowingFront: Boolean,
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
            containerColor = if (isShowingFront) FrenchColor else DarkGrey,
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
