package jp.nlaocs.skriptSyntaxGenerator.generator

import jp.nlaocs.skriptSyntaxGenerator.data.SnapshotManifestData
import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory
import jp.nlaocs.skriptSyntaxGenerator.util.FileUtils
import jp.nlaocs.skriptSyntaxGenerator.util.SnapshotDigests

class SyntaxDataGenerator(
    private val dataSource: SnapshotDataSource = ModernSnapshotDataSource()
) {
    private val objectMapper = JacksonFactory.create()

    fun generate() {
        val outputs = SnapshotFormat.normalize(dataSource.collectOutputs())
        val serializedOutputs = outputs.mapValuesTo(linkedMapOf<String, String>()) { (_, data) ->
            objectMapper.writeValueAsString(data)
        }
        val contentDigest = SnapshotDigests.contentDigest(serializedOutputs)
        val manifest = SnapshotManifestData.create(
            files = SnapshotFormat.getAllFiles(),
            contentDigest = contentDigest,
            capabilities = dataSource.capabilities
        )

        serializedOutputs.forEach { (fileName, json) ->
            FileUtils.writeStringToFile(fileName, json)
        }
        FileUtils.writeStringToFile(
            SnapshotFormat.MANIFEST_FILE,
            objectMapper.writeValueAsString(manifest)
        )
    }
}
