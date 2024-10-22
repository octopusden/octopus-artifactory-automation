package org.octopusden.octopus.automation.artifactory.dto

class BuildInfo(
    val name: String,
    val number: String,
    val modules: Collection<Module>?,
    val statuses: Collection<Status>?,
) {
    override fun toString(): String {
        return "$name:$number"
    }
}