package com.jornaler.app

object NotebookIntents {
    const val EXTRA_NAV = "nav"
    const val EXTRA_ENTRY_ID = "entry_id"
    const val EXTRA_SEED = "seed"
    const val NAV_OPEN = "open"
    const val NAV_QUICK = "quick"
    const val NAV_TODAY = "today"
}

/** Sample pages are a debug aid. A release build never inserts them. */
fun shouldSeedSamplePages(debuggable: Boolean, requested: Boolean): Boolean =
    debuggable && requested
