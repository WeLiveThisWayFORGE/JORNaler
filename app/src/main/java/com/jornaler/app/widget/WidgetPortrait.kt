package com.jornaler.app.widget

/**
 * A square accent in the title row. The same rule applies at every height,
 * so a tall narrow widget does not grow a portrait header.
 * 32dp is about an eighth of a 180dp widget; 40dp is about a ninth of a 360dp one.
 */
internal fun widgetPortraitSizeDp(widthDp: Float): Int = if (widthDp < 190f) 32 else 40
