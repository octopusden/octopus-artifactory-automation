package org.octopusden.octopus.automation.artifactory.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("unused")
class Module @JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("type") val type: String?,
    @JsonProperty("artifacts") val artifacts: Collection<Artifact>
)