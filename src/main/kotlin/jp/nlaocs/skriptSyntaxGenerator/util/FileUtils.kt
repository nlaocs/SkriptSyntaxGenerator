package jp.nlaocs.skriptSyntaxGenerator.util

import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

object FileUtils {
    @JvmStatic
    fun writeStringToFile(fileName: String, content: String) {
        val dirPath = Paths.get("plugins", "SkriptSyntaxGenerator")
        val filePath = dirPath.resolve(fileName)

        dirPath.createDirectories()
        filePath.writeText(content)
    }
}
