package no.fintlabs.autorelation.kafka.mapper

import no.fintlabs.autorelation.model.RelationOperation
import no.fintlabs.autorelation.model.ResourceType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RelationRequestMapperTest {

    private val mapper = RelationRequestMapper()

    @Test
    fun `throws error when topic format is wrong`() {
        val topic = "not-valid"

        assertThrows<Exception> {
            mapper.createRelationRequest(record(topic, ""))
        }
    }

    @Test
    fun `maps orgId, resourceType, resource and operation without header`() {
        val topic = "rogfk-no.fint-core.entity.utdanning-elev-elev"
        val value = "bob"

        val req = mapper.createRelationRequest(record(topic, value))

        assertEquals("rogfk-no", req.orgId, "orgId should be the substring before the first '.'")
        assertEquals(
            ResourceType("utdanning", "elev", "elev"),
            req.type,
            "ResourceType should be parsed from the last topic segment"
        )
        assertEquals(value, req.resource, "resource should be the ConsumerRecord value")
        assertEquals(RelationOperation.ADD, req.operation, "operation should be constant ADD")
        assertNull(req.entityRetentionTime, "entityRetentionTime should be null when header is missing")
    }

    @Test
    fun `maps entityRetentionTime when header is present and 8 bytes big-endian`() {
        val topic = "agder.no.fintlabs.autorelation.utdanning-person-elev"
        val value = "payload"
        val retentionSeconds = 3600L
        val headers = RecordHeaders()
            .add("entity-retention-time", bigEndianBytes(retentionSeconds))

        val req = mapper.createRelationRequest(record(topic, value, headers))

        assertEquals(
            retentionSeconds,
            req.entityRetentionTime,
            "entityRetentionTime should be decoded from big-endian 8 bytes"
        )
    }

    @Test
    fun `entityRetentionTime is null when header has wrong size`() {
        val topic = "vlfk.no.fintlabs.autorelation.utdanning-person-elev"
        val value = "payload"
        val wrongSizedBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(42).array()
        val headers = RecordHeaders()
            .add("entity-retention-time", wrongSizedBytes)

        val req = mapper.createRelationRequest(record(topic, value, headers))

        assertNull(req.entityRetentionTime, "Wrong-sized header should yield null")
    }

    private fun bigEndianBytes(value: Long): ByteArray =
        ByteBuffer.allocate(java.lang.Long.BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(value)
            .array()

    private fun record(
        topic: String,
        value: Any,
        headers: Headers = RecordHeaders()
    ): ConsumerRecord<String, Any> =
        ConsumerRecord(
            topic,
            1,
            0L,
            0L,
            TimestampType.CREATE_TIME,
            0,
            0,
            UUID.randomUUID().toString(),
            value,
            headers,
            Optional.of(0)
        )

}