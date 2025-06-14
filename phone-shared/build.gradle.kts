plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.binom.publish)
    id("maven-publish")
}

kotlin {
    jvm()
    js()
    linuxX64()
    mingwX64()
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.serialization.protobuf)
            api(libs.kotlinx.serialization.json)
            api(libs.binom.io.core)
        }
    }
}