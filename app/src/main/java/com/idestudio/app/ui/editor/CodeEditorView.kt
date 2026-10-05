package com.idestudio.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.app.data.model.EditorTab
import com.idestudio.app.ui.theme.PurplePrimary

@Composable
fun CodeEditorView(
    tab: EditorTab,
    fontSizeSp: Int = 13,
    showLineNumbers: Boolean = true,
    syntaxHighlighting: Boolean = true,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(tab.id) {
        mutableStateOf(TextFieldValue(tab.content, TextRange(tab.cursorPosition)))
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lines = textFieldValue.text.lines()
    val lineCount = lines.size.coerceAtLeast(1)

    // Visual transformation for Syntax Highlighting
    val visualTransformation = remember(tab.isXml, syntaxHighlighting) {
        if (!syntaxHighlighting) {
            VisualTransformation.None
        } else {
            VisualTransformation { text ->
                val annotated = SyntaxHighlighter.highlight(text.text, tab.isXml)
                TransformedText(annotated, OffsetMapping.Identity)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Line Numbers Gutter
        if (showLineNumbers) {
            val gutterWidth = (lineCount.toString().length * 10 + 20).coerceAtLeast(36).dp
            val lineNumbersText = (1..lineCount).joinToString("\n")

            Box(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFF7F8F9))
                    .padding(vertical = 12.dp, horizontal = 6.dp)
                    .verticalScroll(verticalScrollState)
            ) {
                Text(
                    text = lineNumbersText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp + 6).sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Editable Code Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Auto-indentation handling when enter is pressed
                    val oldText = textFieldValue.text
                    val newText = newValue.text
                    var adjustedValue = newValue

                    if (newText.length == oldText.length + 1 &&
                        newValue.selection.start > 0 &&
                        newText[newValue.selection.start - 1] == '\n'
                    ) {
                        val pos = newValue.selection.start - 1
                        val prevLineStart = newText.lastIndexOf('\n', pos - 1).let { if (it == -1) 0 else it + 1 }
                        val prevLine = newText.substring(prevLineStart, pos)
                        val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
                        val extraIndent = if (prevLine.trimEnd().endsWith("{")) "    " else ""
                        val fullIndent = indent + extraIndent

                        if (fullIndent.isNotEmpty()) {
                            val inserted = newText.substring(0, pos + 1) + fullIndent + newText.substring(pos + 1)
                            val newCursor = pos + 1 + fullIndent.length
                            adjustedValue = TextFieldValue(inserted, TextRange(newCursor))
                        }
                    }

                    textFieldValue = adjustedValue
                    tab.cursorPosition = adjustedValue.selection.start
                    onContentChange(adjustedValue.text)
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp + 6).sp,
                    color = Color(0xFF212121)
                ),
                cursorBrush = SolidColor(PurplePrimary),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
