package no.fintlabs.autorelation.kafka

import kotlinx.coroutines.runBlocking
import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.model.createAddEvent
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern
import no.fintlabs.kafka.entity.EntityConsumerConfiguration
import no.fintlabs.kafka.entity.EntityConsumerFactoryService
import no.fintlabs.kafka.entity.topic.EntityTopicNamePatternParameters
import no.fintlabs.metamodel.MetamodelService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class EntityConsumer(
    private val metamodelService: MetamodelService,
    private val autoRelation: AutoRelationService
) {

    @Bean
    fun entityConsumerContainer(
        entityConsumerFactoryService: EntityConsumerFactoryService,
    ): ConcurrentMessageListenerContainer<String?, Any> =
        entityConsumerFactoryService
            .createFactory(
                Any::class.java,
                this::consumeRecord,
                EntityConsumerConfiguration.builder()
                    .seekingOffsetResetOnAssignment(false)
                    .build()
            )
            .createContainer(
                EntityTopicNamePatternParameters.builder()
                    .orgId(FormattedTopicComponentPattern.any())
                    .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                    .resource(FormattedTopicComponentPattern.anyOf(*formattedResourceTopics().toTypedArray()))
                    .build()
            )

    fun consumeRecord(consumerRecord: ConsumerRecord<String, Any>) = runBlocking {
        createAddEvent(consumerRecord.key(), consumerRecord.topic(), consumerRecord.value())
            .run { autoRelation.processRequest(this) }
    }

    private fun formattedResourceTopics(): List<String> =
        metamodelService.getComponents().flatMap { component ->
            component.resources.map { "${component.domainName}-${component.packageName}-${it.name}".lowercase() }
        }

}