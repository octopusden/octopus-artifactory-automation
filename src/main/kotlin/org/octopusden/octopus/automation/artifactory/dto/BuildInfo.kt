package org.octopusden.octopus.automation.artifactory.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class BuildInfo @JsonCreator constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("number") val number: String,
    @JsonProperty("modules") val modules: Collection<Module>?,
    @JsonProperty("statuses") val statuses: Collection<Status>?
) {
    override fun toString(): String {
        return "$name:$number"
    }
}
