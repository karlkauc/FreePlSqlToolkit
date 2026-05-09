plugins {
    java
}

dependencies {
    implementation(project(":plsql-parser"))
    implementation(project(":plsql-linter-core"))
    implementation(project(":plsql-db-sync"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

    runtimeOnly("com.oracle.database.jdbc:ojdbc11:23.6.0.24.10")

    testImplementation("org.mockito:mockito-core:5.14.2")
}
