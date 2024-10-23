package org.octopusden.octopus.automation.artifactory.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class BuildInfoResponse @JsonCreator constructor(@JsonProperty("buildInfo") val buildInfo: BuildInfo)