pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://build-artifacts.signal.org/libraries/maven/") { content { includeGroup("org.signal") } }
  }
}
rootProject.name = "Tonino"
include(":app", ":core")
