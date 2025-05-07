package org.octopusden.octopus.task

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.octopusden.octopus.infrastructure.artifactory.client.dto.ArtifactoryResponse
import org.octopusden.octopus.infrastructure.artifactory.client.dto.PromoteDockerImageRequest


abstract class ConfigureMockServer : DefaultTask() {
    private val mockServerClient = MockServerClient("localhost", 1080)

    @TaskAction
    fun configureMockServer() {
        mockServerClient.reset()

        mockServerClient.`when`(
            HttpRequest.request().withMethod("GET")
                .withPath("/access/api/v1/tokens")
        ).respond {
            val tokens = load("tokens.json", object : TypeReference<Any>() {})
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(200)
                .withBody(mapper.writeValueAsString(tokens))
        }

        mockServerClient.`when`(
            HttpRequest.request().withMethod("GET")
                .withPath("/artifactory/api/system/version")
        ).respond {
            val version = load("version.json", object : TypeReference<Any>() {})
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(200)
                .withBody(mapper.writeValueAsString(version))
        }

        val builds = load("builds.json", object : TypeReference<Map<String, Map<String, Any>>>() {})

        mockServerClient.`when`(
            HttpRequest.request().withMethod("GET")
                .withPath("/artifactory/api/build/{buildName}/{buildNumber}")
                .withPathParameter("buildName")
                .withPathParameter("buildNumber")
        ).respond {
            val buildName = it.getFirstPathParameter("buildName")
            val buildNumber = it.getFirstPathParameter("buildNumber")

            builds[buildName]?.get(buildNumber)?.let { buildInfo ->
                HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(200)
                    .withBody(mapper.writeValueAsString(buildInfo))
            } ?: run {
                val error = load("build-not-found-error.json", object : TypeReference<Any>() {})
                HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(404)
                    .withBody(mapper.writeValueAsString(error))
            }
        }

        mockServerClient.`when`(
            HttpRequest.request().withMethod("POST")
                .withPath("/artifactory/api/build/promote/{buildName}/{buildNumber}")
                .withPathParameter("buildName")
                .withPathParameter("buildNumber")
        ).respond {
            val buildName = it.getFirstPathParameter("buildName")
            val buildNumber = it.getFirstPathParameter("buildNumber")

            builds[buildName]?.get(buildNumber)?.let { buildInfo ->
                HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(200)
                    .withBody(mapper.writeValueAsString(ArtifactoryResponse(emptyList())))
            } ?: run {
                val error = load("build-not-found-error.json", object : TypeReference<Any>() {})
                HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(404)
                    .withBody(mapper.writeValueAsString(error))
            }
        }

        val dockerRepositories =
            load("docker-repositories.json", object : TypeReference<Map<String, Map<String, Map<String, String?>>>>() {})

        mockServerClient.`when`(
            HttpRequest.request().withMethod("POST")
                .withPath("/artifactory/api/docker/{repoKey}/v2/promote")
                .withPathParameter("repoKey")
        ).respond {
            try {
                val repoKey = it.getFirstPathParameter("repoKey")
                val promoteDockerImageRequest =
                    mapper.readValue(it.body.rawBytes, PromoteDockerImageRequest::class.java)
                val images =
                    dockerRepositories[repoKey] ?: throw MockException(400, "docker-repository-not-found-error.json")
                val tags = images[promoteDockerImageRequest.dockerRepository] ?: throw MockException(404, "docker-image-not-found-error.json")
                val targetRepository = tags[promoteDockerImageRequest.tag] ?: throw MockException(404, "docker-image-not-found-error.json")
                if (targetRepository != promoteDockerImageRequest.targetRepo) {
                    throw MockException(400, "docker-repository-not-found-error.json")
                }
                HttpResponse.response().withStatusCode(200)
            } catch (e: MockException) {
                val error = load(e.responseFileName, object : TypeReference<Any>() {})
                HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(e.code)
                    .withBody(mapper.writeValueAsString(error))
            }
        }
    }

    private class MockException(val code: Int, val responseFileName: String) : RuntimeException()

    private fun <T> load(filename: String, typeReference: TypeReference<T>): T {
        val buildPath = project.rootDir.resolve("src").resolve("test").resolve("resources").resolve("mockserver")
            .resolve(filename)
        return mapper.readValue(buildPath, typeReference)
    }

    companion object {
        private val mapper = with(ObjectMapper()) {
            registerModules(KotlinModule.Builder().build())
            this
        }
    }
}
