package com.jornaler.app

import com.jornaler.app.widget.WidgetPortraitPlacement
import com.jornaler.app.widget.widgetPortraitSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPortraitTest {
    @Test
    fun tallNarrowWidgetPutsThePortraitAboveTheControls() {
        val slot = widgetPortraitSlot(widthDp = 200f, heightDp = 280f)
        assertEquals(WidgetPortraitPlacement.Banner, slot.placement)
        assertTrue(slot.breadthDp in 80..110)
    }

    @Test
    fun wideWidgetPutsThePortraitBesideTheControls() {
        val slot = widgetPortraitSlot(widthDp = 360f, heightDp = 220f)
        assertEquals(WidgetPortraitPlacement.Side, slot.placement)
        assertEquals(120, slot.breadthDp)
    }

    @Test
    fun shortWidgetKeepsASideStripSoTheControlsStillFit() {
        val slot = widgetPortraitSlot(widthDp = 180f, heightDp = 110f)
        assertEquals(WidgetPortraitPlacement.Side, slot.placement)
        assertTrue(slot.breadthDp <= 72)
    }
}
