package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.model.RelationOperation
import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.autorelation.model.ResourceType
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern
import no.fintlabs.kafka.entity.EntityConsumerFactoryService
import no.fintlabs.kafka.entity.topic.EntityTopicNamePatternParameters
import no.fintlabs.metamodel.MetamodelService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Component
class EntityConsumer(
    private val metamodelService: MetamodelService,
    private val autoRelation: AutoRelationService
) {

    @Bean
    fun concurrentMessageListenerContainer(
        entityConsumerFactoryService: EntityConsumerFactoryService,
    ): ConcurrentMessageListenerContainer<String?, Any> {
        return entityConsumerFactoryService
            .createFactory(
                Any::class.java,
                this::consumeRecord
            )
            .createContainer(
                EntityTopicNamePatternParameters.builder()
                    .orgId(FormattedTopicComponentPattern.any())
                    .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                    .resource(FormattedTopicComponentPattern.anyOf(*formattedResourceTopics().toTypedArray()))
                    .build()
            )
    }

    private fun formattedResourceTopics(): List<String> =
        metamodelService.getComponents().flatMap { component ->
            component.resources.map { "${component.domainName}-${component.packageName}-${it.name}".lowercase() }
        }

    fun consumeRecord(consumerRecord: ConsumerRecord<String, Any>) =
        consumerRecord.takeIf { shouldBeProcessed(it.value(), it.headers()) }
            ?.let { createRelationRequest(it) }
            ?.let { autoRelation.processRequest(it) }

    fun shouldBeProcessed(value: Any?, headers: Headers) =
        value != null && headers.lastHeader("consumer") == null

    private fun createRelationRequest(consumerRecord: ConsumerRecord<String, Any>) =
        RelationRequest(
            orgId = getOrgId(consumerRecord.topic()),
            type = getResourceType(consumerRecord.topic()),
            resource = consumerRecord.value(),
            operation = RelationOperation.ADD,
            entityRetentionTime = getEntityRetentionTime(consumerRecord.headers())
        )

    private fun getEntityRetentionTime(header: Headers): Long? =
        header.lastHeader("entity-retention-time")
            ?.value()?.toLong()

    private fun ByteArray.toLong(): Long? =
        this.takeIf { it.size == Long.SIZE_BYTES }
            ?.let { ByteBuffer.wrap(it) }
            ?.order(ByteOrder.BIG_ENDIAN)?.long

    private fun getOrgId(topic: String) = topic.substringBefore(".")

    private fun getResourceType(topic: String) =
        topic.substringAfterLast(".")
            .split("-")
            .let { (domain, pkg, resource) -> ResourceType(domain, pkg, resource) }

}