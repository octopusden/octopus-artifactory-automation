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

    private val sourceRepository by option(
        SOURCE_REPOSITORY,
        help = "Sources Artifactory docker repository key (separated by comma/semicolon)"
    )
        .convert { sources ->
            sources.split(SPLIT_SYMBOLS.toRegex()).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }.required().check("$SOURCE_REPOSITORY is empty") { it.isNotEmpty() }

    private val targetRepository by option(TARGET_REPOSITORY, help = "Target Artifactory docker repository key")
        .convert { it.trim() }.required()
        .check("$TARGET_REPOSITORY is empty") { it.isNotEmpty() }

    private val images by option(
        IMAGES, help = "Docker images coordinates in PATH:TAG format (separated by comma/semicolon)"
    ).convert { imagesValue ->
        imagesValue.split(SPLIT_SYMBOLS.toRegex()).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }.required()

    private val ignoreNotFound by option(IGNORE_NOT_FOUND, help = "Ignore errors when image is not found")
        .convert { it.trim().toBoolean() }.default(false)

    private val client by lazy { context[ArtifactoryCommand.CLIENT] as ArtifactoryClient }
    private val log by lazy { context[ArtifactoryCommand.LOG] as Logger }

    override fun run() {
        images.forEach { promoteDockerImage(it) }
    }

    private fun promoteDockerImage(image: String) {
        val coordinates = image.split(':').filter { it.isNotEmpty() }
        if (coordinates.size != 2) {
            log.warn("Image coordinates '$image' has invalid format. Skip promotion")
            return
        }
        sourceRepository.firstOrNull { source ->
            try {
                log.info("Promote docker image '$image' from '$source' to '$targetRepository' repository")
                client.promoteDockerImage(
                    source,
                    PromoteDockerImageRequest(coordinates[0], coordinates[1], targetRepository)
                )
                return
            } catch (_: NotFoundException) { false }
        }
        if (!ignoreNotFound) {
            throw NotFoundException("Docker image '$image' not found in '$sourceRepository'")
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