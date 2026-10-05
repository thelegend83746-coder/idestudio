package com.idestudio.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.idestudio.app.ui.theme.SyntaxComment
import com.idestudio.app.ui.theme.SyntaxKeyword
import com.idestudio.app.ui.theme.SyntaxNumber
import com.idestudio.app.ui.theme.SyntaxString
import com.idestudio.app.ui.theme.SyntaxType
import com.idestudio.app.ui.theme.SyntaxXmlAttr
import com.idestudio.app.ui.theme.SyntaxXmlTag

object SyntaxHighlighter {

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "true", "false", "null"
    )

    private val JAVA_TYPES = setOf(
        "String", "Object", "Integer", "Long", "Double", "Float", "Boolean", "Activity", "Bundle",
        "View", "Button", "TextView", "EditText", "Toast", "Intent", "Context", "List", "Map", "Set",
        "ArrayList", "HashMap", "Override", "Nullable", "NonNull"
    )

    fun highlight(text: String, isXml: Boolean): AnnotatedString {
        return if (isXml) {
            highlightXml(text)
        } else {
            highlightJava(text)
        }
    }

    private fun highlightJava(text: String): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        val length = text.length

        var i = 0
        while (i < length) {
            val c = text[i]

            // Comments
            if (c == '/' && i + 1 < length && text[i + 1] == '/') {
                val start = i
                val end = text.indexOf('\n', start).let { if (it == -1) length else it }
                builder.addStyle(SpanStyle(color = SyntaxComment), start, end)
                i = end
                continue
            } else if (c == '/' && i + 1 < length && text[i + 1] == '*') {
                val start = i
                val end = text.indexOf("*/", start + 2).let { if (it == -1) length else it + 2 }
                builder.addStyle(SpanStyle(color = SyntaxComment), start, end)
                i = end
                continue
            }

            // Strings
            if (c == '"') {
                val start = i
                var end = i + 1
                while (end < length) {
                    if (text[end] == '\\') {
                        end += 2
                    } else if (text[end] == '"') {
                        end++
                        break
                    } else if (text[end] == '\n') {
                        break
                    } else {
                        end++
                    }
                }
                builder.addStyle(SpanStyle(color = SyntaxString), start, end)
                i = end
                continue
            }

            // Character literal
            if (c == '\'') {
                val start = i
                var end = i + 1
                while (end < length && end <= i + 3) {
                    if (text[end] == '\'') {
                        end++
                        break
                    }
                    end++
                }
                builder.addStyle(SpanStyle(color = SyntaxString), start, end)
                i = end
                continue
            }

            // Annotations (@Override)
            if (c == '@' && i + 1 < length && text[i + 1].isJavaIdentifierStart()) {
                val start = i
                var end = i + 1
                while (end < length && text[end].isJavaIdentifierPart()) {
                    end++
                }
                builder.addStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Bold), start, end)
                i = end
                continue
            }

            // Numbers
            if (c.isDigit() && (i == 0 || !text[i - 1].isJavaIdentifierPart())) {
                val start = i
                var end = i + 1
                while (end < length && (text[end].isLetterOrDigit() || text[end] == '.')) {
                    end++
                }
                builder.addStyle(SpanStyle(color = SyntaxNumber), start, end)
                i = end
                continue
            }

            // Identifiers / Keywords
            if (c.isJavaIdentifierStart()) {
                val start = i
                var end = i + 1
                while (end < length && text[end].isJavaIdentifierPart()) {
                    end++
                }
                val word = text.substring(start, end)
                if (JAVA_KEYWORDS.contains(word)) {
                    builder.addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end)
                } else if (JAVA_TYPES.contains(word)) {
                    builder.addStyle(SpanStyle(color = SyntaxType), start, end)
                }
                i = end
                continue
            }

            i++
        }

        return builder.toAnnotatedString()
    }

    private fun highlightXml(text: String): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        val length = text.length

        var i = 0
        while (i < length) {
            val c = text[i]

            // XML Comment <!-- -->
            if (c == '<' && i + 3 < length && text.startsWith("<!--", i)) {
                val start = i
                val end = text.indexOf("-->", start + 4).let { if (it == -1) length else it + 3 }
                builder.addStyle(SpanStyle(color = SyntaxComment), start, end)
                i = end
                continue
            }

            // XML Tags and attributes
            if (c == '<') {
                val tagStart = i
                builder.addStyle(SpanStyle(color = SyntaxXmlTag, fontWeight = FontWeight.Bold), tagStart, tagStart + 1)
                i++

                // Closing slash if </
                if (i < length && text[i] == '/') {
                    builder.addStyle(SpanStyle(color = SyntaxXmlTag, fontWeight = FontWeight.Bold), i, i + 1)
                    i++
                }

                // Tag Name
                val nameStart = i
                while (i < length && (text[i].isLetterOrDigit() || text[i] == ':' || text[i] == '-' || text[i] == '_')) {
                    i++
                }
                builder.addStyle(SpanStyle(color = SyntaxXmlTag, fontWeight = FontWeight.Bold), nameStart, i)

                // Inside Tag Attributes
                while (i < length && text[i] != '>') {
                    val curr = text[i]
                    if (curr == '"') {
                        val strStart = i
                        var strEnd = i + 1
                        while (strEnd < length && text[strEnd] != '"' && text[strEnd] != '\n') {
                            strEnd++
                        }
                        if (strEnd < length && text[strEnd] == '"') strEnd++
                        builder.addStyle(SpanStyle(color = SyntaxString), strStart, strEnd)
                        i = strEnd
                        continue
                    } else if (curr.isLetter() || curr == ':') {
                        val attrStart = i
                        while (i < length && (text[i].isLetterOrDigit() || text[i] == ':' || text[i] == '-' || text[i] == '_')) {
                            i++
                        }
                        builder.addStyle(SpanStyle(color = SyntaxXmlAttr), attrStart, i)
                        continue
                    }
                    i++
                }

                if (i < length && text[i] == '>') {
                    builder.addStyle(SpanStyle(color = SyntaxXmlTag, fontWeight = FontWeight.Bold), i, i + 1)
                }
            }

            i++
        }

        return builder.toAnnotatedString()
    }
}
