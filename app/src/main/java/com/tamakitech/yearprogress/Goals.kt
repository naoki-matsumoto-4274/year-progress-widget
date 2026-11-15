package com.tamakitech.yearprogress

/** 年の目標 1 件。 */
data class Goal(val id: String, val text: String, val done: Boolean = false)

/**
 * 目標リストの操作（Android 非依存の純 Kotlin）。
 * どの操作も元のリストを変更せず、新しいリストを返す。該当 id がなければ元のリストをそのまま返す。
 */
object Goals {
    /** 1 件あたりの最大文字数。 */
    const val MAX_LENGTH = 100

    /** 前後の空白を除き、改行・連続空白を 1 つの空白にまとめ、最大文字数で切る。空なら null。 */
    fun normalize(text: String): String? {
        val t = text.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH).trim()
        return t.ifEmpty { null }
    }

    /** 末尾に追加する。text が空（normalize が null）なら追加しない。 */
    fun add(goals: List<Goal>, id: String, text: String): List<Goal> {
        val t = normalize(text) ?: return goals
        return goals + Goal(id = id, text = t)
    }

    /** 文言を変更する（達成状態は維持）。text が空なら変更しない。 */
    fun edit(goals: List<Goal>, id: String, text: String): List<Goal> {
        val t = normalize(text) ?: return goals
        if (goals.none { it.id == id }) return goals
        return goals.map { if (it.id == id) it.copy(text = t) else it }
    }

    /** 達成／未達成を切り替える。 */
    fun toggle(goals: List<Goal>, id: String): List<Goal> {
        if (goals.none { it.id == id }) return goals
        return goals.map { if (it.id == id) it.copy(done = !it.done) else it }
    }

    fun remove(goals: List<Goal>, id: String): List<Goal> {
        if (goals.none { it.id == id }) return goals
        return goals.filterNot { it.id == id }
    }

    fun doneCount(goals: List<Goal>): Int = goals.count { it.done }
}
