package org.octopusden.octopus.automation.artifactory.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("unused")
class Artifact @JsonCreator constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("type") val type: String?
)