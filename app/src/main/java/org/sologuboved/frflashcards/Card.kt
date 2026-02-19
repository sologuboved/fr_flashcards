package org.sologuboved.frflashcards

data class Card(
    val mot: String,
    val trad: String,
    var showingFrench: Boolean = false
)