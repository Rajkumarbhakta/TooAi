package com.rkbapps.tooai.utils

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Default window. Long enough to swallow an accidental double-tap, short enough that a deliberate
 * second press still registers.
 */
const val DEFAULT_CLICK_DEBOUNCE_MS = 700L

/**
 * Drops clicks that arrive within [windowMs] of the previous accepted one.
 *
 * A single gate is shared by every action on a screen, so it also covers the case of two *different*
 * buttons being hit in quick succession — on the AI Writer that means a tone chip followed by
 * Regenerate, which starts two inference runs, not one.
 *
 * Wrapping the action rather than returning a fixed-arity handler keeps it usable for callbacks
 * that take arguments: `gate { onRunPrompt(prompt) }`.
 *
 * The timestamp is deliberately not snapshot state — a click should not trigger recomposition just
 * to record when it happened. [SystemClock.elapsedRealtime] is used rather than wall-clock time so
 * a clock change cannot freeze or widen the window.
 */
@Stable
class ClickGate(private val windowMs: Long) {

    private var lastClickAt = 0L

    operator fun invoke(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAt >= windowMs) {
            lastClickAt = now
            action()
        }
    }
}

@Composable
fun rememberClickGate(windowMs: Long = DEFAULT_CLICK_DEBOUNCE_MS): ClickGate =
    remember(windowMs) { ClickGate(windowMs) }