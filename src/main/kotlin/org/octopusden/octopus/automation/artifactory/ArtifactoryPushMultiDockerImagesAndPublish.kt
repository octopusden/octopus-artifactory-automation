package org.octopusden.octopus.automation.artifactory

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.octopusden.octopus.automation.artifactory.utils.ContainerEngineNormalizer
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

class ArtifactoryPushMultiDockerImagesAndPublish : CliktCommand(name = COMMAND) {
    private val context by requireObject<MutableMap<String, Any>>()

    private val dockerRegistry by option(DOCKER_REGISTRY, help = "Docker registry URL")
        .convert { it.trim() }.required()
        .check("$DOCKER_REGISTRY is empty") { it.isNotEmpty() }

    private val dockerImages by option(DOCKER_IMAGES, help = "Docker images to push (separated by comma/semicolon)")
        .convert { imagesValue -> imagesValue.split(SPLIT_SYMBOLS.toRegex()).map { it.trim() }.filter { it.isNotEmpty() } }
        .required()
        .check("$DOCKER_IMAGES is empty") { it.isNotEmpty() }

    private val dockerRepository by option(DOCKER_REPOSITORY, help = "Artifactory Docker repository")
        .convert { it.trim() }.required()
        .check("$DOCKER_REPOSITORY is empty") { it.isNotEmpty() }

    private val buildName by option(BUILD_NAME, help = "Artifactory build name")
        .convert { it.trim() }.required()
        .check("$BUILD_NAME is empty") { it.isNotEmpty() }

    private val buildNumber by option(BUILD_NUMBER, help = "Artifactory build number/version")
        .convert { it.trim() }.required()
        .check("$BUILD_NUMBER is empty") { it.isNotEmpty() }

    private val containerEngine by option(CONTAINER_ENGINE, help = "Container engine to use (docker/podman, or comma-separated list - prefers podman)")
        .convert { ContainerEngineNormalizer.normalize(it) }
        .required()

    private val log by lazy { context[ArtifactoryCommand.LOG] as Logger }

    override fun run() {
        log.info("Using container engine: $containerEngine")
        log.info("Pushing ${dockerImages.size} docker image(s) to repository '$dockerRepository'")
        for (dockerImage in dockerImages) {
            pushDockerImage(dockerImage)
        }
        publishBuildInfo()
    }

    private fun pushDockerImage(dockerImage: String) {
        executeCommand(
            listOf("jfrog", "rt", "$containerEngine-push", "$dockerRegistry/$dockerImage", dockerRepository, "--build-name=$buildName", "--build-number=$buildNumber"),
            "Push docker image '$dockerImage'"
        )
    }

    private fun publishBuildInfo() {
        executeCommand(
            listOf("jfrog", "rt", "bp", buildName, buildNumber),
            "Publish build info for '$buildName:$buildNumber'"
        )
    }

    private fun executeCommand(command: List<String>, description: String) {
        log.info("$description: $command")

        val process = ProcessBuilder(command)
            .inheritIO()
            .start()

        val success = process.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)

        if (!success) {
            process.destroyForcibly()
            throw RuntimeException("$description timed out after $DEFAULT_TIMEOUT_MINUTES minutes")
        }

        if (process.exitValue() != 0) {
            throw RuntimeException("$description failed with exit code ${process.exitValue()}")
        }

        log.info("$description completed successfully")
    }

    companion object {
        const val COMMAND = "push-multi-docker-images"
        const val DOCKER_REGISTRY = "--docker-registry"
        const val DOCKER_IMAGES = "--docker-images"
        const val DOCKER_REPOSITORY = "--docker-repository"
        const val BUILD_NAME = "--build-name"
        const val BUILD_NUMBER = "--build-number"
        const val CONTAINER_ENGINE = "--container-engine"
        const val DEFAULT_TIMEOUT_MINUTES = 10L
    }
}
