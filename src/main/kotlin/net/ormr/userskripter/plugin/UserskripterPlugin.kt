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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.properties
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

private const val PACKAGE = "net.ormr.userskripter"
private const val ENGINES_PACKAGE = "${PACKAGE}.engine"
private const val GREASE_MONKEY_PATH = "${ENGINES_PACKAGE}.greasemonkey"
private const val TAMPER_MONKEY_PATH = "${ENGINES_PACKAGE}.tampermonkey"
private const val UNSAFE_WINDOW_COMPATIBLE = "${ENGINES_PACKAGE}.UnsafeWindowCompatibleScriptEngine"

class UserskripterPlugin : Plugin<Project> {
    companion object {
        private val gmGrants = mapOf(
            "GM.setValue" to "GrantGMSetValue",
            "GM.getValue" to "GrantGMGetValue",
            "GM.deleteValue" to "GrantGMDeleteValue",
            "GM.listValues" to "GrantGMListValues",
            "GM.getResourceUrl" to "GrantGMGetResourceUrl",
            "GM.notification" to "GrantGMNotification",
            "GM.openInTab" to "GrantGMOpenInTab",
            "GM.registerMenuCommand" to "GrantGMRegisterMenuCommand",
            "GM.setClipboard" to "GrantGMSetClipboard",
            "GM.xmlHttpRequest" to "GrantGMXmlHttpRequest",
            "unsafeWindow" to "GrantUnsafeWindow",
        )
        private val tmGrants = mapOf(
            "GM.addStyle" to "GrantTMAddStyle",
            "GM.addElement" to "GrantTMAddElement",
            "GM.addValueChangeListener" to "GrantTMAddValueChangeListener",
            "GM.removeValueChangeListener" to "GrantTMRemoveValueChangeListener",
            "GM.log" to "GrantTMLog",
            "GM.getResourceText" to "GrantTMGetResourceText",
            "GM.unregisterMenuCommand" to "GrantTMUnregisterMenuCommand",
            "GM.download" to "GrantTMDownload",
            "GM.getTab" to "GrantTMGetTab",
            "GM.saveTab" to "GrantTMSaveTab",
            "GM.getTabs" to "GrantTMGetTabs",
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun apply(project: Project) {
        // TODO: this does screw over multiplatform projects, but multiplatform project is way out of scope for this
        //       plugin atm
        val kotlinJs = project.kotlinExtension as? KotlinJsProjectExtension ?: error("Project is not Kotlin/JS")
        val userskripterDirectory = project.buildDir.resolve("userskripter")
        val generatedDirectory = project.buildDir.resolve("generated/userskripter/kotlin/")
        val extension = project.extensions.create("userskripter", UserskripterExtension::class.java, project)
        val metadata = (extension as ExtensionAware).extensions.create(
            "metadata",
            UserskripterMetadataBlockExtension::class.java,
            project,
            extension,
        )
        val metadataTranspiler = (extension as ExtensionAware).extensions.create(
            "metadataTranspiler",
            UserskripterMetadataTranspilerExtension::class.java,
            project,
            generatedDirectory,
        )

        kotlinJs.sourceSets.named("main") {
            kotlin.srcDir(generatedDirectory)
        }

        project.task("printUserscriptMetadata") {
            group = "userskripter"
            doLast {
                print(serializeMetadataBlock(metadata))
            }
        }

        project.afterEvaluate {
            // greasemonkey api stuff should be supported in virtually every script-engine, so we just use that as a base
            // we still annotate greasemonkey stuff with opt-in, just in case we encounter an engine that is not
            // compatible
            addGrants(project, metadata, gmGrants, GREASE_MONKEY_PATH)

            when (extension.scriptEngine) {
                ScriptEngine.GREASE_MONKEY -> {
                    addOptIns(project, listOf("${ENGINES_PACKAGE}.ScriptEngineGreaseMonkey", UNSAFE_WINDOW_COMPATIBLE))
                }

                ScriptEngine.TAMPER_MONKEY -> {
                    addOptIns(project, listOf("${ENGINES_PACKAGE}.ScriptEngineTamperMonkey", UNSAFE_WINDOW_COMPATIBLE))
                    addGrants(project, metadata, tmGrants, TAMPER_MONKEY_PATH)
                }
            }

            val webpackTask = project.tasks.getByPath(extension.mode.taskName) as KotlinWebpack
            val metaFile = userskripterDirectory.resolve("${extension.id}.meta.js")
            val userFile = userskripterDirectory.resolve("${extension.id}.user.js")
            val userSourceMapFile = userskripterDirectory.resolve("${extension.id}.user.js.map")
            val transpiledFile = metadataTranspiler.outputDirectory.resolve("${metadataTranspiler.outputName}.kt")

            val generateMetaFile = project.task("generateMetaFile") {
                outputs.file(metaFile)
                inputs.properties(metadata.buildGradleProperties())
                group = "userskripter"

                doLast {
                    metaFile.writeText(serializeMetadataBlock(metadata))
                }
            }

            val generateUserFile = project.task("generateUserFile") {
                dependsOn(webpackTask)
                inputs.properties(metadata.buildGradleProperties())
                inputs.properties("mode" to extension.mode, "id" to extension.id)
                inputs.file(webpackTask.outputFile)
                outputs.file(userFile)
                group = "userskripter"

                doLast {
                    // TODO: can we do this writing more efficiently?
                    userFile.writeText(serializeMetadataBlock(metadata))
                    userFile.appendBytes(webpackTask.outputFile.readBytes())
                }
            }

            // TODO: is this proper?
            val copyUserSourceMap = project.task("copyUserSourceMap") {
                val sourceMapFile = webpackTask.destinationDirectory.resolve("${webpackTask.outputFileName}.map")
                dependsOn(generateUserFile)
                inputs.file(sourceMapFile)
                outputs.file(userSourceMapFile)
                group = "userskripter"

                doLast {
                    if (sourceMapFile.exists()) {
                        sourceMapFile.copyTo(userSourceMapFile)
                    } else {
                        logger.warn("Could not find a source-map at: $sourceMapFile")
                    }
                }
            }

            // TODO: if used this should run first of all the tasks in 'generateUserscript', but it doesn't rn
            val generateUserscriptKotlinFile = project.task("generateUserscriptKotlinFile") {
                inputs.properties(metadata.buildGradleProperties())
                inputs.properties(metadataTranspiler.properties.toGradlePropertyMap())
                outputs.file(transpiledFile)
                group = "userskripter"

                doLast {
                    transpiledFile.writeText(compileToKotlin(extension, metadata, metadataTranspiler))
                }
            }

            project.task("generateUserscript") {
                setDependsOn(buildList {
                    add(generateMetaFile)
                    add(generateUserFile)
                    if (extension.includeSourceMap) add(copyUserSourceMap)
                    if (metadataTranspiler.runOnGenerate) add(generateUserscriptKotlinFile)
                })
                outputs.files(metaFile, userFile)
                group = "userskripter"
            }
        }
    }

    private fun addGrants(
        project: Project,
        metadata: UserskripterMetadataBlockExtension,
        grants: Map<String, String>,
        path: String,
    ) {
        addOptIns(project, metadata.grant.mapNotNull { grants[it] }.map { "${path}.$it" })
    }

    private fun addOptIn(project: Project, annotation: String) {
        project.tasks.withType<Kotlin2JsCompile>().configureEach {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-opt-in=${annotation}"
            }
        }
    }

    private fun addOptIns(project: Project, annotations: Iterable<String>) {
        project.tasks.withType<Kotlin2JsCompile>().configureEach {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + annotations.map { "-opt-in=$it" }
            }
        }
    }
}