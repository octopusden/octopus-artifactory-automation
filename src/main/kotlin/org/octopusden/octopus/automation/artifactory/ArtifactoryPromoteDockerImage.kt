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
import org.slf4j.Logger

class ArtifactoryPromoteDockerImage : CliktCommand(name = COMMAND) {
    private val context by requireObject<MutableMap<String, Any>>()

    private val sourceRepository by option(SOURCE_REPOSITORY, help = "Source Artifactory docker repository key")
        .convert { it.trim() }.required()
        .check("$SOURCE_REPOSITORY is empty") { it.isNotEmpty() }

    private val targetRepository by option(TARGET_REPOSITORY, help = "Target Artifactory docker repository key")
        .convert { it.trim() }.required()
        .check("$TARGET_REPOSITORY is empty") { it.isNotEmpty() }

    private val image by option(IMAGE, help = "Docker image")
        .convert { it.trim() }.required()
        .check("$IMAGE is empty") { it.isNotEmpty() }

    private val tag by option(TAG, help = "Artifactory build version")
        .convert { it.trim() }.required()
        .check("$TAG is empty") { it.isNotEmpty() }

    private val ignoreNotFound by option(IGNORE_NOT_FOUND, help = "Ignore errors when build is not found")
        .convert { it.trim().toBoolean() }.default(false)

    private val client by lazy { context[ArtifactoryCommand.CLIENT] as ArtifactoryClient }
    private val log by lazy { context[ArtifactoryCommand.LOG] as Logger }

    override fun run() {
        log.info("Promote Docker image: '$image:$tag' to repository: '$targetRepository'")
        promoteDockerImage()
    }

    private fun promoteDockerImage() {
        Util.handleNotFoundException(ignoreNotFound) {
            client.promoteDockerImage(
                sourceRepository,
                PromoteDockerImageRequest(image, tag, targetRepository)
            )
        }
    }

    companion object {
        const val COMMAND = "promote-docker-image"
        const val SOURCE_REPOSITORY = "--source-repository"
        const val TARGET_REPOSITORY = "--target-repository"
        const val IMAGE = "--image"
        const val TAG = "--tag"
        const val IGNORE_NOT_FOUND = "--ignore-not-found"
    }
}