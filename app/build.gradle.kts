import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.fdev.nutrimindai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fdev.nutrimindai"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        val metaApiKey = properties.getProperty("META_AI_API_KEY")
            ?: properties.getProperty("FIREWORKS_API_KEY")
            ?: "YOUR_API_KEY_HERE"
        val metaBaseUrl = properties.getProperty("META_AI_BASE_URL")
            ?: "https://api.meta.ai/v1/"
        val metaVisionModel = properties.getProperty("META_AI_VISION_MODEL")
            ?: "muse-spark-1.3"
        val metaTextModel = properties.getProperty("META_AI_TEXT_MODEL")
            ?: "muse-spark-1.3"

        buildConfigField("String", "META_AI_API_KEY", "\"$metaApiKey\"")
        buildConfigField("String", "META_AI_BASE_URL", "\"$metaBaseUrl\"")
        buildConfigField("String", "META_AI_VISION_MODEL", "\"$metaVisionModel\"")
        buildConfigField("String", "META_AI_TEXT_MODEL", "\"$metaTextModel\"")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

val dbVersion = 13

androidComponents {
    onVariants { variant ->
        val variantName = variant.name
        val taskName = "generate${variantName.replaceFirstChar { it.uppercase() }}DbVersion"

        val generateTask = tasks.register(
            taskName,
            GenerateDbVersionTask::class.java
        ) {
            outputDir.set(
                layout.buildDirectory.dir(
                    "generated/source/dbversion/$variantName"
                )
            )
            this.dbVersionValue.set(dbVersion)
        }

        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateTask,
            GenerateDbVersionTask::outputDir
        )

        variant.sources.java?.addGeneratedSourceDirectory(
            generateTask,
            GenerateDbVersionTask::outputDir
        )
    }
}

tasks.named("preBuild").configure {
    dependsOn(tasks.matching { it.name.endsWith("DbVersion") })
}

abstract class GenerateDbVersionTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val dbVersionValue: Property<Int>

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        val version = dbVersionValue.get()

        File(dir, "DbVersion.kt").writeText(
            """
            package com.fdev.nutrimindai.core.database

            const val DB_VERSION = $version
            """.trimIndent()
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.startup)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.mlkit.barcode.scanning)
    implementation(libs.androidx.health.connect)


    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
