package org.sologuboved.frflashcards

data class Card(
    val front: String,
    val back: String,
    var showingFront: Boolean = false
)