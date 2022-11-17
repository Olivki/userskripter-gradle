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

import org.gradle.api.Project
import java.io.File
import kotlin.properties.ReadWriteProperty

open class UserskripterMetadataTranspilerExtension(
    private val project: Project,
    private val defaultOutputDirectory: File,
) {
    internal val properties = mutableMapOf<String, MetadataProperty<*>>()

    /**
     * Whether the transpiler should be run on the `generateUserscript` task, `true` by default.
     */
    var runOnGenerate: Boolean by flag("runOnGenerate") { true }

    var outputDirectory: File by singleNonNull("outputDirectory") { defaultOutputDirectory }

    var outputName: String by singleNonNull("outputName") { "UserscriptMetadata" }

    /**
     * Whether properties that are able to should be turned into `const` properties, `false` by default.
     *
     * This is `false` by default as this could drastically increase the file size of the `*.user.js` file due to
     * inlining.
     */
    var useConst: Boolean by flag("useConst") { false }

    /**
     * The name to use for the `object` that will be wrapping the properties, `UserscriptProperties` by default.
     *
     * To not generate a `object` wrapper at all, simple set this to `null`.
     */
    var objectName: String? by single("objectName") { "UserscriptMetadata" }

    var packageName: String by singleNonNull("packageName") {
        project.group.toString().ifBlank { "net.ormr.userskripter.generated" }
    }

    /**
     * The visibility modifier to use for the wrapper object *(if one is used)* and the properties,
     * [NONE][UserscriptPropertiesVisibility.NONE] by default.
     */
    var visibility: UserscriptPropertiesVisibility = UserscriptPropertiesVisibility.NONE

    fun transformName(transformer: (name: String) -> String) {
        nameConverter = transformer
    }

    fun propertyFilter(predicate: (name: String) -> Boolean) {
        propertyFilter = predicate
    }

    internal var propertyFilter: (name: String) -> Boolean = { it in USEFUL_PROPERTIES }
    internal var nameConverter: (name: String) -> String = { it.replace('-', '_').toUpperCase() }

    private fun flag(
        key: String,
        defaultValue: () -> Boolean = { false },
    ): ReadWriteProperty<Any, Boolean> = flagMetadataProperty(key, defaultValue, properties)

    private fun <T> single(
        key: String,
        defaultValue: () -> T? = { null },
    ): ReadWriteProperty<Any, T?> = singleMetadataProperty(key, defaultValue, properties)

    @Suppress("UNCHECKED_CAST")
    private fun <T> singleNonNull(
        key: String,
        defaultValue: () -> T,
    ): ReadWriteProperty<Any, T> = singleMetadataProperty(key, defaultValue, properties) as ReadWriteProperty<Any, T>

    private fun many(
        key: String,
        defaultValue: () -> MutableList<String> = { mutableListOf() },
    ): ReadWriteProperty<Any, MutableList<String>> = manyMetadataProperty(key, defaultValue, properties)

    private fun named(
        key: String,
        defaultValue: () -> MutableMap<String, String> = { mutableMapOf() },
    ): ReadWriteProperty<Any, MutableMap<String, String>> = namedMetadataProperty(key, defaultValue, properties)
}

private val USEFUL_PROPERTIES = hashSetOf("name", "description", "version", "author", "id")