import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// GitHub Actions numbers each build, so every published APK gets a higher
// version code and installs over the previous one. Local builds are 1.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// Set by the GitHub Actions workflow from the repo's secrets.
val signingKeystorePath: String? = System.getenv("SIGNING_KEYSTORE_PATH")
val signingKeystorePassword: String? = System.getenv("SIGNING_KEYSTORE_PASSWORD")
// The alias and key password are only needed when they differ from the
// defaults (a key made for this app, called "news-switcher", whose password
// is the same as the keystore's).
val signingKeyAlias: String = System.getenv("SIGNING_KEY_ALIAS").orEmpty().ifEmpty { "news-switcher" }
val signingKeyPassword: String? = System.getenv("SIGNING_KEY_PASSWORD").orEmpty().ifEmpty { signingKeystorePassword }

android {
    namespace = "io.github.rednick.newsswitcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.rdfn.newsswitcher" // The Google Play package name. Never change it once published.
        minSdk = 26 // Android 8.0, which covers almost every phone and Android TV in use.
        targetSdk = 36 // Google Play requires a recent target for new apps and updates.
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        // Every published build has to be signed with the same key or Android
        // refuses to install it as an update. It also serves as the upload
        // key for Google Play. GitHub Actions passes the key in from the
        // repo's secrets (see README.md, "Signing key").
        signingKeystorePath?.let { keystorePath ->
            create("sideload") {
                storeFile = file(keystorePath)
                storePassword = signingKeystorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Without the key (e.g. building on your own computer), fall back
            // to Android's debug key. That APK works but can't update one
            // installed from GitHub.
            signingConfig = signingConfigs.findByName("sideload")
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Only used for hiding the status and navigation bars.
    implementation("androidx.core:core-ktx:1.13.1")
}
