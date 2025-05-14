plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()
    js()
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.serialization.protobuf)
            api(libs.kotlinx.serialization.json)
            api(libs.binom.io.core)
        }
        commonTest.dependencies {
            api(kotlin("test-common"))
            api(kotlin("test-annotations-common"))
        }
        jvmTest.dependencies {
            api(kotlin("test-junit"))
        }
    }
}