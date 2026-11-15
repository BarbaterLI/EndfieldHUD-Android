package com.endfieldhud.android

import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Owns the overlay window: a single ComposeView added to WindowManager that
 * switches between the full HUD panel and a small bubble.
 */
class HudOverlay(
    private val service: HudService,
    private val scope: CoroutineScope,
) {
    private val windowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val owners = OverlayLifecycleOwner()
    private val composeView: ComposeView
    private var params: WindowManager.LayoutParams
    private var attached = false
    private var collectJob: kotlinx.coroutines.Job? = null

    private val density = service.resources.displayMetrics.density
    private val screenWidth = service.resources.displayMetrics.widthPixels
    private val screenHeight = service.resources.displayMetrics.heightPixels

    init {
        composeView = ComposeView(service).apply {
            this.setViewTreeLifecycleOwner(owners)
            this.setViewTreeSavedStateRegistryOwner(owners)
            setContent {
                EndfieldTheme {
                    OverlayRoot(
                        onDrag = ::onDrag,
                        onCollapse = {
                            HudState.collapsed.value = true
                        },
                        onExpand = {
                            HudState.collapsed.value = false
                        },
                    )
                }
            }
        }

        val panelWidthPx = (340 * density).toInt()
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            windowFlags(collapsed = false),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - panelWidthPx - (16 * density).toInt()).coerceAtLeast(0)
            y = (90 * density).toInt()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
    }

    private fun windowFlags(collapsed: Boolean): Int {
        return if (collapsed) {
            // bubble: not focusable, ignore outside touches
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            // panel: focusable so text fields + IME work; touches outside pass through
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
    }

    fun attach() {
        if (attached) return
        owners.performRestore()
        owners.dispatchEvent(Lifecycle.Event.ON_CREATE)
        windowManager.addView(composeView, params)
        attached = true
        owners.dispatchEvent(Lifecycle.Event.ON_START)
        owners.dispatchEvent(Lifecycle.Event.ON_RESUME)

        collectJob = scope.launch {
            HudState.collapsed.collect { collapsed ->
                applyWindowMode(collapsed)
            }
        }
    }

    private fun applyWindowMode(collapsed: Boolean) {
        if (!attached) return
        params.flags = windowFlags(collapsed)
        windowManager.updateViewLayout(composeView, params)
    }

    fun onDrag(dx: Float, dy: Float) {
        if (!attached) return
        val d = density
        params.x = (params.x + dx / 1f).toInt().coerceIn(-(16 * d).toInt(), screenWidth - (16 * d).toInt())
        params.y = (params.y + dy / 1f).toInt().coerceIn((24 * d).toInt(), screenHeight - (48 * d).toInt())
        windowManager.updateViewLayout(composeView, params)
    }

    fun destroy() {
        collectJob?.cancel()
        if (attached) {
            owners.dispatchEvent(Lifecycle.Event.ON_PAUSE)
            owners.dispatchEvent(Lifecycle.Event.ON_STOP)
            owners.dispatchEvent(Lifecycle.Event.ON_DESTROY)
            windowManager.removeView(composeView)
            attached = false
        }
    }
}

/**
 * Manual lifecycle + savedstate owner for a ComposeView living in an overlay
 * window (services have no lifecycle or saved-state by default).
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun performRestore() {
        runCatching { controller.performRestore(null as Bundle?) }
    }

    fun dispatchEvent(event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_CREATE) {
            // restore must complete before ON_CREATE
        }
        registry.handleLifecycleEvent(event)
    }
}
