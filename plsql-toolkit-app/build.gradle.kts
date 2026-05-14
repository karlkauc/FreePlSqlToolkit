plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

dependencies {
    implementation(project(":plsql-parser"))
    implementation(project(":plsql-linter-core"))
    implementation(project(":plsql-db-sync"))
    implementation(project(":plsql-app-services"))

    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    implementation("org.fxmisc.richtext:richtextfx:0.11.7")
    implementation("io.github.mkpaz:atlantafx-base:2.1.0")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.4.0")

    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("org.testfx:openjfx-monocle:21.0.2")
}

tasks.withType<Test> {
    jvmArgs(
        "-Djava.awt.headless=true",
        "-Dtestfx.robot=glass",
        "-Dtestfx.headless=true",
        "-Dprism.order=sw",
        "-Dprism.text=t2k",
        "-Dglass.platform=Monocle",
        "-Dmonocle.platform=Headless"
    )
}

javafx {
    version = "22.0.2"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web")
}

application {
    mainClass.set("org.fxt.freeplsql.app.MainApp")
}
