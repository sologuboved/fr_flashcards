package org.sologuboved.frflashcards

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sologuboved.frflashcards.ui.theme.DarkBackgroundColor
import org.sologuboved.frflashcards.ui.theme.DarkGrey
import org.sologuboved.frflashcards.ui.theme.MatrixGreen
import java.net.URL

private data class CollocationEntry(val mot: String?, val trad: String?)
private val TranslationColor = Color(0xFFB6F5C8)
private val TagColor = Color(0xFF8AB4F8)

private sealed interface CollView {
    data object Menu : CollView
    data class Tag(val name: String) : CollView
    data class Random(val lines: List<AnnotatedString>) : CollView
}

private fun parseCollocationsJson(json: String): Map<String, List<CollocationEntry>> {
    val type = object : TypeToken<Map<String, List<CollocationEntry>>>() {}.type
    return Gson().fromJson<Map<String, List<CollocationEntry>>>(json, type) ?: emptyMap()
}

// "mot trad", or just "mot" when there is no translation
// French part in the default green, translation in TranslationColor, no tilde
private fun CollocationEntry.styled(tag: String? = null): AnnotatedString = buildAnnotatedString {
    append(mot.orEmpty())
    if (!trad.isNullOrBlank()) {
        append(" ")
        withStyle(SpanStyle(color = TranslationColor)) { append(trad) }
    }
    if (tag != null) {
        append(" ")
        withStyle(SpanStyle(color = TagColor)) { append("#$tag") }
    }
}

@Composable
fun CollocationsScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var data by remember { mutableStateOf<Map<String, List<CollocationEntry>>>(emptyMap()) }
    var view by remember { mutableStateOf<CollView>(CollView.Menu) }
    var isLoading by remember { mutableStateOf(false) }

    suspend fun load(showToast: Boolean) {
        isLoading = true
        try {
            val json = withContext(Dispatchers.IO) {
                URL("${AppConfig.COLLOCATIONS_JSON_URL}?t=${System.currentTimeMillis()}&_=${System.nanoTime()}").readText()
            }
            data = parseCollocationsJson(json)
            view = CollView.Menu
            if (showToast) {
                Toast.makeText(context, "Chargé: ${data.size} étiquettes", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            if (showToast) {
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load(showToast = false) }

    // Phone's own "back" gesture also acts as "retour"
    BackHandler(enabled = view != CollView.Menu) { view = CollView.Menu }

    val tags = data.keys.sortedBy { it.lowercase() }

    fun usableEntries(tag: String): List<CollocationEntry> =
        data[tag].orEmpty().filter { !it.mot.isNullOrBlank() }

    fun randomLines(): List<AnnotatedString> = tags.mapNotNull { tag ->
        usableEntries(tag).randomOrNull()?.styled(tag)
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
                horizontalArrangement = if (view == CollView.Menu) Arrangement.End else Arrangement.Start
            ) {
                if (view == CollView.Menu) {
                    Button(
                        onClick = { scope.launch { load(showToast = true) } },
                        enabled = !isLoading
                    ) { Text("Actualiser") }
                } else {
                    Button(onClick = { view = CollView.Menu }) { Text("retour") }
                    if (view is CollView.Random) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { view = CollView.Random(randomLines()) }) {
                            Text("relance")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val v = view) {
                is CollView.Menu -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(tags) { tag ->
                        Button(
                            onClick = { view = CollView.Tag(tag) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkGrey,
                                contentColor = MatrixGreen
                            )
                        ) { Text(tag, fontSize = 18.sp) }
                    }
                    item {
                        // Default colors = same green as "Actualiser"
                        Button(
                            onClick = { view = CollView.Random(randomLines()) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("aléatoire", fontSize = 18.sp) }
                    }
                }

                is CollView.Tag -> {
                    val entries = usableEntries(v.name)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item { Text("Étiquette sélectionnée : ${v.name}", color = MatrixGreen, fontSize = 18.sp) }
                        item { Text("-${entries.size}-", color = MatrixGreen, fontSize = 18.sp) }
                        items(entries) { Text(it.styled(), color = MatrixGreen, fontSize = 16.sp) }
                    }
                }

                is CollView.Random -> key(v) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(v.lines) { Text(it, color = MatrixGreen, fontSize = 16.sp) }
                    }
                }
            }
        }
    }
}