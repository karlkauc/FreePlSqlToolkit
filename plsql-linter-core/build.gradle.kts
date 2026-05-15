plugins {
    `java-library`
}

dependencies {
    api(project(":plsql-parser"))
    implementation("org.yaml:snakeyaml:2.6")
}
