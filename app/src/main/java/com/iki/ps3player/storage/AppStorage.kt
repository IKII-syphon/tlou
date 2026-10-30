package com.iki.ps3player.storage

import android.content.Context
import com.iki.ps3player.controls.ControlLayout
import com.iki.ps3player.game.CompatStatus
import com.iki.ps3player.game.GameEntry
import org.json.JSONArray
import org.json.JSONObject

class AppStorage(ctx: Context) {
    private val p = ctx.getSharedPreferences("ps3player", Context.MODE_PRIVATE)

    fun loadGames(): List<GameEntry> = runCatching {
        val a = JSONArray(p.getString("games", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            GameEntry(
                o.getString("id"), o.getString("name"), o.getString("uri"), o.getLong("size"),
                runCatching { CompatStatus.valueOf(o.optString("status")) }.getOrDefault(CompatStatus.NOT_TESTED),
            )
        }
    }.getOrDefault(emptyList())

    fun saveGames(list: List<GameEntry>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name).put("uri", it.uri)
                .put("size", it.sizeBytes).put("status", it.status.name))
        }
        p.edit().putString("games", a.toString()).apply()
    }

    fun loadLayout(gameId: String) = ControlLayout.decode(p.getString("layout_$gameId", null))
    fun saveLayout(gameId: String, l: ControlLayout) { p.edit().putString("layout_$gameId", l.encode()).apply() }
    fun deleteLayout(gameId: String) { p.edit().remove("layout_$gameId").apply() }

    fun firmwareStatus(): String? = p.getString("fw_status", null)
    fun setFirmwareStatus(v: String) { p.edit().putString("fw_status", v).apply() }
}
