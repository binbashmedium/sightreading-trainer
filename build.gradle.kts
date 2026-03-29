// Root build — only plugins required by always-included submodules go here.
// Android-specific plugins (AGP, Hilt, KSP) are declared in :app/build.gradle.kts
// because :app is only included when an Android SDK is present (see settings.gradle.kts).
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
}
