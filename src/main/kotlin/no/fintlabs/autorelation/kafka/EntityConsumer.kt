package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.kafka.mapper.RelationRequestMapper
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern
import no.fintlabs.kafka.entity.EntityConsumerFactoryService
import no.fintlabs.kafka.entity.topic.EntityTopicNamePatternParameters
import no.fintlabs.metamodel.MetamodelService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class EntityConsumer(
    private val metamodelService: MetamodelService,
    private val autoRelation: AutoRelationService,
    private val relationRequestMapper: RelationRequestMapper
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

    fun consumeRecord(consumerRecord: ConsumerRecord<String, Any>) =
        consumerRecord.takeIf { shouldBeProcessed(it.value(), it.headers()) }
            ?.let { relationRequestMapper.createRelationRequest(it) }
            ?.let { autoRelation.processRequest(it) }

    fun shouldBeProcessed(value: Any?, headers: Headers) =
        value != null && headers.lastHeader("consumer") == null

    private fun formattedResourceTopics(): List<String> =
        metamodelService.getComponents().flatMap { component ->
            component.resources.map { "${component.domainName}-${component.packageName}-${it.name}".lowercase() }
        }

}