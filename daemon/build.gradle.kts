plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.refine)
    alias(libs.plugins.aproc)
}

android {
    namespace = "xyz.mufanc.netc"
    compileSdk = 36

    defaultConfig {
        applicationId = "xyz.mufanc.netc"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging.resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/versions/**")
}

dependencies {
    compileOnly(project(":hiddenapi"))
    implementation(libs.refine.runtime)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}
