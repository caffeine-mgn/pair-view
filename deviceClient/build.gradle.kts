plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":deviceShared"))
            implementation(project(":shared"))
            implementation(libs.binom.io.http.client)
            implementation(libs.binom.io.network)
            implementation(libs.binom.io.logger)
        }
    }
}