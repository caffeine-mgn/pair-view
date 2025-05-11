plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    js {
        browser()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.html.core)
            implementation(compose.runtime)
        }
    }
}