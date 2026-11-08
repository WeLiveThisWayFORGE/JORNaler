package com.jornaler.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jornaler.app.R

private fun variable(
    resId: Int,
    weight: FontWeight,
    style: FontStyle = FontStyle.Normal,
    settings: FontVariation.Settings = FontVariation.Settings(FontVariation.weight(weight.weight)),
): Font = Font(resId = resId, weight = weight, style = style, variationSettings = settings)

private val Literata = FontFamily(
    variable(R.font.literata, FontWeight.Normal, settings = reading(400)),
    variable(R.font.literata, FontWeight.Medium, settings = reading(500)),
    variable(R.font.literata, FontWeight.SemiBold, settings = reading(600)),
    variable(R.font.literata, FontWeight.Bold, settings = reading(700)),
    variable(R.font.literata_italic, FontWeight.Normal, FontStyle.Italic, reading(400)),
    variable(R.font.literata_italic, FontWeight.SemiBold, FontStyle.Italic, reading(600)),
)

private val Nunito = FontFamily(
    variable(R.font.nunito, FontWeight.Normal),
    variable(R.font.nunito, FontWeight.Medium),
    variable(R.font.nunito, FontWeight.SemiBold),
    variable(R.font.nunito, FontWeight.Bold),
    variable(R.font.nunito_italic, FontWeight.Normal, FontStyle.Italic),
    variable(R.font.nunito_italic, FontWeight.Medium, FontStyle.Italic),
)

val FrauncesDisplay = FontFamily(
    variable(R.font.fraunces, FontWeight.Medium, settings = display(560)),
    variable(R.font.fraunces, FontWeight.SemiBold, settings = display(640)),
    variable(R.font.fraunces_italic, FontWeight.Medium, FontStyle.Italic, display(560)),
)

private fun reading(weight: Int) = FontVariation.Settings(
    FontVariation.weight(weight),
    FontVariation.Setting("opsz", 20f),
)

private fun display(weight: Int) = FontVariation.Settings(
    FontVariation.weight(weight),
    FontVariation.Setting("opsz", 72f),
    FontVariation.Setting("SOFT", 28f),
    FontVariation.Setting("WONK", 0.35f),
)

val JornalerTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 42.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 30.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

val WritingTitleStyle = TextStyle(
    fontFamily = Literata,
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
)

val WritingBodyStyle = TextStyle(
    fontFamily = Literata,
    fontWeight = FontWeight.Normal,
    fontSize = 19.sp,
    lineHeight = 32.sp,
)
