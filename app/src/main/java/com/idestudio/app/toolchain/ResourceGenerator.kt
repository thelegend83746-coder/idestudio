package com.idestudio.app.toolchain

import com.idestudio.app.data.model.Project
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

object ResourceGenerator {

    /**
     * Scans project res/ folder and generates a real R.java file with unique IDs for
     * string, color, layout, id, drawable, etc., so the Java compiler can compile
     * against R.layout.*, R.id.*, R.string.* without missing symbols.
     */
    fun generateRJava(project: Project, outputDir: File): File {
        val packageSubPath = project.packageName.replace('.', '/')
        val genDir = File(outputDir, packageSubPath)
        genDir.mkdirs()

        val rJavaFile = File(genDir, "R.java")

        val stringMap = mutableMapOf<String, Int>()
        val colorMap = mutableMapOf<String, Int>()
        val layoutMap = mutableMapOf<String, Int>()
        val idMap = mutableMapOf<String, Int>()
        val drawableMap = mutableMapOf<String, Int>()
        val styleMap = mutableMapOf<String, Int>()

        var nextId = 0x7f010000

        // Parse res/values/strings.xml
        val stringsFile = File(project.resDir, "values/strings.xml")
        if (stringsFile.exists()) {
            parseValuesXml(stringsFile.readText(), "string", stringMap) { nextId++ }
        }

        // Parse res/values/colors.xml
        val colorsFile = File(project.resDir, "values/colors.xml")
        if (colorsFile.exists()) {
            parseValuesXml(colorsFile.readText(), "color", colorMap) { nextId++ }
        }

        // Parse res/values/styles.xml
        val stylesFile = File(project.resDir, "values/styles.xml")
        if (stylesFile.exists()) {
            parseValuesXml(stylesFile.readText(), "style", styleMap) { nextId++ }
        }

        // Scan res/layout/*.xml
        val layoutDir = File(project.resDir, "layout")
        if (layoutDir.exists()) {
            layoutDir.listFiles { f -> f.extension.equals("xml", ignoreCase = true) }?.forEach { f ->
                val layoutName = f.nameWithoutExtension
                if (!layoutMap.containsKey(layoutName)) {
                    layoutMap[layoutName] = nextId++
                }
                // Scan layout XML for android:id="@+id/..."
                parseLayoutIds(f.readText(), idMap) { nextId++ }
            }
        }

        // Scan res/drawable
        val drawableDir = File(project.resDir, "drawable")
        if (drawableDir.exists()) {
            drawableDir.listFiles()?.forEach { f ->
                val drawableName = f.nameWithoutExtension
                if (!drawableMap.containsKey(drawableName)) {
                    drawableMap[drawableName] = nextId++
                }
            }
        }

        val rJavaContent = buildString {
            append("/* AUTO-GENERATED FILE. DO NOT MODIFY. */\n")
            append("package ${project.packageName};\n\n")
            append("public final class R {\n")

            appendClass("string", stringMap)
            appendClass("color", colorMap)
            appendClass("style", styleMap)
            appendClass("layout", layoutMap)
            appendClass("id", idMap)
            appendClass("drawable", drawableMap)

            append("}\n")
        }

        rJavaFile.writeText(rJavaContent)
        return rJavaFile
    }

    private fun StringBuilder.appendClass(name: String, map: Map<String, Int>) {
        append("    public static final class $name {\n")
        for ((key, value) in map) {
            val safeKey = key.replace('.', '_').replace('-', '_')
            append(String.format("        public static final int %s = 0x%08x;\n", safeKey, value))
        }
        append("    }\n")
    }

    private fun parseValuesXml(xml: String, tagName: String, map: MutableMap<String, Int>, idGenerator: () -> Int) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == tagName) {
                    val name = parser.getAttributeValue(null, "name")
                    if (!name.isNullOrBlank() && !map.containsKey(name)) {
                        map[name] = idGenerator()
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseLayoutIds(xml: String, idMap: MutableMap<String, Int>, idGenerator: () -> Int) {
        try {
            val idRegex = Regex("""@\+id/([a-zA-Z0-9_]+)""")
            idRegex.findAll(xml).forEach { match ->
                val idName = match.groups[1]?.value
                if (!idName.isNullOrBlank() && !idMap.containsKey(idName)) {
                    idMap[idName] = idGenerator()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
