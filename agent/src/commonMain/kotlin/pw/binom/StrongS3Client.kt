package pw.binom

import pw.binom.http.client.HttpClientRunnable
import pw.binom.io.AsyncOutput
import pw.binom.io.http.range.Range
import pw.binom.properties.AppProperties
import pw.binom.s3.ObjectAsyncOutput
import pw.binom.s3.S3Client
import pw.binom.s3.S3ClientImpl
import pw.binom.s3.dto.Part
import pw.binom.strong.inject
import pw.binom.strong.properties.injectProperty
import pw.binom.url.toURL

class StrongS3Client:S3Client {

    private val clientRunnable: HttpClientRunnable by inject()
    private val applicationProperties: AppProperties by injectProperty()

    private var client: S3Client? = null

    private fun getOrCreateClient(): S3Client {
        var client = client
        if (client != null) {
            return client
        }
        client = S3ClientImpl(
            url = applicationProperties.storage.url.toURL(),
            accessKey = applicationProperties.storage.accessKey,
            secretAccessKey = applicationProperties.storage.secretKey,
            client = clientRunnable,
        )
        this.client = client
        return client
    }

    override suspend fun createBucket(name: String, regin: String, locationConstraint: String?) =
        getOrCreateClient().createBucket(
            name = name,
            regin = regin,
            locationConstraint = locationConstraint,
        )

    override suspend fun deleteObject(bucket: String, key: String, regin: String) =
        getOrCreateClient().deleteObject(bucket, key, regin)

    override suspend fun putObjectContent(
        bucket: String,
        key: String,
        regin: String,
        contentType: String?,
        packageSize: Int,
        payload: suspend (ObjectAsyncOutput) -> Unit,
    ) =
        getOrCreateClient().putObjectContent(
            bucket = bucket,
            key = key,
            regin = regin,
            contentType = contentType,
            packageSize = packageSize,
            payload = payload,
        )

    override suspend fun putObject(
        bucket: String,
        key: String,
        regin: String,
        payloadContentLength: Long?,
        partNumber: Int?,
        uploadId: String?,
        payloadSha256: ByteArray?,
        payload: suspend (AsyncOutput) -> Unit,
    ) = getOrCreateClient().putObject(
        bucket = bucket,
        key = key,
        regin = regin,
        payloadContentLength = payloadContentLength,
        partNumber = partNumber,
        uploadId = uploadId,
        payloadSha256 = payloadSha256,
        payload = payload,
    )

    override suspend fun copyObject(
        regin: String,
        sourceBucket: String,
        sourceKey: String,
        destinationBucket: String,
        destinationKey: String,
    ) = getOrCreateClient().copyObject(
        regin = regin,
        sourceBucket = sourceBucket,
        sourceKey = sourceKey,
        destinationBucket = destinationBucket,
        destinationKey = destinationKey,
    )

    override suspend fun listObject2(
        regin: String,
        bucket: String,
        continuationToken: String?,
        fetchOwner: Boolean?,
        maxKeys: Int,
        startAfter: String?,
        prefix: String?,
        xAmzExpectedBucketOwner: String?,
        xAmzRequestPayer: String?,
        delimiter: String?,
    ) = getOrCreateClient().listObject2(
        regin = regin,
        bucket = bucket,
        continuationToken = continuationToken,
        fetchOwner = fetchOwner,
        maxKeys = maxKeys,
        startAfter = startAfter,
        prefix = prefix,
        xAmzExpectedBucketOwner = xAmzExpectedBucketOwner,
        xAmzRequestPayer = xAmzRequestPayer,
        delimiter = delimiter,
    )

    override fun listObject2Flow(
        regin: String,
        bucket: String,
        fetchOwner: Boolean?,
        partSize: Int,
        startAfter: String?,
        prefix: String?,
        xAmzExpectedBucketOwner: String?,
        xAmzRequestPayer: String?,
    ) = getOrCreateClient().listObject2Flow(
        regin = regin,
        bucket = bucket,
        fetchOwner = fetchOwner,
        partSize = partSize,
        startAfter = startAfter,
        prefix = prefix,
        xAmzExpectedBucketOwner = xAmzExpectedBucketOwner,
        xAmzRequestPayer = xAmzRequestPayer,
    )

    override suspend fun headObject(
        regin: String,
        bucket: String,
        key: String,
        partNumber: Int?,
        versionId: String?,
    ) = getOrCreateClient().headObject(
        regin = regin,
        bucket = bucket,
        key = key,
        partNumber = partNumber,
        versionId = versionId,
    )

    override suspend fun getObject(
        regin: String,
        bucket: String,
        key: String,
        partNumber: Int?,
        versionId: String?,
        range: List<Range>,
    ) = getOrCreateClient().getObject(
        regin = regin,
        bucket = bucket,
        key = key,
        partNumber = partNumber,
        versionId = versionId,
        range = range,
    )

    override suspend fun listBuckets(regin: String) =
        getOrCreateClient().listBuckets(regin)

    override suspend fun createMultipartUpload(
        regin: String,
        bucket: String,
        key: String,
        contentType: String?,
    ) = getOrCreateClient().createMultipartUpload(
        regin = regin,
        bucket = bucket,
        key = key,
        contentType = contentType,
    )

    override suspend fun completeMultipartUpload(
        regin: String,
        bucket: String,
        key: String,
        uploadId: String,
        parts: List<Part>,
    ) = getOrCreateClient().completeMultipartUpload(
        regin = regin,
        bucket = bucket,
        key = key,
        uploadId = uploadId,
        parts = parts,
    )
}