import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "net.mamoe"
version = "1.0-SNAPSHOT"

val serializationVersion = "1.0.0"

fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"


repositories {
    mavenCentral()
    google()
    jcenter()
}

val ktorVersion = "2.0.1"

dependencies {
    testImplementation(kotlin("test"))

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.ktor:ktor-html-builder:1.6.8")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    implementation(kotlinx("serialization-core", serializationVersion))
    implementation(kotlinx("serialization-json", serializationVersion))
}

tasks.test {
    useJUnit()
}


tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}