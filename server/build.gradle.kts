plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.binom.io.strong.webServer)
            implementation(libs.binom.io.strong.properties.ini)
            implementation(libs.binom.io.strong.properties.yaml)
            implementation(libs.binom.io.signal)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}