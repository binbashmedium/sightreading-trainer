plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.21" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    kover(project(":domain"))
    kover(project(":app"))
}

kover {
    reports {
        total {
            filters {
                excludes {
                    classes(
                        "com.binbashmedium.sightreadingtrainer.Hilt_*",
                        "com.binbashmedium.sightreadingtrainer.*_Factory",
                        "com.binbashmedium.sightreadingtrainer.di.*",
                        "com.binbashmedium.sightreadingtrainer.SightReadingApp*",
                        "com.binbashmedium.sightreadingtrainer.MainActivity*",
                        "com.binbashmedium.sightreadingtrainer.ui.*",
                        "com.binbashmedium.sightreadingtrainer.core.midi.*",
                        "com.binbashmedium.sightreadingtrainer.data.SettingsDataStore*",
                        "com.binbashmedium.sightreadingtrainer.data.SettingsRepository*",
                        "com.binbashmedium.sightreadingtrainer.data.ExerciseRepository*"
                    )
                }
            }
            xml {
                onCheck = false
            }
            html {
                onCheck = false
            }
        }
    }
}
