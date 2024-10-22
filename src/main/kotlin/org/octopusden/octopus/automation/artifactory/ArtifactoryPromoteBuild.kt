package org.octopusden.octopus.automation.artifactory

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.apache.http.HttpStatus
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.ArtifactoryResponse
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.octopusden.octopus.automation.artifactory.dto.BuildInfo
import org.octopusden.octopus.automation.artifactory.dto.BuildInfoResponse
import org.octopusden.octopus.automation.artifactory.dto.Promote
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

    private val client by lazy { context[ArtifactoryCommand.CLIENT] as Artifactory }
    private val log by lazy { context[ArtifactoryCommand.LOG] as Logger }

    override fun run() {
        log.info("Promote Artifactory build: '$buildName:$buildNumber', to repository: '$targetRepository', target status: '$targetStatus'")
        promoteBuild()
    }

    private fun promoteBuild() {
        getBuildInfo(buildName, buildNumber)?.let { buildInfo ->
            promote(buildInfo, targetRepository, targetStatus, force)
        }
    }

    private fun getBuildInfo(buildName: String, buildNumber: String): BuildInfo? {
        val buildRequest = ArtifactoryRequestImpl()
            .apiUrl("api/build/$buildName/$buildNumber")
            .method(ArtifactoryRequest.Method.GET)
            .responseType(ArtifactoryRequest.ContentType.JSON)

        val successCodes = with(intArrayOf(HttpStatus.SC_OK)) {
            if (ignoreNotFound) {
                plus(HttpStatus.SC_NOT_FOUND)
            } else {
                this
            }
        }

        return client.restCall(buildRequest).check(*successCodes)
            .takeIf { response -> response.statusLine.statusCode == HttpStatus.SC_OK }
            ?.parseBody(BuildInfoResponse::class.java)?.buildInfo
            ?.also { buildInfo ->
                if (buildInfo.modules.isNullOrEmpty()) {
                    log.warn("The build $buildName/$buildNumber found but has no modules. Check creating and publishing build artifacts.")
                }
            }
    }

    private fun promote(buildInfo: BuildInfo, targetRepository: String, targetStatus: String, forcePromote: Boolean) {
        if (forcePromote || (buildInfo.statuses?.find { it.status == targetStatus } == null)) {
            val promote = Promote(client.username, targetRepository, targetStatus)
            val promoteRequest: ArtifactoryRequest = ArtifactoryRequestImpl()
                .apiUrl("api/build/promote/${buildInfo.name}/${buildInfo.number}")
                .method(ArtifactoryRequest.Method.POST)
                .requestType(ArtifactoryRequest.ContentType.JSON)
                .requestBody(promote)
            client.restCall(promoteRequest).check(HttpStatus.SC_OK)
        } else {
            log.info("Build $buildInfo already promoted to $targetStatus")
        }
    }

    private fun ArtifactoryResponse.check(vararg successCodes: Int): ArtifactoryResponse =
        takeIf { statusLine.statusCode in successCodes } ?: run {
            log.error("Status code: {}, body: {}", statusLine.statusCode, rawBody)
            throw RuntimeException("Fail to promote build")
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
