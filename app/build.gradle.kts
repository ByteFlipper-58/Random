plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

apply(plugin = "com.google.dagger.hilt.android")

android {
    namespace = "com.byteflipper.random"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.byteflipper.random"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "v1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "ru", "uk", "be", "pl", "kk", "hi", "es", "fr")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property",
            "-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi"
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Confetti module
    implementation(project(":confetti"))

    // AndroidX и Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.splashscreen)

	implementation(libs.play.services.ads)
	implementation(libs.androidx.lifecycle.process)

    // Firebase
    implementation(libs.firebase.crashlytics)

    // Play Billing и прочее
    implementation(libs.play.billing)
    implementation(libs.reorderable)
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.review.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

	// UMP (User Messaging Platform) — форма согласия пользователей
	implementation(libs.ump)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    ksp(libs.hilt.compiler)

    // Room annotation processor
    ksp(libs.androidx.room.compiler)

    implementation(libs.smooth.corner.rect)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
