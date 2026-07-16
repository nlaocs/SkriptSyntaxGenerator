package jp.nlaocs.skriptSyntaxGenerator.util

import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

object FileUtils {
    private const val OUTPUT_DIRECTORY_PROPERTY = "skriptSyntaxGenerator.outputDirectory"

    @JvmStatic
    fun outputDirectory() = System.getProperty(OUTPUT_DIRECTORY_PROPERTY)
        ?.takeIf(String::isNotBlank)
        ?.let(Paths::get)
        ?: Paths.get("plugins", "SkriptSyntaxGenerator")

    @JvmStatic
    fun writeStringToFile(fileName: String, content: String) {
        val dirPath = outputDirectory()
        val filePath = dirPath.resolve(fileName)

        dirPath.createDirectories()
        filePath.writeText(content)
    }
}
