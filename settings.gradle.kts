pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.android.application") version "8.8.0"
        id("org.jetbrains.kotlin.android") version "2.1.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
        id("com.google.dagger.hilt.android") version "2.57.1"
        id("com.google.devtools.ksp") version "2.1.21-2.0.2"
    }
}

// AGP classes (BaseVariant, BaseExtension, etc.) exist in the AGP plugin classloader but are
// also referenced by KGP's KotlinAndroidTarget. Due to Gradle classloader isolation, KGP cannot
// see AGP classes when decorating its managed types. Adding AGP to the settings buildscript
// classpath puts these types in a parent classloader visible to all plugin classloaders.
// javapoet is forced to 1.13.0 because AGP brings in 1.10.0 which is missing canonicalName()
// required by Hilt 2.57.1.
buildscript {
    configurations.classpath {
        resolutionStrategy {
            force("com.squareup:javapoet:1.13.0")
        }
    }
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.0")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SightreadingTrainer"

include(":domain")
include(":app")
