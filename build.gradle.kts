import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21" apply false
}

allprojects {
    group = "pl.pawelkielb.xchat"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

extra["kotestVersion"] = "5.3.0"
extra["mockkVersion"] = "1.12.3"
