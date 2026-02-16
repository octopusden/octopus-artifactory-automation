package org.octopusden.octopus.automation.artifactory.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

class ContainerEngineNormalizerTest {

    @ParameterizedTest
    @MethodSource("validEngineInputs")
    fun `normalize should return correct engine for valid inputs`(input: String, expected: String) {
        Assertions.assertEquals(expected, ContainerEngineNormalizer.normalize(input))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "invalid", "kubernetes", "containerd", "rkt"])
    fun `normalize should throw exception for invalid engine`(input: String) {
        val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            ContainerEngineNormalizer.normalize(input)
        }
        Assertions.assertTrue(exception.message!!.contains("Invalid container.engine value"))
    }

    companion object {
        @JvmStatic
        private fun validEngineInputs(): Stream<Arguments> = Stream.of(
            // Single valid engine
            Arguments.of("docker", ContainerEngineNormalizer.DOCKER),
            Arguments.of("podman", ContainerEngineNormalizer.PODMAN),

            // Case-insensitive
            Arguments.of("Docker", ContainerEngineNormalizer.DOCKER),
            Arguments.of("DOCKER", ContainerEngineNormalizer.DOCKER),
            Arguments.of("Podman", ContainerEngineNormalizer.PODMAN),
            Arguments.of("PODMAN", ContainerEngineNormalizer.PODMAN),

            // With whitespace
            Arguments.of("  docker  ", ContainerEngineNormalizer.DOCKER),
            Arguments.of("  podman  ", ContainerEngineNormalizer.PODMAN),

            // Multiple values - prefer podman
            Arguments.of("docker,podman", ContainerEngineNormalizer.PODMAN),
            Arguments.of("podman,docker", ContainerEngineNormalizer.PODMAN),
            Arguments.of("docker, podman", ContainerEngineNormalizer.PODMAN),
            Arguments.of("podman, docker", ContainerEngineNormalizer.PODMAN),

            // Multiple values with whitespace
            Arguments.of("  docker  ,  podman  ", ContainerEngineNormalizer.PODMAN),

            // Multiple docker values
            Arguments.of("docker,docker", ContainerEngineNormalizer.DOCKER),

            // Multiple podman values
            Arguments.of("podman,podman", ContainerEngineNormalizer.PODMAN),

            // Valid with invalid mixed in
            Arguments.of("invalid,docker", ContainerEngineNormalizer.DOCKER),
            Arguments.of("docker,invalid", ContainerEngineNormalizer.DOCKER),
            Arguments.of("invalid,podman", ContainerEngineNormalizer.PODMAN),
            Arguments.of("podman,invalid", ContainerEngineNormalizer.PODMAN),
            Arguments.of("invalid,docker,podman", ContainerEngineNormalizer.PODMAN),
            Arguments.of("kubernetes,docker,containerd", ContainerEngineNormalizer.DOCKER)
        )
    }
}
