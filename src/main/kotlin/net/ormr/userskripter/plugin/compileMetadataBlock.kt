/*
 * Copyright 2022 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.ormr.userskripter.plugin

import net.ormr.userskripter.plugin.UserscriptPropertiesVisibility.*

// TODO: sort using the sort defined for 'metadata'?
// TODO: escape generated strings
internal class MetadataBlockToKotlinCompiler {
    private val properties = mutableMapOf<String, Expression>()

    fun flagProperty(key: String, property: MetadataProperty.Flag) {
        if (property.isManuallySet) properties[key] = Expression(if (property.getValue()!!) "true" else "false", true)
    }

    fun property(key: String, property: MetadataProperty.Single<*>) {
        val value = property.getValue()
        if (value != null) properties[key] = Expression("\"${value}\"", true)
    }

    fun stringProperty(key: String, value: String) {
        properties[key] = Expression("\"${value}\"", true)
    }

    fun properties(key: String, property: MetadataProperty.Many) {
        val value = property.getValue()!!
        if (value.isNotEmpty()) {
            properties[key] = Expression("listOf(${value.joinToString { "\"$it\"" }})", false)
        }
    }

    fun extra(key: String, value: List<String>) {
        if (value.isNotEmpty()) {
            properties[key] = Expression("listOf(${value.joinToString { "\"$it\"" }})", false)
        }
    }

    fun namedProperties(key: String, property: MetadataProperty.NamedMany) {
        val value = property.getValue()!!
        if (value.isNotEmpty()) {
            properties[key] =
                Expression("mapOf(${value.entries.joinToString { "\"${it.key}\" to \"${it.value}\"" }})", false)
        }
    }

    fun encodeToString(extension: UserskripterMetadataTranspilerExtension): String {
        val wrapperName = extension.objectName
        val visibility = when (val it = extension.visibility) {
            NONE -> ""
            PUBLIC, INTERNAL -> "${it.name.toLowerCase()} "
        }
        val indent = if (wrapperName != null) " ".repeat(4) else ""
        return """
            |package ${extension.packageName}
            
            |${if (wrapperName != null) "${visibility}object $wrapperName {" else ""}
            ${
            properties
                .entries
                .filter { extension.propertyFilter(it.key) }
                .joinToString(separator = "\n") { (key, expr) ->
                    val prefix = if (expr.isConstable && extension.useConst) "const " else ""
                    "|${indent}${visibility}${prefix}val ${extension.nameConverter(key)} = ${expr.code}"
                }
        }
            |${if (wrapperName != null) "}" else ""}
        """.trimMargin()
            .splitToSequence('\n')
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n", postfix = "\n") {
                it.trimEnd().let { s -> if (s.startsWith("package")) "${s}\n" else s }
            }
    }

    private data class Expression(val code: String, val isConstable: Boolean)
}

fun compileToKotlin(
    extension: UserskripterExtension,
    metadata: UserskripterMetadataBlockExtension,
    transpiler: UserskripterMetadataTranspilerExtension,
): String {
    val compiler = MetadataBlockToKotlinCompiler()
    compiler.stringProperty("id", extension.id)
    for ((key, property) in metadata.properties) {
        when (property) {
            is MetadataProperty.Flag -> compiler.flagProperty(key, property)
            is MetadataProperty.Single -> compiler.property(key, property)
            is MetadataProperty.Many -> compiler.properties(key, property)
            is MetadataProperty.NamedMany -> compiler.namedProperties(key, property)
        }
    }
    for ((key, value) in metadata.extraProperties) compiler.extra(key, value)
    return compiler.encodeToString(transpiler)
}