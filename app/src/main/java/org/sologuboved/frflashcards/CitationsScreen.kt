package org.sologuboved.frflashcards

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sologuboved.frflashcards.ui.theme.DarkBackgroundColor
import org.sologuboved.frflashcards.ui.theme.DarkGrey
import org.sologuboved.frflashcards.ui.theme.MatrixGreen
import java.net.URL

private data class CitationEntry(
    val cit: String?,
    @SerializedName("œuvre") val oeuvre: String?
)

private val AuthorColor = Color(0xFFB6F5C8)

private sealed interface CitView {
    data object Menu : CitView

    // fromAuthor == null means the quote came from "Aléatoire"
    data class Result(
        val quote: String,
        val author: String,
        val work: String?,
        val fromAuthor: String?
    ) : CitView
}

private fun parseCitationsJson(json: String): Map<String, List<CitationEntry>> {
    val type = object : TypeToken<Map<String, List<CitationEntry>>>() {}.type
    return Gson().fromJson<Map<String, List<CitationEntry>>>(json, type) ?: emptyMap()
}

@Composable
fun CitationsScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var data by remember { mutableStateOf<Map<String, List<CitationEntry>>>(emptyMap()) }
    var view by remember { mutableStateOf<CitView>(CitView.Menu) }
    var isLoading by remember { mutableStateOf(false) }

    suspend fun load(showToast: Boolean) {
        isLoading = true
        try {
            val json = withContext(Dispatchers.IO) {
                URL("${AppConfig.CITATIONS_JSON_URL}?t=${System.currentTimeMillis()}&_=${System.nanoTime()}").readText()
            }
            data = parseCitationsJson(json)
            view = CitView.Menu
            if (showToast) {
                Toast.makeText(context, "Chargé: ${data.size} auteurs", Toast.LENGTH_SHORT).show()
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

    // Phone's Back button / gesture returns to the author list
    BackHandler(enabled = view != CitView.Menu) { view = CitView.Menu }

    val authors = data.keys.sortedBy { it.lowercase() }

    fun usableQuotes(author: String): List<CitationEntry> =
        data[author].orEmpty().filter { !it.cit.isNullOrBlank() }

    // One random quote: by the given author, or by a random author if null
    fun pick(authorFilter: String?): CitView.Result? {
        val author = authorFilter
            ?: authors.filter { usableQuotes(it).isNotEmpty() }.randomOrNull()
            ?: return null
        val entry = usableQuotes(author).randomOrNull() ?: return null
        return CitView.Result(
            quote = entry.cit.orEmpty(),
            author = author,
            work = entry.oeuvre?.takeIf { it.isNotBlank() },
            fromAuthor = authorFilter
        )
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
                horizontalArrangement = if (view == CitView.Menu) Arrangement.End else Arrangement.Start
            ) {
                val current = view
                if (current == CitView.Menu) {
                    Button(
                        onClick = { scope.launch { load(showToast = true) } },
                        enabled = !isLoading
                    ) { Text("Actualiser") }
                } else if (current is CitView.Result) {
                    Button(onClick = { pick(current.fromAuthor)?.let { view = it } }) {
                        Text("Relance")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val v = view) {
                is CitView.Menu -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Button(
                            onClick = { pick(null)?.let { view = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Aléatoire", fontSize = 18.sp) }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    items(authors) { author ->
                        Button(
                            onClick = { pick(author)?.let { view = it } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkGrey,
                                contentColor = MatrixGreen
                            )
                        ) { Text(author, fontSize = 18.sp) }
                    }
                }

                is CitView.Result -> SelectionContainer {
                    Column {
                        Text(v.quote, color = MatrixGreen, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (v.work != null) "${v.author} - ${v.work}" else v.author,
                            color = AuthorColor,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}