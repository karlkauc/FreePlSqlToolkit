plugins {
    java
}

dependencies {
    implementation(project(":plsql-parser"))
    implementation(project(":plsql-linter-core"))
    implementation(project(":plsql-db-sync"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("io.github.java-diff-utils:java-diff-utils:4.17")
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

    runtimeOnly("com.oracle.database.jdbc:ojdbc11:23.26.2.0.0")

    testImplementation("org.mockito:mockito-core:5.23.0")
}
