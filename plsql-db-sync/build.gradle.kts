plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")
    implementation("com.oracle.database.jdbc:ojdbc11:23.26.1.0.0")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.fxt.freeplsql.sync.cli.SyncCli"
    }
}

tasks.named<Jar>("shadowJar") {
    archiveClassifier.set("all")
    archiveBaseName.set("plsqlsync")
    manifest {
        attributes["Main-Class"] = "org.fxt.freeplsql.sync.cli.SyncCli"
    }
}

// Testcontainers-backed end-to-end test against a live Oracle XE container is
// stubbed out for now; see TODO in plsql-db-sync/README.md.
