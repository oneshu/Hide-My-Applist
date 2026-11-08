import com.android.build.api.dsl.Packaging
import java.util.*

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.autoresconfig)
    alias(libs.plugins.materialthemebuilder)
    alias(libs.plugins.refine)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.nav.safeargs.kotlin)
}

android {
    namespace = "com.tsng.hidemyapplist"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging {
        dex.useLegacyPackaging = true
        resources {
            excludes += arrayOf(
                "/META-INF/*",
                "/META-INF/androidx/**",
                "/kotlin/**",
                "/okhttp3/**",
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
}

autoResConfig {
    generateClass.set(true)
    generateRes.set(false)
    generatedClassFullName.set("icu.nullptr.hidemyapplist.util.LangList")
    generatedArrayFirstItem.set("SYSTEM")
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf("Blue" to "2196F3")) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = true
}

fun afterEval() = android.applicationVariants.forEach { variant ->
    val variantCapped = variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
    val variantLowered = variant.name.lowercase(Locale.ROOT)

    tasks.register<Sync>("build$variantCapped") {
        dependsOn("assemble$variantCapped")
        from(layout.buildDirectory.dir("outputs/apk/$variantLowered"))
        into(layout.buildDirectory.dir("apk/$variantLowered"))
        rename(".*.apk", "HMA-V${variant.versionName}-${variant.buildType.name}.apk")
    }
}

afterEvaluate {
    afterEval()
}

dependencies {
    implementation(projects.common)
    runtimeOnly(projects.xposed)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.com.drakeet.about)
    implementation(libs.com.drakeet.multitype)
    implementation(libs.com.github.kirich1409.viewbindingpropertydelegate)
    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.google.android.material)
    implementation(libs.dev.rikka.hidden.compat)
    implementation(libs.dev.rikka.rikkax.material)
    implementation(libs.dev.rikka.rikkax.material.preference)
    compileOnly(libs.dev.rikka.hidden.stub)
}

configurations.all {
    exclude("androidx.appcompat", "appcompat")
}
