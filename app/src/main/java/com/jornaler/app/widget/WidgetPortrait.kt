package com.jornaler.app.widget

internal enum class WidgetPortraitPlacement {
    /** Narrow and tall: the painting sits above the controls. */
    Banner,

    /** Wide, or too short for a banner: the painting sits beside the controls. */
    Side,
}

internal data class WidgetPortraitSlot(
    val placement: WidgetPortraitPlacement,
    val breadthDp: Int,
)

/**
 * Picks a portrait slot that leaves room for the title, Today, and +.
 * A short widget uses a side strip so the controls are not pushed off.
 */
internal fun widgetPortraitSlot(widthDp: Float, heightDp: Float): WidgetPortraitSlot {
    val narrow = widthDp < 280f
    val short = heightDp < 150f
    if (narrow && !short) {
        // About 2.15:1, matching the prepared banner crop, and never more
        // than about a third of the widget so the controls stay put.
        val banner = (widthDp / 2.15f).toInt().coerceIn(64, 120)
        val limited = minOf(banner, (heightDp * 0.38f).toInt()).coerceAtLeast(56)
        return WidgetPortraitSlot(WidgetPortraitPlacement.Banner, limited)
    }
    val side = when {
        widthDp < 200f -> 56
        widthDp < 320f -> 88
        else -> 120
    }
    return WidgetPortraitSlot(WidgetPortraitPlacement.Side, side)
}
