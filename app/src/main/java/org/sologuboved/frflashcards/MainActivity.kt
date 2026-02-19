package org.sologuboved.frflashcards

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlashcardApp()
        }
    }
}

@Composable
fun FlashcardApp() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    var showFrenchDefault by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        showFrenchDefault = !showFrenchDefault
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
                                    URL(AppConfig.JSON_URL).readText()
                                }
                                val gson = Gson()
                                val type = object : TypeToken<List<Card>>() {}.type
                                cards = gson.fromJson(json, type)
                                showFrenchDefault = false
                                Toast.makeText(context, "Chargé: ${cards.size} cartes", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
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
                    var showingFrench by remember { mutableStateOf(card.showingFrench) }

                    CardItem(
                        text = if (showingFrench) card.mot else card.trad,
                        onClick = {
                            showingFrench = !showingFrench
                            cards = cards.toMutableList().also { it[index].showingFrench = showingFrench }
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
