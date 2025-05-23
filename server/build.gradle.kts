plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.johnrengelman.shadow)
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":deviceShared"))
            implementation(libs.binom.io.strong.webServer)
            implementation(libs.binom.io.strong.properties.ini)
            implementation(libs.binom.io.strong.properties.yaml)
            implementation(libs.binom.io.strong.nats.client)
            implementation(libs.binom.io.signal)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

tasks {
    val jvmJar by getting(Jar::class)

    val shadowJar by creating(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        from(jvmJar.archiveFile)
        group = "build"
        configurations = listOf(project.configurations["jvmRuntimeClasspath"])
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/*.txt",
            "META-INF/NOTICE",
            "LICENSE",
        )
        manifest {
            attributes("Main-Class" to "pw.binom.JvmMain")
        }
    }
}