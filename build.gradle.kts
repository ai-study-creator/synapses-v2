plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    group = "com.synapses"
    version = "1.0.0"

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion("2.2.21")
                because("The project uses Kotlin compiler 2.1.0, which can read metadata up to 2.2.x but not 2.3.x.")
            }
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization")) {
                useVersion("1.9.0")
                because("Avoid Kotlin 2.3 metadata from kotlinx.serialization 1.11.x.")
            }
        }
    }
}
