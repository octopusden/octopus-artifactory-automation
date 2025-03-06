package org.octopusden.octopus.automation.artifactory

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import org.octopusden.octopus.infrastructure.artifactory.client.ArtifactoryClassicClient
import org.octopusden.octopus.infrastructure.artifactory.client.ArtifactoryClient
import org.octopusden.octopus.infrastructure.client.commons.ClientParametersProvider
import org.octopusden.octopus.infrastructure.client.commons.CredentialProvider
import org.octopusden.octopus.infrastructure.client.commons.StandardBasicCredCredentialProvider
import org.octopusden.octopus.infrastructure.client.commons.StandardBearerTokenCredentialProvider
import org.slf4j.LoggerFactory

class ArtifactoryCommand : CliktCommand(name = "") {
    private val url by option(URL_OPTION, help = "Artifactory URL").convert { it.trim() }.required()
        .check("$URL_OPTION is empty") { it.isNotEmpty() }
    private val user by option(USER_OPTION, help = "Artifactory user").convert { it.trim() }
        .check("$USER_OPTION is empty") { it.isNotEmpty() }
    private val password by option(PASSWORD_OPTION, help = "Artifactory password").convert { it.trim() }.validate {
        require(it.isNotBlank() && user?.isNotBlank() == true || user?.isBlank() == true) {
            "Password must not be blank with basic authentication"
        }
    }
    private val token by option(TOKEN_OPTION, help = "Artifactory token").convert { it.trim() }.validate {
        require(user?.isBlank() == false && password?.isBlank() == false || it.isNotBlank()) {
            "Either $TOKEN_OPTION or $USER_OPTION/$PASSWORD_OPTION must be set"
        }
    }

    private val context by findOrSetObject { mutableMapOf<String, Any>() }

    override fun run() {
        val log = LoggerFactory.getLogger(ArtifactoryCommand::class.java.`package`.name)
        val client: ArtifactoryClient = ArtifactoryClassicClient(object : ClientParametersProvider {
            override fun getApiUrl() = url
            override fun getAuth(): CredentialProvider =
                token?.let { t -> StandardBearerTokenCredentialProvider(t) }
                    ?: user?.let { u -> password?.let { p -> StandardBasicCredCredentialProvider(u, p) } }
                    ?: throw IllegalArgumentException("Artifactory credentials not found")
        })

        val resultUser = token?.let { client.getTokens().tokens.map { t -> t.subject.substringAfterLast("/") } }
            ?.firstOrNull { it.isNotBlank() }
            ?: user
            ?: throw IllegalStateException("Artifactory user not found")

        val version = client.getVersion()
        log.info("Artifactory server: ${version.license}:${version.version}")
        context[LOG] = log
        context[CLIENT] = client
        context[USERNAME] = resultUser
    }

    companion object {
        const val URL_OPTION = "--url"
        const val USER_OPTION = "--user"
        const val PASSWORD_OPTION = "--password"
        const val TOKEN_OPTION = "--token"
        const val LOG = "log"
        const val CLIENT = "client"
        const val USERNAME = "username"
        private const val AUTH_NO_CREDENTIAL_ERROR_MESSAGE = "Either username/password or token must be set"
    }
}