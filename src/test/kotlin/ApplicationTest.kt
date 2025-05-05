import java.io.File
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.octopusden.octopus.automation.artifactory.ArtifactoryCommand
import org.octopusden.octopus.automation.artifactory.ArtifactoryPromoteBuild
import org.octopusden.octopus.automation.artifactory.ArtifactoryPromoteDockerImages


class ApplicationTest {
    private val jar = System.getProperty("jar") ?: throw IllegalStateException("System property 'jar' must be provided")

    /**
     * Test with basic authentication
     */
    @ParameterizedTest
    @MethodSource("commands")
    fun parametrizedTest(name: String, expectedExitCode: Int, command: Array<String>) {
        Assertions.assertEquals(expectedExitCode, execute(name, *ARTIFACTORY_OPTIONS, *command))
    }

    /**
     * Test with token authentication
     */
    @ParameterizedTest
    @MethodSource("commands")
    fun parametrizedTestWithToken(name: String, expectedExitCode: Int, command: Array<String>) {
        Assertions.assertEquals(expectedExitCode, execute(name, *ARTIFACTORY_OPTIONS_WITH_TOKEN, *command))
    }

    private fun execute(name: String, vararg command: String) =
        ProcessBuilder("java", "-jar", jar, *command)
            .redirectErrorStream(true)
            .redirectOutput(
                File("").resolve("build").resolve("logs").resolve("$name.log").also { it.parentFile.mkdirs() })
            .start()
            .waitFor()

