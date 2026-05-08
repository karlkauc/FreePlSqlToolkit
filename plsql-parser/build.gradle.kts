plugins {
    java
    antlr
    `java-library`
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    api("org.antlr:antlr4-runtime:4.13.2")
}

tasks.generateGrammarSource {
    arguments = arguments + listOf(
        "-visitor",
        "-long-messages",
        "-package", "org.fxt.freeplsql.parser.gen"
    )
    outputDirectory = layout.buildDirectory.dir(
        "generated-src/antlr/main/org/fxt/freeplsql/parser/gen"
    ).get().asFile
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-src/antlr/main"))
        }
    }
}

tasks.named("compileJava") {
    dependsOn("generateGrammarSource")
}
