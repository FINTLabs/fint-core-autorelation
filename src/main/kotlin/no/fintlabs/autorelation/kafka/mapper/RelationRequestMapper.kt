package no.fintlabs.autorelation.kafka.mapper

import no.fintlabs.autorelation.model.RelationOperation
import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.autorelation.model.ResourceType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Component
class RelationRequestMapper {

    fun createRelationRequest(consumerRecord: ConsumerRecord<String, Any>) =
        RelationRequest(
            orgId = getOrgId(consumerRecord.topic()),
            type = getResourceType(consumerRecord.topic()),
            resource = consumerRecord.value(),
            operation = RelationOperation.ADD,
            entityRetentionTime = getEntityRetentionTime(consumerRecord.headers())
        )

    private fun getOrgId(topic: String) = topic.substringBefore(".")

    private fun getResourceType(topic: String) =
        topic.substringAfterLast(".")
            .split("-")
            .let { (domain, pkg, resource) -> ResourceType(domain, pkg, resource) }

    private fun getEntityRetentionTime(header: Headers): Long? =
        header.lastHeader("entity-retention-time")
            ?.value()?.toLong()

    private fun ByteArray.toLong(): Long? =
        this.takeIf { it.size == Long.SIZE_BYTES }
            ?.let { ByteBuffer.wrap(it) }
            ?.order(ByteOrder.BIG_ENDIAN)?.long

}