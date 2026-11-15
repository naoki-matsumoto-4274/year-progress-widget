package com.tamakitech.yearprogress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GoalsTest {
    @Test fun normalizeTrimsEdgesAndCollapsesWhitespace() {
        assertEquals("one two three", Goals.normalize(" \tone\n two   three \t"))
    }

    @Test fun normalizeReturnsNullForWhitespaceOnly() {
        assertEquals(null, Goals.normalize(" \n\t "))
    }

    @Test fun normalizeTruncatesToMaximumLength() {
        assertEquals("x".repeat(Goals.MAX_LENGTH), Goals.normalize("x".repeat(101)))
    }

    @Test fun normalizeTrimsTrailingSpaceCreatedByTruncation() {
        val input = "x".repeat(Goals.MAX_LENGTH - 1) + "  y"
        assertEquals("x".repeat(Goals.MAX_LENGTH - 1), Goals.normalize(input))
    }

    @Test fun addAppendsUndoneGoalWithoutMutatingInput() {
        val original = listOf(Goal("a", "first", done = true))
        val result = Goals.add(original, "b", "  second  ")
        assertEquals(listOf(Goal("a", "first", true), Goal("b", "second", false)), result)
        assertEquals(listOf(Goal("a", "first", true)), original)
    }

    @Test fun addIgnoresEmptyTextAndReturnsSameList() {
        val original = listOf(Goal("a", "first"))
        assertSame(original, Goals.add(original, "b", " \n "))
    }

    @Test fun editChangesTextAndPreservesDoneState() {
        val original = listOf(Goal("a", "old", done = true), Goal("b", "other"))
        assertEquals(listOf(Goal("a", "new", done = true), Goal("b", "other")), Goals.edit(original, "a", "new"))
        assertEquals("old", original.first().text)
    }

    @Test fun editWithEmptyTextLeavesListUnchanged() {
        val original = listOf(Goal("a", "old"))
        assertSame(original, Goals.edit(original, "a", " \t"))
    }

    @Test fun editWithUnknownIdLeavesListUnchanged() {
        val original = listOf(Goal("a", "old"))
        assertSame(original, Goals.edit(original, "missing", "new"))
    }

    @Test fun toggleTwiceRestoresGoalAndLeavesOtherGoalsUnchanged() {
        val original = listOf(Goal("a", "first"), Goal("b", "second", done = true))
        val once = Goals.toggle(original, "a")
        assertEquals(listOf(Goal("a", "first", true), Goal("b", "second", true)), once)
        assertEquals(original, Goals.toggle(once, "a"))
    }

    @Test fun removeDeletesOnlyMatchingId() {
        val original = listOf(Goal("a", "first"), Goal("b", "second"), Goal("c", "third"))
        assertEquals(listOf(Goal("a", "first"), Goal("c", "third")), Goals.remove(original, "b"))
        assertEquals(3, original.size)
    }

    @Test fun removeWithUnknownIdLeavesListUnchanged() {
        val original = listOf(Goal("a", "first"))
        assertEquals(original, Goals.remove(original, "missing"))
    }

    @Test fun doneCountHandlesEmptyAndMixedLists() {
        assertEquals(0, Goals.doneCount(emptyList()))
        assertEquals(2, Goals.doneCount(listOf(Goal("a", "first", true), Goal("b", "second"), Goal("c", "third", true))))
    }
}
