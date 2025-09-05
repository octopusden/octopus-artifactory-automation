package org.octopusden.octopus.automation.artifactory

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.octopusden.octopus.infrastructure.artifactory.client.ArtifactoryClient
import org.octopusden.octopus.infrastructure.artifactory.client.dto.PromoteBuildRequest
import org.octopusden.octopus.infrastructure.artifactory.client.exception.NotFoundException
import org.slf4j.Logger

class ArtifactoryPromoteBuild : CliktCommand(name = COMMAND) {
    private val context by requireObject<MutableMap<String, Any>>()

    private val buildName by option(BUILD_NAME, help = "Artifactory build name")
        .convert { it.trim() }.required()
        .check("$BUILD_NAME is empty") { it.isNotEmpty() }

    private val buildNumber by option(BUILD_NUMBER, help = "Artifactory build version")
        .convert { it.trim() }.required()
        .check("$BUILD_NUMBER is empty") { it.isNotEmpty() }

    private val targetRepository by option(TARGET_REPOSITORY, help = "Target Artifactory repository")
        .convert { it.trim() }.required()
        .check("$TARGET_REPOSITORY is empty") { it.isNotEmpty() }

    private val targetStatus by option(TARGET_STATUS, help = "Target promotion status (e.g. 'release')")
        .convert { it.trim() }.required()
        .check("$TARGET_STATUS is empty") { it.isNotEmpty() }

    private val ignoreNotFound by option(IGNORE_NOT_FOUND, help = "Ignore errors when build is not found")
        .convert { it.trim().toBoolean() }.default(true)

    private val force by option(FORCE, help = "Force promotion").convert { it.trim().toBoolean() }
        .default(false)

    private val client by lazy { context[ArtifactoryCommand.CLIENT] as ArtifactoryClient }
    private val log by lazy { context[ArtifactoryCommand.LOG] as Logger }
    private val username by lazy { context[ArtifactoryCommand.USERNAME] as String }

    override fun run() {
        val build = "$buildName:$buildNumber"
        log.info("Promote Artifactory build '$build' to repository '$targetRepository' with target status '$targetStatus'")
        val buildInfo = try {
            client.getBuildInfo(buildName, buildNumber).buildInfo
        } catch (e: NotFoundException) {
            if (ignoreNotFound) {
                log.info("Artifactory build '$build' is not found")
                return
            } else throw e
        }
        if (buildInfo.modules.isNullOrEmpty()) {
            log.warn("Artifactory build '$build' is empty (has no modules)")
        }
        if (force || (buildInfo.statuses?.find { it.status == targetStatus } == null)) {
            val promote = PromoteBuildRequest(username, targetRepository, targetStatus)
            client.promoteBuild(buildInfo.name, buildInfo.number, promote).also {
                it.messages.joinToString { artifactoryMessage -> artifactoryMessage.message }.let { message ->
                    log.info("Artifactory build '$build' promoted with message: $message")
                }
            }
        } else {
            log.info("Artifactory build '$build' already has target status '$targetStatus'")
        }
    }

    companion object {
        const val COMMAND = "promote-build"
        const val BUILD_NAME = "--build-name"
        const val BUILD_NUMBER = "--build-number"
        const val TARGET_REPOSITORY = "--target-repository"
        const val TARGET_STATUS = "--target-status"
        const val IGNORE_NOT_FOUND = "--ignore-not-found"
        const val FORCE = "--force"
    }
}