    companion object {
        const val HELP_OPTION = "-h"

        const val ARTIFACTORY_URL = "http://localhost:1080"
        const val ARTIFACTORY_USER = "admin"
        const val ARTIFACTORY_PASSWORD = "password"
        const val ARTIFACTORY_TOKEN = "token"

        val ARTIFACTORY_OPTIONS = arrayOf(
            "${ArtifactoryCommand.TOKEN_OPTION}=",
            "${ArtifactoryCommand.URL_OPTION}=$ARTIFACTORY_URL",
            "${ArtifactoryCommand.USER_OPTION}=$ARTIFACTORY_USER",
            "${ArtifactoryCommand.PASSWORD_OPTION}=$ARTIFACTORY_PASSWORD"
        )

        val ARTIFACTORY_OPTIONS_WITH_TOKEN = arrayOf(
            "${ArtifactoryCommand.TOKEN_OPTION}=$ARTIFACTORY_TOKEN",
            "${ArtifactoryCommand.URL_OPTION}=$ARTIFACTORY_URL",
            "${ArtifactoryCommand.USER_OPTION}=$ARTIFACTORY_USER"
        )

        //<editor-fold defaultstate="collapsed" desc="Test Data">
        @JvmStatic
        private fun commands(): Stream<Arguments> = Stream.of(

            Arguments.of(
                "Test help", 0, arrayOf(HELP_OPTION)
            ),
            Arguments.of(
                "Test promote-build help", 0, arrayOf(ArtifactoryPromoteBuild.COMMAND, HELP_OPTION)
            ),
            Arguments.of(
                "Test promote-build", 0, arrayOf(
                    ArtifactoryPromoteBuild.COMMAND,
                    "${ArtifactoryPromoteBuild.BUILD_NAME}=existed-build-name",
                    "${ArtifactoryPromoteBuild.BUILD_NUMBER}=existed-build-number1",
                    "${ArtifactoryPromoteBuild.TARGET_REPOSITORY}=release",
                    "${ArtifactoryPromoteBuild.TARGET_STATUS}=release",
                    "${ArtifactoryPromoteBuild.IGNORE_NOT_FOUND}=false",
                )
            ),
            Arguments.of(
                "Test promote-build with not existed build", 1, arrayOf(
                    ArtifactoryPromoteBuild.COMMAND,
                    "${ArtifactoryPromoteBuild.BUILD_NAME}=not-existed-build-name",
                    "${ArtifactoryPromoteBuild.BUILD_NUMBER}=not-existed-build-version",
                    "${ArtifactoryPromoteBuild.TARGET_REPOSITORY}=release",
                    "${ArtifactoryPromoteBuild.TARGET_STATUS}=release",
                    "${ArtifactoryPromoteBuild.IGNORE_NOT_FOUND}=false",
                )
            ),
            Arguments.of(
                "Test promote-build with not existed build with ignore-not-found", 0, arrayOf(
                    ArtifactoryPromoteBuild.COMMAND,
                    "${ArtifactoryPromoteBuild.BUILD_NAME}=not-existed-build-name",
                    "${ArtifactoryPromoteBuild.BUILD_NUMBER}=not-existed-build-version",
                    "${ArtifactoryPromoteBuild.TARGET_REPOSITORY}=release",
                    "${ArtifactoryPromoteBuild.TARGET_STATUS}=release",
                    "${ArtifactoryPromoteBuild.IGNORE_NOT_FOUND}=true"
                )
            ),
            Arguments.of(
                "Test promote-docker-images help", 0, arrayOf(ArtifactoryPromoteDockerImages.COMMAND, HELP_OPTION)
            ),
            Arguments.of(
                "Test promote-docker-images", 0, arrayOf(
                    ArtifactoryPromoteDockerImages.COMMAND,
                    "${ArtifactoryPromoteDockerImages.SOURCE_REPOSITORY}=docker-dev-repository",
                    "${ArtifactoryPromoteDockerImages.TARGET_REPOSITORY}=docker-release-repository",
                    "${ArtifactoryPromoteDockerImages.IMAGES}=existed-image:existed-tag"
                )
            ),
            Arguments.of(
                "Test promote-docker-images from RC to Release", 0, arrayOf(
                    ArtifactoryPromoteDockerImages.COMMAND,
                    "${ArtifactoryPromoteDockerImages.SOURCE_REPOSITORY}=docker-dev-repository,docker-rc-repository",
                    "${ArtifactoryPromoteDockerImages.TARGET_REPOSITORY}=docker-release-repository",
                    "${ArtifactoryPromoteDockerImages.IMAGES}=existed-image-rc:existed-tag"
                )
            ),
            Arguments.of(
                "Test promote-docker-images with not existed source docker repository", 1, arrayOf(
                    ArtifactoryPromoteDockerImages.COMMAND,
                    "${ArtifactoryPromoteDockerImages.SOURCE_REPOSITORY}=not-existed-docker-dev-repository",
                    "${ArtifactoryPromoteDockerImages.TARGET_REPOSITORY}=docker-release-repository",
                    "${ArtifactoryPromoteDockerImages.IMAGES}=existed-image:existed-tag"
                )
            ),
            Arguments.of(
                "Test promote-docker-images with not existed target docker repository", 1, arrayOf(
                    ArtifactoryPromoteDockerImages.COMMAND,
                    "${ArtifactoryPromoteDockerImages.SOURCE_REPOSITORY}=docker-dev-repository",
                    "${ArtifactoryPromoteDockerImages.TARGET_REPOSITORY}=not-existed-docker-release-repository",
                    "${ArtifactoryPromoteDockerImages.IMAGES}=existed-image:existed-tag"
                )
            ),
            Arguments.of(
                "Test promote-docker-images with not existed docker image", 1, arrayOf(
                    ArtifactoryPromoteDockerImages.COMMAND,
                    "${ArtifactoryPromoteDockerImages.SOURCE_REPOSITORY}=docker-dev-repository",
                    "${ArtifactoryPromoteDockerImages.TARGET_REPOSITORY}=docker-release-repository",
                    "${ArtifactoryPromoteDockerImages.IMAGES}=not-existed-image:existed-tag"
                )
            ),
            Arguments.of(
                "Test promote-docker-images with not existed docker image with ignore-not-found", 0, arrayOf(
                    ArtifactoryPromoteDockerImages.COMMAND,
                    "${ArtifactoryPromoteDockerImages.SOURCE_REPOSITORY}=docker-dev-repository",
                    "${ArtifactoryPromoteDockerImages.TARGET_REPOSITORY}=docker-release-repository",
                    "${ArtifactoryPromoteDockerImages.IMAGES}=not-existed-image:existed-tag,existed-image:existed-tag",
                    "${ArtifactoryPromoteDockerImages.IGNORE_NOT_FOUND}=true"
                )
            ),
            Arguments.of(
                "Test promote-docker-images with invalid format image coordinates", 0, arrayOf(
                    ArtifactoryPromoteDockerImages.COMMAND,
                    "${ArtifactoryPromoteDockerImages.SOURCE_REPOSITORY}=docker-dev-repository",
                    "${ArtifactoryPromoteDockerImages.TARGET_REPOSITORY}=docker-release-repository",
                    "${ArtifactoryPromoteDockerImages.IMAGES}=invalid-format-image-coordinates,existed-image:existed-tag"
                )
            )
        )
        //</editor-fold>
    }
}