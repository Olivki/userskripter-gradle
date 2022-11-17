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

internal class MetadataBlockBuilder {
    private val properties: MutableList<Property> = mutableListOf()

    fun flagProperty(key: String, flag: Boolean) {
        if (flag) properties += Property(key, "")
    }

    fun property(key: String, value: String?) {
        if (value != null) properties += Property(key, value)
    }

    fun properties(key: String, properties: List<String>) {
        for (property in properties) property(key, property)
    }

    fun namedProperties(key: String, properties: Map<String, String>) {
        for ((valueKey, value) in properties) property(key, "$valueKey $value")
    }

    fun encodeToString(comparator: Comparator<String>?): String {
        val maxKeySize =
            properties.maxOfOrNull { it.key.length } ?: error("properties was empty, this shouldn't happen")
        val sortable = properties.filter { it.key != "name" }
        val sortedProperties = comparator?.let { sortable.sortedWith { a, b -> it.compare(a.key, b.key) } } ?: sortable
        return """
            |// $METADATA_START
            |// @name${" ".repeat(maxKeySize - 4)}  ${properties.first { it.key == "name" }.value}
            ${sortedProperties.joinToString(separator = "\n") { (key, value) -> "|// @${key}${" ".repeat(maxKeySize - key.length)}  $value" }}
            |// $METADATA_END
        """.trimMargin()
            .splitToSequence('\n')
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n", postfix = "\n") { it.trimEnd() }
    }

    private data class Property(val key: String, val value: String)
}

private const val METADATA_START = "==UserScript=="
private const val METADATA_END = "==/UserScript=="

internal inline fun buildMetadataBlock(
    sorter: Comparator<String>?,
    builder: MetadataBlockBuilder.() -> Unit,
): String {
    val block = MetadataBlockBuilder()
    block.apply(builder)
    return block.encodeToString(sorter)
}

internal fun serializeMetadataBlock(
    metadata: UserskripterMetadataBlockExtension,
): String = buildMetadataBlock(metadata.propertySorter) {
    require("name" in metadata.properties) { "@name is required for metadata blocks" }
    for ((key, property) in metadata.properties.filter { it.key != "id" }) {
        when (property) {
            is MetadataProperty.Flag -> flagProperty(key, property.getValue()!!)
            is MetadataProperty.Single -> property(key, property.getValue()?.toString())
            is MetadataProperty.Many -> properties(key, property.getValue()!!)
            is MetadataProperty.NamedMany -> namedProperties(key, property.getValue()!!)
        }
    }
    for ((key, value) in metadata.extraProperties) properties(key, value)
}