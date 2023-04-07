plugins {
    kotlin("js") version "1.8.0"
}

group = "com.kakaouo.viewer.phi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
}

kotlin {
    js {
        binaries.executable()
        browser {
            /*
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }*/
        }
    }
}