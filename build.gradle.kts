import com.avast.gradle.dockercompose.ComposeExtension
import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.octopusden.octopus.task.ConfigureMockServer
import java.net.InetAddress
import java.util.zip.CRC32

plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("com.github.johnrengelman.shadow")
    id("com.avast.gradle.docker-compose")
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin")
    id("org.octopusden.octopus.oc-template")
    signing
}

val defaultVersion = "${
    with(CRC32()) {
        update(InetAddress.getLocalHost().hostName.toByteArray())
        value
    }
}-snapshot"

if (version == "unspecified") {
    version = defaultVersion
}

group = "org.octopusden.octopus.automation.artifactory"
description = "Octopus Artifactory Automation"

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        suppressWarnings.set(true)
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

ext {
    System.getenv().let {
        set("signingRequired", it.containsKey("ORG_GRADLE_PROJECT_signingKey") && it.containsKey("ORG_GRADLE_PROJECT_signingPassword"))
        set("testPlatform", it.getOrDefault("TEST_PLATFORM", properties["test.platform"]))
        set("dockerRegistry", it.getOrDefault("DOCKER_REGISTRY", properties["docker.registry"]))
        set("octopusGithubDockerRegistry", it.getOrDefault("OCTOPUS_GITHUB_DOCKER_REGISTRY", project.properties["octopus.github.docker.registry"]))
        set("okdActiveDeadlineSeconds", it.getOrDefault("OKD_ACTIVE_DEADLINE_SECONDS", properties["okd.active-deadline-seconds"]))
        set("okdProject", it.getOrDefault("OKD_PROJECT", properties["okd.project"]))
        set("okdClusterDomain", it.getOrDefault("OKD_CLUSTER_DOMAIN", properties["okd.cluster-domain"]))
        set("okdWebConsoleUrl", (it.getOrDefault("OKD_WEB_CONSOLE_URL", properties["okd.web-console-url"]) as String).trimEnd('/'))
        set("mockServerVersion", it.getOrDefault("MOCK_SERVER_VERSION", properties["mock-server.version"]))
    }
}
val supportedTestPlatforms = listOf("docker", "okd")
if (project.ext["testPlatform"] !in supportedTestPlatforms) {
    throw IllegalArgumentException("Test platform must be set to one of the following $supportedTestPlatforms. Start gradle build with -Ptest.platform=... or set env variable TEST_PLATFORM")
}
val mandatoryProperties = mutableListOf("dockerRegistry", "octopusGithubDockerRegistry")
if (project.ext["testPlatform"] == "okd") {
    mandatoryProperties.add("okdActiveDeadlineSeconds")
    mandatoryProperties.add("okdProject")
    mandatoryProperties.add("okdClusterDomain")
}
fun String.getExt() = project.ext[this].toString()

configure<ComposeExtension> {
    useComposeFiles.add(layout.projectDirectory.file("docker/docker-compose.yml").asFile.path)
    waitForTcpPorts.set(true)
    captureContainersOutputToFiles.set(layout.buildDirectory.dir("docker-logs"))
    environment.putAll(
        mapOf(
            "DOCKER_REGISTRY" to "dockerRegistry".getExt(),
            "MOCK_SERVER_VERSION" to "mockServerVersion".getExt(),
        )
    )
}

ocTemplate {
    workDir.set(layout.buildDirectory.dir("okd"))
    clusterDomain.set("okdClusterDomain".getExt())
    namespace.set("okdProject".getExt())
    prefix.set("a-automation")
    projectVersion.set(defaultVersion)

    "okdWebConsoleUrl".getExt().takeIf { it.isNotBlank() }?.let{
        webConsoleUrl.set(it)
    }

    service("mockserver") {
        templateFile.set(rootProject.layout.projectDirectory.file("okd/mockserver.yaml"))
        parameters.set(mapOf(
            "DOCKER_REGISTRY" to "dockerRegistry".getExt(),
            "ACTIVE_DEADLINE_SECONDS" to "okdActiveDeadlineSeconds".getExt(),
            "MOCK_SERVER_VERSION" to "mockServerVersion".getExt()
        ))
    }
}

tasks {
    val configureMockServer by registering(ConfigureMockServer::class)
}

when ("testPlatform".getExt()) {
    "okd" -> {
        tasks.named<ConfigureMockServer>("configureMockServer") {
            host.set(ocTemplate.getOkdHost("mockserver"))
            port.set(80)
            dependsOn("ocCreate")
        }
        tasks.withType<Test> {
            systemProperties["test.mockserver-host"] = ocTemplate.getOkdHost("mockserver")
            systemProperties["test.mockserver-port"] = 80
            dependsOn("configureMockServer", "shadowJar")
            useJUnitPlatform()
            testLogging {
                info.events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
            }
            systemProperties["jar"] = configurations["shadow"].artifacts.files.asPath
            finalizedBy( "ocLogs", "ocDelete")
        }
    }
    "docker" -> {
        tasks.named<ConfigureMockServer>("configureMockServer") {
            host.set("localhost")
            port.set(1080)
            dependsOn("composeUp")
        }
        tasks.withType<Test> {
            dependsOn("configureMockServer", "shadowJar")
            systemProperties["test.mockserver-host"] = "localhost"
            systemProperties["test.mockserver-port"] = "1080"
            useJUnitPlatform()
            testLogging {
                info.events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
            }
            systemProperties["jar"] = configurations["shadow"].artifacts.files.asPath
            finalizedBy("composeLogs", "composeDown")
        }
    }
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.3.14")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("org.octopusden.octopus.octopus-external-systems-clients:artifactory-client:${properties["artifactory-client.version"]}")

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
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
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
    isRequired = "signingRequired".getExt().toBooleanStrict()
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}

tasks.distZip.get().isEnabled = false
tasks.shadowDistZip.get().isEnabled = false
tasks.distTar.get().isEnabled = false
tasks.shadowDistTar.get().isEnabled = false