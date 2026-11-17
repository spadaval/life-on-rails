package com.spadaval.lor.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.spadaval.lor.homecore.R

@OptIn(ExperimentalTextApi::class)
private fun saira(weight: Int) = Font(R.font.saira, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

actual val Saira: FontFamily = FontFamily(saira(400), saira(500), saira(600))
actual val SairaCondensed: FontFamily = FontFamily(
    Font(R.font.saira_condensed_medium, FontWeight.Medium),
    Font(R.font.saira_condensed_semibold, FontWeight.SemiBold),
)
