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
    "plsql-toolkit-app",
)
