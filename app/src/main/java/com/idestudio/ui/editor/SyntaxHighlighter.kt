package com.idestudio.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.idestudio.ui.theme.*
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"
    )

    private val JAVA_KEYWORD_PATTERN = Pattern.compile("\\b(" + JAVA_KEYWORDS.joinToString("|") + ")\\b")
    private val JAVA_STRING_PATTERN = Pattern.compile("\".*?\"")
    private val JAVA_NUMBER_PATTERN = Pattern.compile("\\b(\\d+(\\.\\d+)?([fFdDlL])?|0x[0-9a-fA-F]+)\\b")
    private val JAVA_COMMENT_LINE = Pattern.compile("//.*$")
    private val JAVA_COMMENT_BLOCK = Pattern.compile("/\\*[\\s\\S]*?\\*/")
    private val JAVA_ANNOTATION_PATTERN = Pattern.compile("@[A-Za-z0-9_]+")

    private val XML_TAG_PATTERN = Pattern.compile("</?[a-zA-Z0-9_:.-]+|/?>")
    private val XML_ATTR_PATTERN = Pattern.compile("\\b[a-zA-Z0-9_:.-]+(?=\\=)")
    private val XML_STRING_PATTERN = Pattern.compile("\".*?\"|'.*?'")
    private val XML_COMMENT_PATTERN = Pattern.compile("<!--[\\s\\S]*?-->")

    fun highlight(text: String, isJava: Boolean, isXml: Boolean): AnnotatedString {
        if (!isJava && !isXml) {
            return AnnotatedString(text)
        }

        return buildAnnotatedString {
            append(text)

            if (isJava) {
                // 1. Keywords
                applyPattern(JAVA_KEYWORD_PATTERN, text, SpanStyle(color = SyntaxKeyword))
                // 2. Types / Annotations
                applyPattern(JAVA_ANNOTATION_PATTERN, text, SpanStyle(color = SyntaxAnnotation))
                // 3. Numbers
                applyPattern(JAVA_NUMBER_PATTERN, text, SpanStyle(color = SyntaxNumber))
                // 4. Strings
                applyPattern(JAVA_STRING_PATTERN, text, SpanStyle(color = SyntaxString))
                // 5. Comments (override earlier styles)
                applyPattern(JAVA_COMMENT_LINE, text, SpanStyle(color = SyntaxComment))
                applyPattern(JAVA_COMMENT_BLOCK, text, SpanStyle(color = SyntaxComment))
            } else if (isXml) {
                // XML Tags
                applyPattern(XML_TAG_PATTERN, text, SpanStyle(color = SyntaxXmlTag))
                // XML Attributes
                applyPattern(XML_ATTR_PATTERN, text, SpanStyle(color = SyntaxXmlAttr))
                // XML Attribute values (strings)
                applyPattern(XML_STRING_PATTERN, text, SpanStyle(color = SyntaxXmlValue))
                // XML Comments
                applyPattern(XML_COMMENT_PATTERN, text, SpanStyle(color = SyntaxComment))
            }
        }
    }

    private fun AnnotatedString.Builder.applyPattern(pattern: Pattern, text: String, style: SpanStyle) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            addStyle(style, matcher.start(), matcher.end())
        }
    }
}
