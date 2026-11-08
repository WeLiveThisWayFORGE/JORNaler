package com.jornaler.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jornaler.app.MainActivity
import com.jornaler.app.NotebookIntents
import com.jornaler.app.domain.Formatting
import com.jornaler.app.domain.Library
import com.jornaler.app.domain.Shelf
import com.jornaler.app.domain.WritingStats
import com.jornaler.app.jornalerGraph
import com.jornaler.app.model.Entry

private val Paper = ColorProvider(day = Color(0xFFF6EFE4), night = Color(0xFF221C18))
private val Card = ColorProvider(day = Color(0xFFFFFBF6), night = Color(0xFF322A25))
private val Ink = ColorProvider(day = Color(0xFF2B2118), night = Color(0xFFF3E7DA))
private val Muted = ColorProvider(day = Color(0xFF7A6556), night = Color(0xFFD2C0B0))
private val Accent = ColorProvider(day = Color(0xFF8C4A32), night = Color(0xFFE7B9A4))
private val OnAccent = ColorProvider(day = Color(0xFFFFF8F3), night = Color(0xFF3F2414))
private val Sage = ColorProvider(day = Color(0xFF3E6244), night = Color(0xFFB7D0AE))

private fun pageIntent(nav: String? = null, entryId: String? = null): Intent =
    Intent().apply {
        setClassName("com.jornaler.app", MainActivity::class.java.name)
        if (nav != null) putExtra(NotebookIntents.EXTRA_NAV, nav)
        if (entryId != null) putExtra(NotebookIntents.EXTRA_ENTRY_ID, entryId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

private fun openApp() = actionStartActivity(pageIntent())

private fun quickAdd() = actionStartActivity(pageIntent(NotebookIntents.NAV_QUICK))

private fun openToday() = actionStartActivity(pageIntent(NotebookIntents.NAV_TODAY))

private fun openEntry(id: String) = actionStartActivity(pageIntent(NotebookIntents.NAV_OPEN, id))

class JornalerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pages = Library.visible(
            jornalerGraph(context).repository.snapshot(),
            Shelf.IN_PROGRESS,
            "",
        )
        provideContent {
            WidgetContent(pages)
        }
    }
}

@Composable
private fun WidgetContent(pages: List<Entry>) {
    val compact = LocalSize.current.width < 190.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Paper)
            .cornerRadius(24.dp)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Jornaler",
                style = TextStyle(
                    color = Ink,
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                ),
                modifier = GlanceModifier.clickable(openApp()),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (!compact) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(14.dp)
                        .background(Card)
                        .clickable(openToday())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Today",
                        style = TextStyle(color = Sage, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
            Box(
                modifier = GlanceModifier
                    .size(if (compact) 36.dp else 40.dp)
                    .cornerRadius(20.dp)
                    .background(Accent)
                    .clickable(quickAdd()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = OnAccent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "In progress",
            style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (pages.isEmpty()) {
            Text(
                text = "Nothing in progress",
                style = TextStyle(color = Ink, fontSize = 15.sp, fontFamily = FontFamily.Serif),
                modifier = GlanceModifier.padding(top = 8.dp),
            )
            Text(
                text = "Tap + to catch a spark.",
                style = TextStyle(color = Muted, fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 2.dp),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(pages, itemId = { entry -> stableId(entry.id) }) { entry ->
                    WidgetCard(entry)
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(entry: Entry) {
    val title = Formatting.cardTitle(entry)
    val detail = widgetDetail(entry)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .cornerRadius(16.dp)
            .background(Card)
            .clickable(openEntry(entry.id))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = Formatting.indicator(entry),
            style = TextStyle(color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        Text(
            text = title,
            style = TextStyle(
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
            ),
            maxLines = 1,
        )
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                style = TextStyle(color = Muted, fontSize = 13.sp),
                maxLines = 1,
            )
        }
    }
}

private fun widgetDetail(entry: Entry): String {
    val first = WritingStats.firstContentLine(entry.body)
    if (first.isBlank()) return ""
    if (entry.title.isBlank()) return ""
    if (first.equals(entry.title.trim(), ignoreCase = true)) return ""
    return first
}

private fun stableId(id: String): Long = id.fold(0L) { acc, c -> acc * 31 + c.code }
