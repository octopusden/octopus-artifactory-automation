plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.mock-server:mockserver-client-java:5.15.0")
}
