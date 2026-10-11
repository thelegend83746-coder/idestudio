package com.idestudio.engine

import com.idestudio.data.model.Project
import com.idestudio.util.FileUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

object RJavaGenerator {

    fun generateRJava(project: Project, outputDir: File): File {
        val resDir = File(project.resDir)
        val packageDir = File(outputDir, project.packageName.replace('.', '/'))
        packageDir.mkdirs()

        val idNames = mutableSetOf<String>()
        val layoutNames = mutableSetOf<String>()
        val stringNames = mutableSetOf<String>()
        val colorNames = mutableSetOf<String>()
        val styleNames = mutableSetOf<String>()
        val drawableNames = mutableSetOf<String>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true

        if (resDir.exists()) {
            // 1. Process layouts
            val layoutDir = File(resDir, "layout")
            if (layoutDir.exists()) {
                layoutDir.listFiles()?.forEach { file ->
                    if (file.extension == "xml") {
                        val baseName = file.nameWithoutExtension
                        layoutNames.add(baseName)
                        extractIdsFromXml(file, idNames, factory)
                    }
                }
            }

            // 2. Process drawables
            val drawableDir = File(resDir, "drawable")
            if (drawableDir.exists()) {
                drawableDir.listFiles()?.forEach { file ->
                    drawableNames.add(file.nameWithoutExtension)
                    if (file.extension == "xml") {
                        extractIdsFromXml(file, idNames, factory)
                    }
                }
            }

            // 3. Process values
            val valuesDir = File(resDir, "values")
            if (valuesDir.exists()) {
                valuesDir.listFiles()?.forEach { file ->
                    if (file.extension == "xml") {
                        parseValuesXml(file, stringNames, colorNames, styleNames, idNames, factory)
                    }
                }
            }
        }

        // Build R.java source code
        val sb = StringBuilder()
        sb.append("/* AUTO-GENERATED FILE. DO NOT MODIFY. */\n")
        sb.append("package ").append(project.packageName).append(";\n\n")
        sb.append("public final class R {\n")

        var idCounter = 0x7f010000
        appendSubClass(sb, "id", idNames, idCounter)

        idCounter = 0x7f020000
        appendSubClass(sb, "layout", layoutNames, idCounter)

        idCounter = 0x7f030000
        appendSubClass(sb, "string", stringNames, idCounter)

        idCounter = 0x7f040000
        appendSubClass(sb, "color", colorNames, idCounter)

        idCounter = 0x7f050000
        appendSubClass(sb, "style", styleNames, idCounter)

        idCounter = 0x7f060000
        appendSubClass(sb, "drawable", drawableNames, idCounter)

        sb.append("}\n")

        val rFile = File(packageDir, "R.java")
        FileUtils.writeFileSafe(rFile, sb.toString())
        return rFile
    }

    private fun appendSubClass(sb: StringBuilder, subClassName: String, names: Set<String>, startId: Int) {
        sb.append("    public static final class ").append(subClassName).append(" {\n")
        var currentId = startId
        for (name in names.sorted()) {
            val validName = sanitizeIdentifier(name)
            val hex = String.format("0x%08x", currentId++)
            sb.append("        public static final int ").append(validName).append(" = ").append(hex).append(";\n")
        }
        sb.append("    }\n")
    }

    private fun extractIdsFromXml(file: File, idSet: MutableSet<String>, factory: XmlPullParserFactory) {
        try {
            val content = file.readText(Charsets.UTF_8)
            val parser = factory.newPullParser()
            parser.setInput(StringReader(content))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    for (i in 0 until parser.attributeCount) {
                        val attrVal = parser.getAttributeValue(i)
                        if (attrVal != null && (attrVal.startsWith("@+id/") || attrVal.startsWith("@id/"))) {
                            val id = attrVal.substringAfter('/')
                            idSet.add(id)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
    }

    private fun parseValuesXml(
        file: File,
        strings: MutableSet<String>,
        colors: MutableSet<String>,
        styles: MutableSet<String>,
        ids: MutableSet<String>,
        factory: XmlPullParserFactory
    ) {
        try {
            val content = file.readText(Charsets.UTF_8)
            val parser = factory.newPullParser()
            parser.setInput(StringReader(content))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    val nameAttr = parser.getAttributeValue(null, "name")
                    if (nameAttr != null) {
                        when (tagName) {
                            "string" -> strings.add(nameAttr)
                            "color" -> colors.add(nameAttr)
                            "style" -> styles.add(nameAttr)
                            "item" -> {
                                val typeAttr = parser.getAttributeValue(null, "type")
                                if (typeAttr == "id") ids.add(nameAttr)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
    }

    private fun sanitizeIdentifier(name: String): String {
        return name.replace('.', '_').replace('-', '_')
    }
}
