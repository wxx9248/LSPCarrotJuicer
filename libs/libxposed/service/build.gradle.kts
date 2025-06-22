plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "io.github.libxposed.service"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("upstream/service/src/main/AndroidManifest.xml")
            java.setSrcDirs(listOf("upstream/service/src/main/java"))
            aidl.setSrcDirs(listOf("upstream/interface/src/main/aidl"))
        }
    }

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = false
        resValues = false
        aidl = true
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}
