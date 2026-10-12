plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Avoid Gradle's Windows test-worker argfile encoding issue with Chinese workspace paths.
tasks.register("writeJvmTestClasspath") {
    dependsOn("compileDebugUnitTestKotlin", "bundleDebugClassesToRuntimeJar", "processDebugUnitTestJavaRes")
    doLast {
        val unitTest = tasks.named<Test>("testDebugUnitTest").get()
        layout.buildDirectory.file("jvm-test-classpath.txt").get().asFile
            .writeText(unitTest.classpath.asPath, Charsets.UTF_8)
    }
}

android {
    namespace = "com.zwds.today"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zwds.today"
        minSdk = 26
        targetSdk = 35
        versionCode = 18
        versionName = "1.4.2"

        // 預設只包 64 位元 ABI（MSI App Player 是 x86_64，真機是 arm64-v8a）
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Preserve the certificate used by the distributed APK; keystore remains local and ignored.
    signingConfigs.getByName("debug") {
        val publishedStore = rootProject.file(".android-home/published-debug.keystore")
        if (publishedStore.exists()) storeFile = publishedStore
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // V1.0 之前先共用 debug 簽名，方便每天裝模擬器
            // V1.0 發布前改為 release keystore
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "今日紫微-v${variant.versionName}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room schema 匯出目錄（schema 變更時 git 可追蹤 diff）
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            all {
                it.systemProperty("file.encoding", "UTF-8")
                it.systemProperty("sun.jnu.encoding", "UTF-8")
                it.jvmArgs("-Dfile.encoding=UTF-8")
            }
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // OkHttp（帳號授權與雲端解讀）
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Test (純 JVM 演算法測試)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
