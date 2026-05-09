pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "FreePlSqlToolkit"

include(
    "plsql-parser",
    "plsql-linter-core",
    "plsql-linter-cli",
    "plsql-db-sync",
    "plsql-app-services",
    "plsql-toolkit-app",
)
