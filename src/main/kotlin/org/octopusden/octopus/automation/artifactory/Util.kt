package org.octopusden.octopus.automation.artifactory

import org.octopusden.octopus.infrastructure.artifactory.client.exception.NotFoundException

class Util private constructor() {
    companion object {
        fun <T> handleNotFoundException(ignore: Boolean, function: () -> T): T? {
            return try {
                function()
            } catch (e: NotFoundException) {
                if (!ignore) {
                    throw e
                }
                null
            }
        }
    }
}