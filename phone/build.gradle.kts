plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
}

repositories {
//    mavenLocal()
//    maven(url = "https://repo.binom.pw")
//    mavenCentral()
}

android {
    compileSdk = 35
    namespace = "pw.binom.video"
    defaultConfig {
        applicationId = namespace
        minSdk = 32
        targetSdk = minSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.fragment:fragment-ktx:1.5.3")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("com.google.android.exoplayer:exoplayer:2.17.1")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(project(":android-common"))
    implementation(project(":phone-shared"))
    implementation(libs.device.shared)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // 2. aar文件引入模式
//    implementation(fileTree("libs"))
    implementation("androidx.appcompat:appcompat:1.3.0")
//    implementation(project(":glasses"))
//    implementation(project(":deviceClient"))
    implementation("androidx.activity:activity-ktx:1.3.0-alpha08")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    // kotlin & coroutines
    implementation("androidx.core:core-ktx:1.8.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.binom.io.logger)
    implementation(libs.binom.io.network)
    implementation(libs.binom.io.http.client)


    implementation(libs.kotlinx.serialization.protobuf)
}
