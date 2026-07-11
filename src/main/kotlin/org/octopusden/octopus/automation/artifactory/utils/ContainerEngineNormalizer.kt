package org.octopusden.octopus.automation.artifactory.utils

object ContainerEngineNormalizer {
    const val DOCKER = "docker"
    const val PODMAN = "podman"
    const val DEFAULT_CONTAINER_ENGINE = PODMAN

    private val VALID_ENGINES = setOf(DOCKER, PODMAN)

    /**
     * Normalizes the container engine parameter value.
     *
     * @param containerEngineParam the raw container engine parameter (may contain multiple comma-separated values)
     * @return the normalized engine name (prefers 'podman' if present, otherwise 'docker')
     * @throws IllegalArgumentException if no valid engine is found
     */
    fun normalize(containerEngineParam: String): String {
        val engines = containerEngineParam
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        val validEngines = engines.filter { it in VALID_ENGINES }

        if (validEngines.isEmpty()) {
            throw IllegalArgumentException(
                "Invalid container.engine value: '$containerEngineParam'. Must contain '$DOCKER' or '$PODMAN'.",
            )
        }

        return if (validEngines.contains(DEFAULT_CONTAINER_ENGINE)) DEFAULT_CONTAINER_ENGINE else validEngines.first()
    }
}
