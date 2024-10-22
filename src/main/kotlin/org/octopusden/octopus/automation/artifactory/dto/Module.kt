package org.octopusden.octopus.automation.artifactory.dto

@Suppress("unused")
class Module(val id: String, val type: String?, val artifacts: Collection<Artifact>)