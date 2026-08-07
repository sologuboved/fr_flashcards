package org.sologuboved.frflashcards

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

private data class MotTradEntry(val mot: String, val trad: String)

private data class VerbePrepositionEntry(
    val verbe: String,
    @SerializedName("préposition") val preposition: String
)

fun parseMotTradJson(json: String): List<Card> {
    val type = object : TypeToken<List<MotTradEntry>>() {}.type
    val entries: List<MotTradEntry> = Gson().fromJson(json, type) ?: emptyList()
    return entries.map { Card(front = it.mot, back = it.trad) }
}

fun parseVerbePrepositionJson(json: String): List<Card> {
    val type = object : TypeToken<List<VerbePrepositionEntry>>() {}.type
    val entries: List<VerbePrepositionEntry> = Gson().fromJson(json, type) ?: emptyList()
    return entries.map { Card(front = it.preposition, back = it.verbe) }
}
