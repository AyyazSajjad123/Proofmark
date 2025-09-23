pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ProofMark"

include(
    ":app",
    ":core:ui",
    ":core:common",
    ":core:data",
    ":core:crypto",
    ":core:pdf",
    ":feature:capture",
    ":feature:library",
    ":feature:verify",
    ":feature:report",
    ":feature:settings",
    ":work"
)

// (optional safety – keep if your folder names match)
project(":app").projectDir = file("app")
project(":core:ui").projectDir = file("core/ui")
project(":core:common").projectDir = file("core/common")
project(":core:data").projectDir = file("core/data")
project(":core:crypto").projectDir = file("core/crypto")
project(":core:pdf").projectDir = file("core/pdf")
project(":feature:capture").projectDir = file("feature/capture")
project(":feature:library").projectDir = file("feature/library")
project(":feature:verify").projectDir = file("feature/verify")
project(":feature:report").projectDir = file("feature/report")
project(":feature:settings").projectDir = file("feature/settings")
project(":work").projectDir = file("work")
