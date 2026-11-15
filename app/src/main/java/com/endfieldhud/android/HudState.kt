package com.endfieldhud.android

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.hudDataStore by preferencesDataStore(name = "hud_prefs")

enum class HudModule(val label: String, val code: String) {
    NOTES("便笺", "01"),
    CLIPBOARD("剪贴板", "02"),
    TIMER("计时", "03"),
    MONITOR("监视", "04"),
    ID("名片", "05"),
    LOG("日志", "06"),
    APPS("应用", "07"),
    MERGE("融合", "08"),
    SETTINGS("设置", "09"),
}

data class NoteItem(val id: Long, val text: String, val done: Boolean = false, val isTodo: Boolean = false)

data class ClipItem(val id: Long, val text: String, val at: Long)

data class LogEvent(val at: Long, val msg: String, val kind: Int = 0) {
    companion object {
        const val INFO = 0
        const val WARN = 1
        const val OK = 2
    }
}

data class Profile(
    val codename: String = "ENDMINISTRATOR",
    val operatorId: String = "EF-000",
    val title: String = "管理员",
    val division: String = "塔卫二 · 工程部",
    val status: String = "在岗",
)

data class HudSettings(
    val scale: Int = 1,               // 0 = S, 1 = M, 2 = L
    val bubbleAlpha: Float = 0.9f,
    val vibrate: Boolean = true,
)

object HudState {
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val initialized = MutableStateFlow(false)

    // runtime state
    val serviceRunning = MutableStateFlow(false)
    val collapsed = MutableStateFlow(false)
    val currentModule = MutableStateFlow(HudModule.NOTES)

    // persisted data
    val notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val clipboardItems = MutableStateFlow<List<ClipItem>>(emptyList())
    val events = MutableStateFlow<List<LogEvent>>(emptyList())
    val profile = MutableStateFlow(Profile())
    val favApps = MutableStateFlow<Set<String>>(emptySet())
    val settings = MutableStateFlow(HudSettings())
    val bestScore = MutableStateFlow(0)

