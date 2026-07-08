plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.subscriptionmanager"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.subscriptionmanager"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "2.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "password123"
            keyAlias = "release"
            keyPassword = "password123"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}



kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Supabase
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.auth)
  implementation(libs.ktor.client.android)
  implementation(libs.kotlinx.serialization.json)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)

  // Credential Manager for Native Google Sign-In
  implementation("androidx.credentials:credentials:1.3.0-alpha01")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha01")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
}
