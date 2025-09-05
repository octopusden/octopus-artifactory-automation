package org.octopusden.octopus.automation.artifactory

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.octopusden.octopus.infrastructure.artifactory.client.ArtifactoryClient
import org.octopusden.octopus.infrastructure.artifactory.client.dto.PromoteDockerImageRequest
import org.octopusden.octopus.infrastructure.artifactory.client.exception.NotFoundException
import org.slf4j.Logger

class ArtifactoryPromoteDockerImages : CliktCommand(name = COMMAND) {
    private val context by requireObject<MutableMap<String, Any>>()

    private val sourceRepositories by option(
        SOURCE_REPOSITORY, help = "Source Artifactory repositories (separated by comma/semicolon)"
    ).convert { sources ->
        sources.split(SPLIT_SYMBOLS.toRegex()).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }.required().check("$SOURCE_REPOSITORY is empty") { it.isNotEmpty() }

    private val targetRepository by option(
        TARGET_REPOSITORY, help = "Target Artifactory repository"
    ).convert { it.trim() }.required().check("$TARGET_REPOSITORY is empty") { it.isNotEmpty() }

    private val images by option(
        IMAGES, help = "Docker images coordinates in PATH:TAG format (separated by comma/semicolon)"
    ).convert { imagesValue ->
        imagesValue.split(SPLIT_SYMBOLS.toRegex()).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }.required()

    private val ignoreNotFound by option(
        IGNORE_NOT_FOUND, help = "Ignore errors when docker image is not found"
    ).convert { it.trim().toBoolean() }.default(false)

    private val client by lazy { context[ArtifactoryCommand.CLIENT] as ArtifactoryClient }
    private val log by lazy { context[ArtifactoryCommand.LOG] as Logger }

    override fun run() {
        images.forEach { promoteDockerImage(it) }
    }

    private fun promoteDockerImage(image: String) {
        val coordinates = image.split(':').filter { it.isNotEmpty() }
        if (coordinates.size != 2) {
            log.warn("Docker image coordinates '$image' has invalid format")
            return
        }
        log.info("Promote docker image '$image' to repository '$targetRepository'")
        sourceRepositories.forEach { sourceRepository ->
            try {
                client.promoteDockerImage(
                    sourceRepository,
                    PromoteDockerImageRequest(coordinates[0], coordinates[1], targetRepository)
                )
                log.info("Docker image '$image' promoted from '$sourceRepository' to '$targetRepository'")
                return
            } catch (_: NotFoundException) {
            }
        }
        with("Docker image '$image' is not found in repositories $sourceRepositories") {
            if (ignoreNotFound) log.info(this)
            else throw NotFoundException(this)
        }
    }

    companion object {
        const val COMMAND = "promote-docker-images"
        const val SOURCE_REPOSITORY = "--source-repository"
        const val TARGET_REPOSITORY = "--target-repository"
        const val IMAGES = "--images"
        const val IGNORE_NOT_FOUND = "--ignore-not-found"
    }
}