plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.5.31"
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
}

group = "net.ormr.userskripter"
version = "0.1.0"
description = "Gradle plugin for creating userscripts with Kotlin/JS"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(gradleApi())

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")

    testImplementation(kotlin("test"))
}

pluginBundle {
    website = "https://github.com/Olivki/userskripter-gradle"
    vcsUrl = "https://github.com/Olivki/userskripter-gradle"
    tags = listOf("codegen", "userscript")
}

gradlePlugin {
    plugins {
        create("userskripter-plugin") {
            id = "net.ormr.userskripter.plugin"
            displayName = "Userskripter Gradle Plugin"
            description = project.description
            implementationClass = "net.ormr.userskripter.plugin.UserskripterPlugin"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "8"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}