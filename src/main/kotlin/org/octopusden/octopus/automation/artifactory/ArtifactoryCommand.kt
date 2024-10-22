package org.octopusden.octopus.automation.artifactory

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.slf4j.LoggerFactory

class ArtifactoryCommand : CliktCommand(name = "") {
    private val url by option(URL_OPTION, help = "Artifactory URL").convert { it.trim() }.required()
        .check("$URL_OPTION is empty") { it.isNotEmpty() }
    private val user by option(USER_OPTION, help = "Artifactory user").convert { it.trim() }.required()
        .check("$USER_OPTION is empty") { it.isNotEmpty() }
    private val password by option(PASSWORD_OPTION, help = "Artifactory password").convert { it.trim() }.required()
        .check("$PASSWORD_OPTION is empty") { it.isNotEmpty() }
    private val insecure by option(INSECURE_OPTION, help = "Ignore SSL issues").convert { it.trim().toBoolean() }
        .default(false)

    private val context by findOrSetObject { mutableMapOf<String, Any>() }

    override fun run() {
        val log = LoggerFactory.getLogger(ArtifactoryCommand::class.java.`package`.name)
        val client: Artifactory = ArtifactoryClientBuilder.create()
            .setUrl("$url/artifactory")
            .setIgnoreSSLIssues(insecure)
            .setUsername(user)
            .setPassword(password)
            .build()
        val version = client.system().version()
        log.info("Artifactory server: ${version.license}:${version.version}")
        context[LOG] = log
        context[CLIENT] = client
    }

    companion object {
        const val URL_OPTION = "--url"
        const val USER_OPTION = "--user"
        const val PASSWORD_OPTION = "--password"
        const val INSECURE_OPTION = "--insecure"
        const val LOG = "log"
        const val CLIENT = "client"
    }
}