package org.octopusden.octopus.task

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType


abstract class ConfigureMockServer : DefaultTask() {
    private val mockServerClient = MockServerClient("localhost", 1080)

    @TaskAction
    fun configureMockServer() {
        mockServerClient.reset()
        val builds = load("builds.json", object : TypeReference<Map<String, Map<String, Any>>>() {})

        mockServerClient.`when`(
            HttpRequest.request().withMethod("GET")
                .withPath("/artifactory/api/system/version")
        ).respond {
            val version = load("version.json", object : TypeReference<Any>() {})
            HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(200)
                .withBody(mapper.writeValueAsString(version))
        }

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
            } ?: run {
                val error = load("build-not-found-error.json", object : TypeReference<Any>() {})
                HttpResponse.response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withStatusCode(404)
                    .withBody(mapper.writeValueAsString(error))
            }
        }
    }

    private fun <T> load(filename: String, typeReference: TypeReference<T>): T {
        val buildPath = project.rootDir.resolve("src").resolve("test").resolve("resources").resolve("mockserver")
            .resolve(filename)
        return mapper.readValue(buildPath, typeReference)
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}
