package com.endfieldhud.android

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile — the Android replacement for the desktop app's
 * global Ctrl+` hotkey. One tap toggles the HUD overlay.
 */
class HudTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val ctx = applicationContext
        if (!Settings.canDrawOverlays(ctx)) {
            // no overlay permission -> guide the user to the app
            val intent = Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(
                        ctx, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        if (HudState.serviceRunning.value) {
            HudService.send(ctx, HudService.ACTION_TOGGLE)
        } else {
            HudService.start(ctx)
        }
        updateTile()
    }

    private fun updateTile() {
        val t = qsTile ?: return
        val running = HudState.serviceRunning.value
        t.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.subtitle = if (running) "运行中" else null
        t.updateTile()
    }
}
