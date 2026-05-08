plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    implementation(project(":plsql-linter-core"))
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.fxt.freeplsql.cli.LintCli"
    }
}

tasks.named<Jar>("shadowJar") {
    archiveClassifier.set("all")
    archiveBaseName.set("plsqllint")
    manifest {
        attributes["Main-Class"] = "org.fxt.freeplsql.cli.LintCli"
    }
}
