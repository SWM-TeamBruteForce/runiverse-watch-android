plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.runiverse.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.runiverse.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "env"
    productFlavors {
        // 폰 dev 빌드와 applicationId가 같아야 Data Layer로 통신된다
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
        }
        create("prod") {
            dimension = "env"
        }
    }

    buildFeatures {
        compose = true
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
        // 버전은 IntelliJ에서 열리는 값으로 고정해 둔다
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion")
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
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
}
