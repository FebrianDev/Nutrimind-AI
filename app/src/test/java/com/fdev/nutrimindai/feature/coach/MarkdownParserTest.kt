package com.fdev.nutrimindai.feature.coach

import androidx.compose.ui.text.font.FontWeight
import com.fdev.nutrimindai.feature.coach.presentation.components.parseInlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parseInlineMarkdown_emptyText_returnsEmptyAnnotatedString() {
        val result = parseInlineMarkdown("")
        assertEquals("", result.text)
    }

    @Test
    fun parseInlineMarkdown_plainText_returnsUnstyledText() {
        val input = "Hello, this is a plain message without markdown."
        val result = parseInlineMarkdown(input)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun parseInlineMarkdown_boldText_appliesBoldSpan() {
        val input = "Your target is **2200 kcal** for today."
        val result = parseInlineMarkdown(input)
        assertEquals("Your target is 2200 kcal for today.", result.text)
        assertEquals(1, result.spanStyles.size)

        val span = result.spanStyles[0]
        assertEquals(FontWeight.Bold, span.item.fontWeight)
        assertEquals("Your target is ".length, span.start)
        assertEquals("Your target is 2200 kcal".length, span.end)
    }

    @Test
    fun parseInlineMarkdown_multipleStyles_appliesAllSpans() {
        val input = "Eat **chicken breast** with *olive oil* and `quinoa`."
        val result = parseInlineMarkdown(input)
        assertEquals("Eat chicken breast with olive oil and quinoa.", result.text)
        assertEquals(3, result.spanStyles.size)
    }
}
