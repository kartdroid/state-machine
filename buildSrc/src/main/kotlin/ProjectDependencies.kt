object Versions {
    const val kotlin = "1.6.0"
    const val junitJupiter = "5.5.1"
    const val junitJupiterPlatformLauncher = "1.5.1"
}

object Deps {
    object Test {
        object JunitJupiter {
            const val API = "org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiter}"
            const val PARAMS = "org.junit.jupiter:junit-jupiter-params:${Versions.junitJupiter}"
            const val RUNTIME_LAUNCHER = "org.junit.platform:junit-platform-launcher:${Versions.junitJupiterPlatformLauncher}"
            const val RUNTIME_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.junitJupiter}"
            const val RUNTIME_VINTAGE_ENGINE = "org.junit.vintage:junit-vintage-engine:${Versions.junitJupiter}"
        }
    }
}