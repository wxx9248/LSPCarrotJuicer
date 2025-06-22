plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "io.github.libxposed.api"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    
    sourceSets {
        val main by getting
        main.apply {
            java.setSrcDirs(listOf("upstream/api/src/main/java"))
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
        androidResources {
            enable = false
        }
        buildConfig = false
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}
