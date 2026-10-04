package com.example.sequencelab.ui

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Returns a light "click" haptic that does nothing when haptics are disabled. */
@Composable
fun rememberHaptic(enabled: Boolean): () -> Unit {
    val view = LocalView.current
    return remember(view, enabled) {
        { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) }
    }
}
