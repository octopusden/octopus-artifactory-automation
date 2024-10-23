package org.octopusden.octopus.automation.artifactory.dto

import java.util.Date

@Suppress("unused")
class Promote(
    val user: String,
    val targetRepo: String,
    val status: String,
    val timestamp: Date = Date(),
    val dryRun: Boolean = false,
    val copy: Boolean = false,
    val artifacts: Boolean = true,
    val dependencies: Boolean = false,
    val failFast: Boolean = true
)