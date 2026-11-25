plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val natives by configurations.creating

android {
    namespace = "com.etheria.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.etheria.game"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("ETHERIA_KEYSTORE_PASS") ?: "etheria123"
                keyAlias = System.getenv("ETHERIA_KEY_ALIAS") ?: "etheria"
                keyPassword = System.getenv("ETHERIA_KEY_PASS") ?: "etheria123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (file("release.keystore").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        named("main") {
            assets.srcDirs("../assets")
            jniLibs.srcDirs("libs")
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/robovm/ios/robovm.xml",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.libgdx.android)
    implementation(libs.libgdx.box2d.core)
    implementation(libs.libgdx.freetype.core)

    val gdxVersion = libs.versions.libgdx.get()
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
}

val copyAndroidNatives by tasks.registering {
    doFirst {
        file("libs/armeabi-v7a").mkdirs()
        file("libs/arm64-v8a").mkdirs()
        natives.files.forEach { jar ->
            val outputDir = when {
                jar.name.contains("natives-arm64-v8a") -> file("libs/arm64-v8a")
                jar.name.contains("natives-armeabi-v7a") -> file("libs/armeabi-v7a")
                else -> null
            }
            if (outputDir != null) {
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("**/*.so")
                    eachFile {
                        path = name
                    }
                    includeEmptyDirs = false
                }
            }
        }
    }
}

tasks.matching {
    it.name.contains("package", ignoreCase = true) ||
        it.name.contains("merge", ignoreCase = true) ||
        it.name == "preBuild"
}.configureEach {
    dependsOn(copyAndroidNatives)
}
