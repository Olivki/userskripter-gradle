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

open class UserskripterExtension(private val project: Project) {
    /**
     * The identifier of the userscript, [project.name][Project.getName] by default.
     *
     * This is used to determine the file name of the generated `user` and `meta` files, among other things.
     */
    var id: String = project.name

    /**
     * What mode the resulting JS file should be compiled in, `PRODUCTION` by default.
     *
     * Note that this does not respect the mode set in `commonWebpackConfig`.
     */
    var mode: CompilationMode = CompilationMode.PRODUCTION

    /**
     * Whether the source-map for the generated JS file should be included in the `build/userskripter` directory,
     * `true` by default.
     *
     * Source-maps are normally only created if [mode] is [PRODUCTION][CompilationMode.PRODUCTION].
     */
    var includeSourceMap: Boolean = false

    /**
     * Which userscript engine the userscript is built for, [GREASE_MONKEY][ScriptEngine.GREASE_MONKEY] by default.
     *
     * If the desired userscript engine isn't available, use `GREASE_MONKEY`, as all engines should at the very minimum
     * be compatible with GreaseMonkey standards.
     */
    var scriptEngine: ScriptEngine = ScriptEngine.GREASE_MONKEY
}