import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.octopusden.octopus.automation.artifactory.ArtifactoryCommand
import org.octopusden.octopus.automation.artifactory.ArtifactoryPromoteBuild


class ApplicationTest {
    private val jar = System.getProperty("jar") ?: throw IllegalStateException("System property 'jar' must be provided")

    @Test
    fun testGlobalHelp(testInfo: TestInfo) {
        execute(testInfo.testMethod.get().name, HELP_OPTION)
    }

    @Test
    fun testPromoteBuildHelp(testInfo: TestInfo) {
        execute(
            testInfo.testMethod.get().name, *ARTIFACTORY_OPTIONS,
            ArtifactoryPromoteBuild.COMMAND, HELP_OPTION
        )
    }

    @Test
    fun testPromoteBuild(testInfo: TestInfo) {
        Assertions.assertEquals(
            0, execute(
                testInfo.testMethod.get().name,
                *ARTIFACTORY_OPTIONS,
                ArtifactoryPromoteBuild.COMMAND,
                "${ArtifactoryPromoteBuild.BUILD_NAME}=existed-build-name",
                "${ArtifactoryPromoteBuild.BUILD_NUMBER}=existed-build-number1",
                "${ArtifactoryPromoteBuild.TARGET_REPOSITORY}=release",
                "${ArtifactoryPromoteBuild.TARGET_STATUS}=release",
                "${ArtifactoryPromoteBuild.IGNORE_NOT_FOUND}=false",
            )
        )
    }

    @Test
    fun testPromoteNotExistedBuild(testInfo: TestInfo) {
        Assertions.assertEquals(
            1, execute(
                testInfo.testMethod.get().name,
                *ARTIFACTORY_OPTIONS,
                ArtifactoryPromoteBuild.COMMAND,
                "${ArtifactoryPromoteBuild.BUILD_NAME}=not-existed-build-name",
                "${ArtifactoryPromoteBuild.BUILD_NUMBER}=not-existed-build-version",
                "${ArtifactoryPromoteBuild.TARGET_REPOSITORY}=release",
                "${ArtifactoryPromoteBuild.TARGET_STATUS}=release",
                "${ArtifactoryPromoteBuild.IGNORE_NOT_FOUND}=false",
            )
        )
    }

    @Test
    fun testPromoteNotExistedBuildWithIgnoreNotFound(testInfo: TestInfo) {
        Assertions.assertEquals(
            0, execute(
                testInfo.testMethod.get().name,
                *ARTIFACTORY_OPTIONS,
                ArtifactoryPromoteBuild.COMMAND,
                "${ArtifactoryPromoteBuild.BUILD_NAME}=not-existed-build-name",
                "${ArtifactoryPromoteBuild.BUILD_NUMBER}=not-existed-build-version",
                "${ArtifactoryPromoteBuild.TARGET_REPOSITORY}=release",
                "${ArtifactoryPromoteBuild.TARGET_STATUS}=release",
                "${ArtifactoryPromoteBuild.IGNORE_NOT_FOUND}=true",
            )
        )
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

        val ARTIFACTORY_OPTIONS = arrayOf(
            "${ArtifactoryCommand.URL_OPTION}=$ARTIFACTORY_URL",
            "${ArtifactoryCommand.USER_OPTION}=$ARTIFACTORY_USER",
            "${ArtifactoryCommand.PASSWORD_OPTION}=$ARTIFACTORY_PASSWORD"
        )

        //<editor-fold defaultstate="collapsed" desc="Test Data">

        //</editor-fold>
    }
}