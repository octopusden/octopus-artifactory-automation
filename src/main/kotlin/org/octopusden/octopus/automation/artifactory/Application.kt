package org.octopusden.octopus.automation.artifactory

import com.github.ajalt.clikt.core.subcommands

const val SPLIT_SYMBOLS = "[,;]"

fun main(args: Array<String>) {
    ArtifactoryCommand().subcommands(
        ArtifactoryPromoteBuild(),
        ArtifactoryPromoteDockerImages()
    ).main(args)
}