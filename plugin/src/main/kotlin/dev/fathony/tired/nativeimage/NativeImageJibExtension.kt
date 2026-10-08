package dev.fathony.tired.nativeimage

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.gradle.extension.GradleData
import com.google.cloud.tools.jib.gradle.extension.JibGradlePluginExtension
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.GradleException
import java.util.Optional

/**
 * Makes Jib's image hold the native executable in place of the classes, the libraries and the `java` command.
 */
class NativeImageJibExtension : JibGradlePluginExtension<Void> {
    override fun getExtraConfigType(): Optional<Class<Void>> = Optional.empty()

    override fun extendContainerBuildPlan(
        buildPlan: ContainerBuildPlan,
        properties: Map<String, String>,
        extraConfig: Optional<Void>,
        gradleData: GradleData,
        logger: ExtensionLogger,
    ): ContainerBuildPlan {
        if (!System.getProperty("os.name").startsWith("Linux")) {
            throw GradleException(
                "A native executable only runs on the system it was built on, so the image has to be built on Linux, " +
                    "for example in CI. This is ${System.getProperty("os.name")}.",
            )
        }
        val executable =
            gradleData.project.tasks
                .named("nativeCompile", BuildNativeImageTask::class.java)
                .flatMap { it.outputFile }
                .get()
                .asFile
        val inImage = AbsoluteUnixPath.get("/app/${executable.name}")
        val layer =
            FileEntriesLayer
                .builder()
                .setName("native image")
                .addEntry(executable.toPath(), inImage, FilePermissions.fromOctalString("755"))
                .build()
        return buildPlan
            .toBuilder()
            .setLayers(listOf(layer))
            .setEntrypoint(listOf(inImage.toString()))
            .setCmd(null)
            .setEnvironment(buildPlan.environment - "JAVA_TOOL_OPTIONS")
            .build()
    }
}
