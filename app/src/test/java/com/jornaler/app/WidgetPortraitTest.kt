package com.jornaler.app

import com.jornaler.app.widget.widgetPortraitSizeDp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPortraitTest {
    @Test
    fun narrowWidgetKeepsATinyThumbnail() {
        val size = widgetPortraitSizeDp(180f)
        assertEquals(32, size)
        assertTrue(size < 180 / 3)
    }

    @Test
    fun tallNarrowWidgetDoesNotGrowAPortraitHeader() {
        assertEquals(32, widgetPortraitSizeDp(160f))
        assertEquals(40, widgetPortraitSizeDp(200f))
    }

    @Test
    fun wideWidgetThumbnailStaysWellUnderAThird() {
        val size = widgetPortraitSizeDp(360f)
        assertEquals(40, size)
        assertTrue(size * 3 < 360)
    }
}
