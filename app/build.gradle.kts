plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.myassistant"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.myassistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        resources.pickFirsts += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // 真正的背景模糊（玻璃效果）
    implementation("dev.chrisbanes.haze:haze:1.7.2")
    // 应用内无线调试（配对码 + 连接 + shell）
    implementation("com.github.MuntashirAkon:libadb-android:1.0.1")
    implementation("com.github.MuntashirAkon:sun-security-android:1.1")
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    // 用 Bouncy Castle 替代 Android 不可用的 sun.security.x509 内部 API
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.78.1")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.78.1")
}
