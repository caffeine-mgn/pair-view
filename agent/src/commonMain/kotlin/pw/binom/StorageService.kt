package pw.binom

import pw.binom.io.AsyncInput
import pw.binom.properties.AppProperties
import pw.binom.s3.S3Client
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty
import pw.binom.uuid.nextUuid
import kotlin.random.Random

class StorageService {
    private val s3Client: S3Client by inject()
    private val applicationProperties: AppProperties by injectProperty()
    suspend fun storage(stream: AsyncInput): String {
        val key = Random.nextUuid().toString()
        s3Client.putObjectContent(
            bucket = applicationProperties.storage.bucketName,
            key = key,
            regin = applicationProperties.storage.regin,
        ) { output ->
            stream.copyTo(output)
        }
        return key
    }

    suspend fun load(key: String) = s3Client.getObject(
        regin = applicationProperties.storage.regin,
        bucket = applicationProperties.storage.bucketName,
        key = key,
    )
}