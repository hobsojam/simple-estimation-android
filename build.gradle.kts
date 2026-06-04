plugins {
    alias(libs.plugins.gradle.doctor)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

doctor {
    disallowMultipleDaemons.set(false)

    javaHome {
        ensureJavaHomeMatches.set(true)
        ensureJavaHomeIsSet.set(true)
        failOnError.set(false)
    }
}
