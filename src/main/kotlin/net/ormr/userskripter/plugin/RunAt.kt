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

sealed class RunAt(internal val name: String) {
    final override fun toString(): String = name

    /**
     * The script will run before any document loading, thus before any scripts run or images load.
     */
    object DocumentStart : RunAt("document-start")

    /**
     * The script will run after the main page is loaded, but before other resources *(images, style sheets, etc.) have
     * loaded.
     */
    object DocumentEnd : RunAt("document-end")

    /**
     * The script will run after the page and all resources *(images, style sheets, etc.) are loaded and page scripts
     * have run.
     */
    object DocumentIdle : RunAt("document-idle")

    // TODO: tampermonkey specific
    object DocumentBody : RunAt("document-body")

    // TODO: tampermonkey specific
    object ContextMenu : RunAt("context-menu")

    class Custom(name: String) : RunAt(name)
}