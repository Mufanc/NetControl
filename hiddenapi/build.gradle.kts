plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "xyz.mufanc.netc.hiddenapi"
    compileSdk = 36

    defaultConfig { minSdk = 30 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(libs.refine.annotation)
    annotationProcessor(libs.refine.annotation.processor)
}
