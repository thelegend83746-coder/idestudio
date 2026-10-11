package com.idestudio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.data.model.EditorSettings
import com.idestudio.ui.theme.*

@Composable
fun CodeEditorSurface(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    settings: EditorSettings,
    isJava: Boolean,
    isXml: Boolean,
    errorLine: Int? = null,
    onInsertSymbol: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lines = remember(textFieldValue.text) {
        textFieldValue.text.lines()
    }

    val currentLineIndex = remember(textFieldValue.selection.start, textFieldValue.text) {
        val cursor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
        var count = 0
        var lineIdx = 0
        for ((idx, line) in lines.withIndex()) {
            val nextCount = count + line.length + 1
            if (cursor in count until nextCount) {
                lineIdx = idx
                break
            }
            count = nextCount
        }
        lineIdx
    }

    // Common quick code symbols for mobile coding
    val quickSymbols = listOf(
        "    " to "Tab",
        "{" to "{",
        "}" to "}",
        "(" to "(",
        ")" to ")",
        ";" to ";",
        "=" to "=",
        "\"" to "\"",
        "<" to "<",
        ">" to ">",
        "/" to "/",
        "." to "."
    )

    // Autocomplete suggestions based on current word
    val autocompleteSuggestions = remember(textFieldValue.text, textFieldValue.selection.start, isJava, isXml, settings.autoComplete) {
        if (!settings.autoComplete) return@remember emptyList()
        val cursor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
        val textBefore = textFieldValue.text.take(cursor)
        val word = textBefore.takeLastWhile { it.isLetterOrDigit() || it == '_' || it == ':' }
        if (word.length >= 2) {
            val pool = if (isJava) {
                listOf("public", "private", "protected", "class", "void", "String", "int", "boolean", "return", "override", "findViewById", "setContentView", "Activity", "Bundle")
            } else if (isXml) {
                listOf("android:layout_width", "android:layout_height", "match_parent", "wrap_content", "LinearLayout", "TextView", "Button", "RelativeLayout", "android:id=\"@+id/")
            } else emptyList()
            pool.filter { it.startsWith(word, ignoreCase = true) && !it.equals(word, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (settings.isDarkTheme) DarkBackground else androidx.compose.ui.graphics.Color.White)
    ) {
        // Quick Symbol & Autocomplete Toolbar
        Surface(
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (autocompleteSuggestions.isNotEmpty()) {
                    items(autocompleteSuggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { onInsertSymbol(suggestion) },
                            label = { Text(suggestion, fontSize = 12.sp, color = BrandPrimary) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceElevated)
                        )
                    }
                }

                items(quickSymbols) { (symbol, label) ->
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .background(DarkSurfaceElevated, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { onInsertSymbol(symbol) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = TextPrimary, fontFamily = CodeFontFamily)
                        }
                    }
                }
            }
        }

        // Main Editor Surface (Line numbers + Editor text)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // Line numbers column
            if (settings.showLineNumbers) {
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .background(if (settings.isDarkTheme) DarkGutter else androidx.compose.ui.graphics.Color(0xFFEBEBEB))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lines.size) {
                        val isCurrLine = (i - 1 == currentLineIndex)
                        val isErrLine = (i == errorLine)
                        Text(
                            text = "$i",
                            fontFamily = CodeFontFamily,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * 1.35f).sp,
                            color = when {
                                isErrLine -> StatusError
                                isCurrLine -> TextPrimary
                                else -> TextTertiary
                            }
                        )
                    }
                }
            }

            // Code Text Editing Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (settings.wordWrap) Modifier else Modifier.horizontalScroll(horizontalScrollState))
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = CodeFontFamily,
                        fontSize = settings.fontSizeSp.sp,
                        lineHeight = (settings.fontSizeSp * 1.35f).sp,
                        color = if (settings.isDarkTheme) TextPrimary else androidx.compose.ui.graphics.Color.Black
                    ),
                    cursorBrush = SolidColor(BrandPrimary),
                    visualTransformation = VisualTransformation { text ->
                        val highlighted = SyntaxHighlighter.highlight(text.text, isJava, isXml)
                        TransformedText(highlighted, OffsetMapping.Identity)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
