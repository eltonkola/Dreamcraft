import org.gradle.kotlin.dsl.implementation
import java.io.FileInputStream
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.serialization)
}

// Function to safely load properties from local.properties
fun getApiKey(project: Project, propertyName: String): String {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val properties = Properties()
        FileInputStream(localPropertiesFile).use { fis ->
            properties.load(fis)
        }
        // Return property value or an empty string if not found
        // The quotes are expected to be part of the value in local.properties
        return properties.getProperty(propertyName, "\"\"")
    }
    // Fallback for CI: Read from environment variable if local.properties doesn't exist or key missing
    // Gradle automatically makes environment variables available as project properties
    // Note: env var names often match property names, but can be different if mapped in CI
    return project.findProperty(propertyName)?.toString() ?: "\"\"" // Default to empty string literal if not found anywhere
}


android {
    namespace = "com.eltonkola.dreamcraft"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eltonkola.dreamcraft"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val groqApiKey = getApiKey(project, "GROQ_API_KEY")
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {

    implementation(mapOf("name" to "library-embed-record-release", "ext" to "aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation( libs.androidx.datastore.preferences)

    implementation (libs.okhttp)
    implementation (libs.kotlinx.serialization.json)
    implementation (libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)

    implementation(libs.qawaz.compose.code.editor)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}