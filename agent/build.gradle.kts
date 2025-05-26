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
            implementation(project(":glasses"))
            implementation(libs.binom.io.strong.webServer)
            implementation(libs.binom.io.strong.properties.ini)
            implementation(libs.binom.io.strong.properties.yaml)
            implementation(libs.binom.io.signal)
            implementation(libs.binom.io.postgresql)
            implementation(libs.binom.io.db.serialization.core)
            implementation(libs.binom.io.kmigrator)
            implementation(libs.binom.io.http.ssl)
            implementation(libs.binom.io.xml.core)
            implementation(libs.binom.io.s3.client)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.binom.telegram)
            implementation(libs.binom.io.strong.nats.client)
            implementation(libs.binom.io.strong.application)
            implementation(libs.device.telegram)
            implementation(libs.binom.io.nats)
        }
        jvmMain.dependencies {
            implementation("io.github.givimad:whisper-jni:1.7.1")
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