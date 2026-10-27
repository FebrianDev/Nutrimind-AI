package com.fdev.nutrimindai.feature.coach.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight, zero-dependency Markdown content renderer tailored specifically
 * for AI chat bubbles (headers, bullets, numbered lists, bold, italics, code).
 */
@Composable
fun MarkdownChatContent(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val lines = remember(content) { content.split("\n") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val numberedRegex = remember { Regex("""^(\d+)\.\s+(.*)$""") }

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            val trimmed = line.trim()

            when {
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = textColor.copy(alpha = 0.2f)
                    )
                }

                trimmed.startsWith("### ") -> {
                    val headerText = trimmed.removePrefix("### ").trim()
                    Text(
                        text = remember(headerText) { parseInlineMarkdown(headerText) },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
                    )
                }

                trimmed.startsWith("## ") -> {
                    val headerText = trimmed.removePrefix("## ").trim()
                    Text(
                        text = remember(headerText) { parseInlineMarkdown(headerText) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                trimmed.startsWith("# ") -> {
                    val headerText = trimmed.removePrefix("# ").trim()
                    Text(
                        text = remember(headerText) { parseInlineMarkdown(headerText) },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ") -> {
                    val bulletText = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = remember(bulletText) { parseInlineMarkdown(bulletText) },
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                numberedRegex.matches(trimmed) -> {
                    val match = numberedRegex.find(trimmed)
                    val number = match?.groupValues?.getOrNull(1) ?: "1"
                    val itemText = match?.groupValues?.getOrNull(2) ?: trimmed

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$number.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = remember(itemText) { parseInlineMarkdown(itemText) },
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                else -> {
                    Text(
                        text = remember(line) { parseInlineMarkdown(line) },
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Parses inline markdown elements (**bold**, *italic*, _italic_, `code`) into an AnnotatedString.
 */
internal fun parseInlineMarkdown(text: String): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")

    val builder = AnnotatedString.Builder()
    val pattern = Regex("""(\*\*(.+?)\*\*)|(\*(.+?)\*)|(_(.+?)_)|(`(.+?)`)|([^*_`]+|[*_`])""")
    val matches = pattern.findAll(text)

    for (match in matches) {
        val boldContent = match.groups[2]?.value
        val italicStarContent = match.groups[4]?.value
        val italicUnderContent = match.groups[6]?.value
        val codeContent = match.groups[8]?.value

        when {
            boldContent != null -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(boldContent)
                builder.pop()
            }
            italicStarContent != null -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(italicStarContent)
                builder.pop()
            }
            italicUnderContent != null -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(italicUnderContent)
                builder.pop()
            }
            codeContent != null -> {
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x18888888)
                    )
                )
                builder.append(codeContent)
                builder.pop()
            }
            else -> {
                builder.append(match.value)
            }
        }
    }

    return builder.toAnnotatedString()
}
