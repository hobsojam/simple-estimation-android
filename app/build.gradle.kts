plugins {
    alias(libs.plugins.android.application)
    // kotlin.android is applied transitively by kotlin.plugin.compose / AGP in Kotlin 2.x — do not add it explicitly
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.hobsojam.simpleestimation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hobsojam.simpleestimation"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
    }
}

kover {
    reports {
        filters {
            excludes {
                classes("*.BuildConfig", "*.ComposableSingletons\$*", "*.R", "*.R\$*")
            }
        }
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    failBuildOnCVSS = 7.0f
    failOnError = true
    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    data.directory =
        rootProject.layout.buildDirectory
            .dir("dependency-check-data")
            .get()
            .asFile
            .absolutePath
    analyzers.assemblyEnabled = false
    nvd.delay = 8000
    nvd.maxRetryCount = 10

    providers.environmentVariable("NVD_API_KEY").orNull
        ?.takeIf { it.isNotBlank() }
        ?.let { nvd.apiKey = it }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget.set("17")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}
