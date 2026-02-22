/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.stability.analyzer)
    alias(libs.plugins.gradle.toolchains) apply false
    //alias(libs.plugins.kotlin.scripting)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

android {
    namespace = "com.machiav3lli.backup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.machiav3lli.backup"
        minSdk = 26
        targetSdk = 36
        versionCode = 8329
        versionName = "8.3.16"
        buildConfigField("int", "MAJOR", "8")
        buildConfigField("int", "MINOR", "3")

        testApplicationId = "$applicationId.tests"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            isMinifyEnabled = true
        }
        named("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        create("neo") {
            applicationIdSuffix = ".neo"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    val generateLocales by tasks.registering(GenerateBuildConfig::class) {
        resDir.set(project.layout.projectDirectory.dir("src/main/res"))
        outputDir.set(project.layout.buildDirectory.dir("generated/source/locales/kotlin/main"))
    }

    androidComponents.onVariants { variant ->
        variant.sources.java!!.addGeneratedSourceDirectory(
            generateLocales,
            GenerateBuildConfig::outputDir
        )

        tasks.withType<KotlinCompile> {
            dependsOn(generateLocales)
        }
    }

    androidComponents.onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                output.outputFileName.set(
                    "Neo_Backup_${output.versionName.get()}_${variant.buildType}.apk"
                )
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    lint {
        checkReleaseBuilds = false
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        resources {
            excludes += listOf(
                "/DebugProbesKt.bin",
                "/kotlin/**.kotlin_builtins",
                "/kotlin/**.kotlin_metadata",
                "/META-INF/**.kotlin_module",
                "/META-INF/**.pro",
                "/META-INF/**.version",     // comment out to enable layout inspector
                "/META-INF/LICENSE-notice.md",
                "/META-INF/LICENSE.md",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            )
        }
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp)
    // not yet necessary: implementation(libs.kotlin.reflect)

    // Koin
    api(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.startup)
    implementation(libs.koin.annotations)
    ksp(libs.koin.compiler)

    // Libs
    implementation(libs.activity.compose)
    implementation(libs.collections.immutable)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // TODO use the new WorkInfo.stopReason (report stopReason), WorkManager.getWorkInfosFlow (Flow instead of LiveData), setNextScheduleTimeOverride (Precise scheduling), Configuration.Builder.setContentUriTriggerWorkersLimit (limit for content uri workers)
    implementation(libs.work.runtime)
    implementation(libs.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.lifecycle)
    implementation(libs.biometric)
    implementation(libs.kaml)
    implementation(libs.security.crypto)
    implementation(libs.commons.io)
    implementation(libs.commons.compress)
    implementation(variantOf(libs.zstd.jni) { artifactType("aar") })
    implementation(libs.pgpainless)
    implementation(libs.timber)
    implementation(libs.semver)
    implementation(libs.libsu.core)
    implementation(libs.libsu.io)
    implementation(libs.documentfile)

    // UI
    implementation(libs.material)
    implementation(libs.preference)

    // Compose
    api(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.tracing)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.navigationsuite)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.adaptive.layout)
    implementation(libs.compose.adaptive.navigation)
    implementation(libs.compose.animation)
    implementation(libs.compose.navigation)
    implementation(libs.compose.navigation3)
    implementation(libs.compose.navigation3.ui)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)

    // Testing
    androidTestImplementation(libs.test.runner)
    implementation(libs.test.rules)
    implementation(libs.test.ext)

    // compose testing
    // Test rules and transitive dependencies:
    androidTestImplementation(libs.compose.ui.test.junit4)
    // Needed for createComposeRule, but not createAndroidComposeRule:
    debugImplementation(libs.compose.ui.test.manifest)
    //---------------------------------------- hg42
    // can only be enabled on demand, otherwise it conflicts with compilation
    // TODO hg42 without the library the .main.kts script still works, but syntax checking is not working
    //implementation(libs.kotlin.main.kts)
    implementation(kotlin("script-runtime"))    // for intellisense in kts scripts
}

abstract class GenerateBuildConfig : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val resDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = "build"
        description = "Generates BuildConfig.kt from auto-fetched values"
    }

    @TaskAction
    fun generate() {
        val detectedLocales = mutableSetOf<String>()
        resDir.get().asFileTree.visit {
            if (file.isFile && file.name == "strings.xml" && file.readText().contains("<string")) {
                val languageCode = file.parentFile?.name?.removePrefix("values-")?.let {
                    if (it == "values") "en" else it
                }
                languageCode?.let { detectedLocales.add(it) }
            }
        }

        val outputFile =
            outputDir.file("com/machiav3lli/backup/config/BuildConfig.kt").get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package com.machiav3lli.backup.config
            
            object BuildConfig {
                val DETECTED_LOCALES: Array<String> = arrayOf(${
                detectedLocales.sorted().joinToString { "\"$it\"" }
            })
            }
        """.trimIndent()
        )

        println("BuildConfig: Generated ${detectedLocales.size} locales to ${outputFile.absolutePath}")
    }
}

tasks.withType<Test> {
    useJUnit() // we still use junit4
    // useTestNG()
    // useJUnitPlatform()
}


// Exclude (non-gradle) kts scripts from compilation
tasks.withType<KotlinCompile>().configureEach {
    setSource(sources.filterNot {
        //it.name.endsWith(".generator.kts")
        it.extension == "kts"
    })
    compilerOptions {
        if (project.findProperty("enableComposeCompilerReports") == "true") {
            val metricsDir =
                "${project.layout.buildDirectory.asFile.get().absolutePath}/compose_metrics"
            println("--- enableComposeCompilerReports -> $metricsDir")
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$metricsDir",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir",
            )
        }
    }
}
