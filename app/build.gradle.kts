import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// 폰 앱과 같은 업로드 키여야 Play가 같은 앱으로 받는다
// 파일이 없으면 debug 키로 빌드한다. 그 산출물은 Play가 받지 않는다
fun loadKeystore(fileName: String): Properties? =
    rootProject.file(fileName).takeIf { it.isFile }?.let { file ->
        Properties().apply { FileInputStream(file).use { load(it) } }
    }

val prodKeystore = loadKeystore("key.properties")
val devKeystore = loadKeystore("key-dev.properties")

android {
    namespace = "com.runiverse.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.runiverse.app"
        minSdk = 30
        targetSdk = 36
        // 배포 workflow가 10000 + 실행 번호를 넘긴다 — 폰은 실행 번호 그대로라 같은 앱 안에서 겹치지 않는다
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = "1.3.0"
    }

    signingConfigs {
        fun uploadKey(name: String, keystore: Properties) = create(name) {
            keyAlias = keystore["keyAlias"] as String
            keyPassword = keystore["keyPassword"] as String
            storeFile = file(keystore["storeFile"] as String)
            storePassword = keystore["storePassword"] as String
        }
        prodKeystore?.let { uploadKey("prod", it) }
        devKeystore?.let { uploadKey("dev", it) }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "env"
    productFlavors {
        // 폰 dev 빌드와 applicationId가 같아야 Data Layer로 통신된다
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            signingConfig = signingConfigs.findByName("dev") ?: signingConfigs.getByName("debug")
        }
        create("prod") {
            dimension = "env"
            signingConfig = signingConfigs.findByName("prod") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        // 폰에 워치 버전을 알리려고 켠다 — AGP 기본값은 꺼져 있다
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric이 Android 16 환경을 준비할 때 JDK 내부 API를 쓴다
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }

    lint {
        // 버전은 Android Studio에서 열리는 AGP와 Wear Compose에 맞춰 고정해 둔다
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion")
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    // 코드에서 직접 쓰지 않는다 — Play 서비스가 끌어오는 옛 버전을 권한 요청 API가 요구하는 버전으로 올린다
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.wear.compose.material3)
    // 미리보기 어노테이션만 담긴다 — 그리는 도구는 debug 빌드에만 넣는다
    implementation(libs.androidx.wear.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.wear.ongoing)
    implementation(libs.androidx.health.services.client)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // 화면 하나만 띄우는 테스트의 빈 액티비티를 manifest에 넣는다
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
