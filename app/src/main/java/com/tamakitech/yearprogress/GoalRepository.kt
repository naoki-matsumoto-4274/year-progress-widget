package com.tamakitech.yearprogress

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 年の目標を端末内（SharedPreferences）に年ごとに保存する。
 * 通信はしない。Android の自動バックアップ（allowBackup）の対象になる。
 */
class GoalRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("goals", Context.MODE_PRIVATE)

    fun load(year: Int): List<Goal> {
        val json = prefs.getString(key(year), null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Goal(id = o.getString("id"), text = o.getString("text"), done = o.optBoolean("done"))
            }
        } catch (e: org.json.JSONException) {
            emptyList()
        }
    }

    fun save(year: Int, goals: List<Goal>) {
        val array = JSONArray()
        goals.forEach {
            array.put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.done))
        }
        prefs.edit().putString(key(year), array.toString()).apply()
    }

    private fun key(year: Int) = "goals_$year"
}
