plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
}

allprojects {
    if (version == "unspecified") {
        version = "1.0.0-SNAPSHOT"
    }
    group = "pw.binom.device.android"
}

//tasks {
//    val clean by register<Delete>("clean") {
//        delete(rootProject.layout.buildDirectory)
//    }
//}
