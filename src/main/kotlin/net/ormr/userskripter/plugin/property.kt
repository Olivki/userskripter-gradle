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

@file:Suppress("PropertyName")

package net.ormr.userskripter.plugin

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal fun Map<String, MetadataProperty<*>>.toGradlePropertyMap(): MutableMap<String, String> =
    mapValuesTo(linkedMapOf()) { (_, property) -> property.getValue().toString() }

internal fun flagMetadataProperty(
    key: String,
    defaultValue: () -> Boolean,
    properties: MutableMap<String, MetadataProperty<*>>,
): ReadWriteProperty<Any, Boolean> {
    properties[key] = MetadataProperty.Flag(false, defaultValue)
    return MetadataPropertyAdapter(key, properties)
}

internal fun <T> singleMetadataProperty(
    key: String,
    defaultValue: () -> T?,
    properties: MutableMap<String, MetadataProperty<*>>,
): ReadWriteProperty<Any, T?> {
    properties[key] = MetadataProperty.Single(null, defaultValue)
    return MetadataPropertyAdapter(key, properties)
}

internal fun manyMetadataProperty(
    key: String,
    defaultValue: () -> MutableList<String>,
    properties: MutableMap<String, MetadataProperty<*>>,
): ReadWriteProperty<Any, MutableList<String>> {
    properties[key] = MetadataProperty.Many(mutableListOf(), defaultValue)
    return MetadataPropertyAdapter(key, properties)
}

internal fun namedMetadataProperty(
    key: String,
    defaultValue: () -> MutableMap<String, String>,
    properties: MutableMap<String, MetadataProperty<*>>,
): ReadWriteProperty<Any, MutableMap<String, String>> {
    properties[key] = MetadataProperty.NamedMany(mutableMapOf(), defaultValue)
    return MetadataPropertyAdapter(key, properties)
}

internal sealed class MetadataProperty<T> {
    protected abstract var currentValue: T
    protected abstract val defaultValue: () -> T
    var isManuallySet: Boolean = false
        private set

    fun setValue(value: T) {
        currentValue = value
        isManuallySet = true
    }

    fun getValue(): T? {
        return when (val value = currentValue) {
            null -> if (isManuallySet) null else defaultValue()
            else -> value
        }
    }

    class Flag(
        override var currentValue: Boolean,
        override val defaultValue: () -> Boolean,
    ) : MetadataProperty<Boolean>()

    class Single<T>(
        override var currentValue: T?,
        override val defaultValue: () -> T?,
    ) : MetadataProperty<T?>()

    class Many(
        override var currentValue: MutableList<String>,
        override val defaultValue: () -> MutableList<String>,
    ) : MetadataProperty<MutableList<String>>()

    class NamedMany(
        override var currentValue: MutableMap<String, String>,
        override val defaultValue: () -> MutableMap<String, String>,
    ) : MetadataProperty<MutableMap<String, String>>()
}

private class MetadataPropertyAdapter<T>(
    private val key: String,
    private val properties: MutableMap<String, MetadataProperty<*>>,
) : ReadWriteProperty<Any, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T =
        (properties[key] ?: error("Property with key '${key}' is not registered.")).getValue() as T

    @Suppress("UNCHECKED_CAST")
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val prop = (properties[key] ?: error("Property with key '${key}' is not registered.")) as MetadataProperty<Any?>
        prop.setValue(value)
    }
}