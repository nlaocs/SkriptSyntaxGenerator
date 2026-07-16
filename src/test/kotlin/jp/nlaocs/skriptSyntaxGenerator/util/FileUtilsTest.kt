package jp.nlaocs.skriptSyntaxGenerator.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class FileUtilsTest {
    @TempDir
    lateinit var tempDirectory: Path

    @AfterEach
    fun clearOutputDirectoryProperty() {
        System.clearProperty(OUTPUT_DIRECTORY_PROPERTY)
    }

    @Test
    fun `uses plugin directory by default`() {
        System.clearProperty(OUTPUT_DIRECTORY_PROPERTY)

        assertEquals(Path.of("plugins", "SkriptSyntaxGenerator"), FileUtils.outputDirectory())
    }

    @Test
    fun `uses configured output directory`() {
        System.setProperty(OUTPUT_DIRECTORY_PROPERTY, tempDirectory.toString())

        assertEquals(tempDirectory, FileUtils.outputDirectory())
    }

    companion object {
        private const val OUTPUT_DIRECTORY_PROPERTY = "skriptSyntaxGenerator.outputDirectory"
    }
}
