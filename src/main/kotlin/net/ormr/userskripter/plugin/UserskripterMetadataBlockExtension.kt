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
import kotlin.properties.ReadWriteProperty

open class UserskripterMetadataBlockExtension(
    private val project: Project,
    private val userskripter: UserskripterExtension,
) {
    internal val properties = mutableMapOf<String, MetadataProperty<*>>()

    var name: String by singleNonNull("name") { project.name }
    var namespace: String? by single("namespace") { project.group.toString().ifBlank { null } }
    var description: String? by single("description") { project.description }
    var version: String? by single("version") {
        project.version.toString().ifBlank { null }?.takeIf { it != "unspecified" }
    }

    val match: MutableList<String> by many("match")

    val require: MutableList<String> by many("require")

    val resource: MutableMap<String, String> by named("resource")

    var grant: MutableList<String> by many("grant")

    var runAt: RunAt? by single("run-at")

    var icon: String? by single("icon")

    /**
     * If `true` then the script will only run in the top-level document, never in nested frames, `false` by default.
     */
    var noFrames: Boolean by flag("noframes")

    // TAMPERMONKEY START
    // TODO: do we want to have some sort of mechanism for alerting the user that they're using tampermonkey
    //       specific metadata if engine is not set to tampermonkey?

    var icon64: String? by single("icon64")

    var copyright: String? by single("copyright")

    var website: String? by single("website")

    var author: String? by single("author")

    var updateUrl: String? by single("updateURL")

    var downloadUrl: String? by single("downloadURL")

    var supportUrl: String? by single("supportURL")

    /**
     * Whether the userscript should be injected without any wrapper or sandbox.
     */
    var unwrap: Boolean by flag("unwrap")

    var antiFeature: MutableList<String> by many("antifeature")

    fun antiFeatureAds(description: String) {
        antiFeature.add("ads $description")
    }

    fun antiFeatureTracking(description: String) {
        antiFeature.add("tracking $description")
    }

    fun antiFeatureMiner(description: String) {
        antiFeature.add("miner $description")
    }

    /**
     * Defines the domains *(no top-level domains)* including subdomains, which are allowed to be retrieved by
     * `GM.xmlHttpRequest`.
     *
     * Note that this is specific to the TamperMonkey script engine.
     *
     * See [@connect](https://www.tampermonkey.net/documentation.php#_connect) for more information.
     *
     * @see connectSelf
     * @see connectLocalhost
     * @see connectWildcard
     */
    var connect: MutableList<String> by many("connect")

    /**
     * Adds the given [values] to [connect].
     *
     * See [@connect](https://www.tampermonkey.net/documentation.php#_connect) for more information.
     *
     * @param values the values to add, must be one of the following:
     * - domains, like `tampermonkey.net` *(this will also allow all sub-domains)*
     * - sub-domains, i.e `safari.tampermonkey.net`
     * - `self` to whitelist the domain the script is currently running at
     * - `localhost` to access the localhost
     * - `1.2.3.4` to connect to an IP address
     * - `*` for wildcard *(see [connectWildcard])*
     *
     * @see connectSelf
     * @see connectLocalhost
     * @see connectWildcard
     */
    fun connect(vararg values: String) {
        connect.addAll(values)
    }

    /**
     * Adds `self` to [connect] to whitelist the domain the script is currently running at.
     */
    fun connectSelf() {
        connect("self")
    }

    /**
     * Adds `localhost` to [connect] to allow access to the localhost.
     */
    fun connectLocalhost() {
        connect("localhost")
    }

    /**
     * Adds `*` to [connect] to allow any url to be connected to by `GM.xmlHttpRequest`.
     *
     * On every request sent the user will be asked if they are okay with the connection happening or not. An
     * "Always allow all domains" button is also provided.
     *
     * See [@connect](https://www.tampermonkey.net/documentation.php#_connect) for more information.
     */
    fun connectWildcard() {
        connect("*")
    }

    /**
     * Adds [updateUrl] and [downloadUrl] for the given [url].
     *
     * The urls are created using `url` and [id], using the following pattern `${url}${id}.(meta|user).js`, where
     * `meta` is for the `*.meta.js` file *(the file that only contains the metadata block)*, and `user` is for the
     * `*.user.js` file *(the file that contains the actual userscript code)*.
     */
    fun hostedAt(url: String, id: String = userskripter.id) {
        val prefix = if (url.endsWith('/')) url else "${url}/"
        updateUrl = "${prefix}${id}.meta.js"
        downloadUrl = "${prefix}${id}.user.js"
    }

    // TAMPERMONKEY STOP

    /**
     * The comparator to use when sorting the properties for the written metadata block.
     *
     * Note that because of expectations by most userscript engines, the [name] property will *always* be the first
     * property, regardless of the given `comparator`.
     */
    fun sort(comparator: Comparator<String>) {
        propertySorter = comparator
    }

    /**
     * Adds [match] entries to match [hostName] as thoroughly as possible.
     *
     * @param hostName the host name to match, should not be a full url, instead of `www.website.com` use `website.com`,
     * instead of `https://website.com` use `website.com`, etc..
     */
    fun matchHostName(hostName: String) {
        match.add("*://${hostName}/*")
        match.add("*://www.${hostName}/*")
    }

    fun match(vararg patterns: String) {
        match.addAll(patterns)
    }

    fun require(vararg urls: String) {
        require.addAll(urls)
    }

    fun grant(vararg names: String) {
        grant.addAll(names)
    }

    fun resource(vararg resources: Pair<String, String>) {
        this.resource.putAll(resources)
    }

    fun extra(key: String, value: String) {
        extraProperties.getOrPut(key) { mutableListOf() }.add(value);
    }

    fun extra(key: String, vararg values: String) {
        extraProperties.getOrPut(key) { mutableListOf() }.addAll(values)
    }

    fun extra(key: String, values: Iterable<String>) {
        extraProperties.getOrPut(key) { mutableListOf() }.addAll(values)
    }


    internal val extraProperties = mutableMapOf<String, MutableList<String>>()
    internal var propertySorter: Comparator<String>? = null

    internal fun buildGradleProperties(): MutableMap<String, String> {
        val map = properties.toGradlePropertyMap()
        extraProperties.mapValuesTo(map) { (_, value) -> value.toString() }
        return map
    }

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