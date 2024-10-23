import com.avast.gradle.dockercompose.ComposeExtension
import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.octopusden.octopus.task.ConfigureMockServer

plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("com.github.johnrengelman.shadow")
    id("com.avast.gradle.docker-compose")
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin")
    signing
}

group = "org.octopusden.octopus.automation.artifactory"
description = "Octopus Artifactory Automation"

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        suppressWarnings = true
        jvmTarget = "1.8"
    }
}

java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

tasks.withType<Test> {
    dependsOn("configureMockServer")
    useJUnitPlatform()
    testLogging {
        info.events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }
    systemProperties["jar"] = configurations["shadow"].artifacts.files.asPath
}

configure<ComposeExtension> {
    useComposeFiles.add(layout.projectDirectory.file("docker/docker-compose.yml").asFile.path)
    waitForTcpPorts.set(true)
    captureContainersOutputToFiles.set(layout.buildDirectory.dir("docker-logs"))
    environment.putAll(
        mapOf(
            "DOCKER_REGISTRY" to properties["docker.registry"],
            "MOCK_SERVER_VERSION" to properties["mock-server.version"],
        )
    )
}

tasks {
    val configureMockServer by registering(ConfigureMockServer::class)
}

tasks.named("configureMockServer") {
    dependsOn("composeUp")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.3.14")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    implementation("org.jfrog.artifactory.client:artifactory-java-client-services:${properties["artifactory-client.version"]}")
    //ToDo dependencies related to artifactory-java-client-services and must be transitive
    implementation("org.apache.groovy:groovy:4.0.23")
    implementation("org.apache.httpcomponents:httpcore:4.4.13")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.14.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${properties["junit.version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${properties["junit.version"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit.version"]}")
    testImplementation("it.skrape:skrapeit:1.2.2")
    testImplementation("org.mock-server:mockserver-client-java:${properties["mock-server.version"]}")
}

application {
    mainClass = "$group.ApplicationKt"
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.register<Zip>("zipMetarunners") {
    archiveFileName = "metarunners.zip"
    from(layout.projectDirectory.dir("metarunners")) {
        expand(properties)
    }
}

configurations {
    create("distributions")
}

val metarunners = artifacts.add(
    "distributions",
    layout.buildDirectory.file("distributions/metarunners.zip").get().asFile
) {
    classifier = "metarunners"
    type = "zip"
    builtBy("zipMetarunners")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("MAVEN_USERNAME"))
            password.set(System.getenv("MAVEN_PASSWORD"))
        }
    }
    transitionCheckOptions {
        maxRetries.set(60)
        delayBetween.set(Duration.ofSeconds(30))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(metarunners)
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/octopusden/${project.name}.git")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/octopusden/${project.name}.git")
                    connection.set("scm:git://github.com/octopusden/${project.name}.git")
                }
                developers {
                    developer {
                        id.set("octopus")
                        name.set("octopus")
                    }
                }
            }
        }
    }
}

signing {
    isRequired = System.getenv().containsKey("ORG_GRADLE_PROJECT_signingKey") && System.getenv()
        .containsKey("ORG_GRADLE_PROJECT_signingPassword")
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}

tasks.distZip.get().isEnabled = false
tasks.shadowDistZip.get().isEnabled = false
tasks.distTar.get().isEnabled = false
tasks.shadowDistTar.get().isEnabled = false