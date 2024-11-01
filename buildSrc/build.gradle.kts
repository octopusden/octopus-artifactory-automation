plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.mock-server:mockserver-client-java:${properties["mock-server.version"]}")
    implementation("org.octopusden.octopus.octopus-external-systems-clients:artifactory-client:${properties["artifactory-client.version"]}")
}
