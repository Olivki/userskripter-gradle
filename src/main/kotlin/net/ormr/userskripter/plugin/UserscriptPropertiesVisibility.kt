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

enum class UserscriptPropertiesVisibility {
    /**
     * No visibility modifier will be used, i.e: `val ID = ...`.
     */
    NONE,

    /**
     * Explicit public visibility modifier will be used, i.e: `public val ID = ...`.
     */
    PUBLIC,

    /**
     * Internal visibility modifier will be used, i.e: `internal val ID = ...`.
     */
    INTERNAL,
}