    // timer / stopwatch (driven by HudService)
    val timerRemainingMs = MutableStateFlow(0L)
    val timerTotalMs = MutableStateFlow(1L)
    val timerRunning = MutableStateFlow(false)
    val stopwatchMs = MutableStateFlow(0L)
    val stopwatchRunning = MutableStateFlow(false)
    val stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())

    private object Keys {
        val NOTES = stringPreferencesKey("notes_json")
        val EVENTS = stringPreferencesKey("events_json")
        val PROFILE = stringPreferencesKey("profile_json")
        val FAV_APPS = stringPreferencesKey("fav_apps_json")
        val SETTINGS = stringPreferencesKey("settings_json")
        val BEST_SCORE = intPreferencesKey("merge_best_score")
    }

    fun init(context: Context) {
        if (initialized.value) return
        appContext = context.applicationContext
        scope.launch {
            try {
                val prefs = appContext.hudDataStore.data.first()
                prefs[Keys.NOTES]?.let { json ->
                    runCatching {
                        val arr = JSONArray(json)
                        notes.value = (0 until arr.length()).map { i ->
                            val o = arr.getJSONObject(i)
                            NoteItem(
                                id = o.getLong("id"),
                                text = o.getString("text"),
                                done = o.optBoolean("done", false),
                                isTodo = o.optBoolean("todo", false),
                            )
                        }
                    }
                }
                prefs[Keys.EVENTS]?.let { json ->
                    runCatching {
                        val arr = JSONArray(json)
                        events.value = (0 until arr.length()).map { i ->
                            val o = arr.getJSONObject(i)
                            LogEvent(o.getLong("at"), o.getString("msg"), o.optInt("kind", 0))
                        }
                    }
                }
                prefs[Keys.PROFILE]?.let { json ->
                    runCatching {
                        val o = JSONObject(json)
                        profile.value = Profile(
                            codename = o.optString("codename", "ENDMINISTRATOR"),
                            operatorId = o.optString("operatorId", "EF-000"),
                            title = o.optString("title", "管理员"),
                            division = o.optString("division", "塔卫二 · 工程部"),
                            status = o.optString("status", "在岗"),
                        )
                    }
                }
                prefs[Keys.FAV_APPS]?.let { json ->
                    runCatching {
                        val arr = JSONArray(json)
                        favApps.value = (0 until arr.length()).map { arr.getString(it) }.toSet()
                    }
                }
                prefs[Keys.SETTINGS]?.let { json ->
                    runCatching {
                        val o = JSONObject(json)
                        settings.value = HudSettings(
                            scale = o.optInt("scale", 1).coerceIn(0, 2),
                            bubbleAlpha = o.optDouble("alpha", 0.9).toFloat().coerceIn(0.4f, 1f),
                            vibrate = o.optBoolean("vibrate", true),
                        )
                    }
                }
                bestScore.value = prefs[Keys.BEST_SCORE] ?: 0
            } finally {
                initialized.value = true
            }
        }
    }

    // ---------- events ----------

    fun logEvent(msg: String, kind: Int = LogEvent.INFO) {
        val now = System.currentTimeMillis()
        events.value = (listOf(LogEvent(now, msg, kind)) + events.value).take(80)
        scope.launch {
            val arr = JSONArray()
            events.value.take(80).forEach {
                arr.put(JSONObject().put("at", it.at).put("msg", it.msg).put("kind", it.kind))
            }
            appContext.hudDataStore.edit { it[Keys.EVENTS] = arr.toString() }
        }
    }

    fun clearEvents() {
        events.value = emptyList()
        scope.launch { appContext.hudDataStore.edit { it.remove(Keys.EVENTS) } }
        logEvent("事件日志已清空", LogEvent.WARN)
    }

    // ---------- notes ----------

    fun addNote(text: String, isTodo: Boolean) {
        val item = NoteItem(id = System.currentTimeMillis(), text = text, isTodo = isTodo)
        notes.value = (listOf(item) + notes.value).take(100)
        persistNotes()
    }

    fun toggleNote(id: Long) {
        notes.value = notes.value.map { if (it.id == id) it.copy(done = !it.done) else it }
        persistNotes()
    }

    fun deleteNote(id: Long) {
        notes.value = notes.value.filter { it.id != id }
        persistNotes()
    }

    private fun persistNotes() {
        scope.launch {
            val arr = JSONArray()
            notes.value.forEach {
                arr.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("text", it.text)
                        .put("done", it.done)
                        .put("todo", it.isTodo)
                )
            }
            appContext.hudDataStore.edit { it[Keys.NOTES] = arr.toString() }
        }
    }

    // ---------- clipboard (session-only, cleared on service exit) ----------

    @Volatile
    var lastCapturedClip: String? = null

    fun captureClip(text: String) {
        val item = ClipItem(id = System.currentTimeMillis(), text = text.take(600), at = System.currentTimeMillis())
        clipboardItems.value = (listOf(item) + clipboardItems.value).take(20)
    }

    fun clearClips() {
        clipboardItems.value = emptyList()
        lastCapturedClip = null
    }

    // ---------- profile / settings / favorites ----------

    fun setProfile(p: Profile) {
        profile.value = p
        scope.launch {
            val o = JSONObject()
                .put("codename", p.codename)
                .put("operatorId", p.operatorId)
                .put("title", p.title)
                .put("division", p.division)
                .put("status", p.status)
            appContext.hudDataStore.edit { it[Keys.PROFILE] = o.toString() }
        }
    }

    fun setSettings(s: HudSettings) {
        settings.value = s
        scope.launch {
            val o = JSONObject()
                .put("scale", s.scale)
                .put("alpha", s.bubbleAlpha.toDouble())
                .put("vibrate", s.vibrate)
            appContext.hudDataStore.edit { it[Keys.SETTINGS] = o.toString() }
        }
    }

    fun toggleFav(pkg: String) {
        favApps.value = if (pkg in favApps.value) favApps.value - pkg else favApps.value + pkg
        scope.launch {
            val arr = JSONArray()
            favApps.value.forEach { arr.put(it) }
            appContext.hudDataStore.edit { it[Keys.FAV_APPS] = arr.toString() }
        }
    }

    fun setBestScore(score: Int) {
        if (score <= bestScore.value) return
        bestScore.value = score
        scope.launch { appContext.hudDataStore.edit { it[Keys.BEST_SCORE] = score } }
    }
}
