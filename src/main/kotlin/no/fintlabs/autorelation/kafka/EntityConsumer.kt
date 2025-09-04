package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern
import no.fintlabs.kafka.entity.EntityConsumerFactoryService
import no.fintlabs.kafka.entity.topic.EntityTopicNamePatternParameters
import no.fintlabs.metamodel.MetamodelService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class EntityConsumer(
    private val metamodelService: MetamodelService,
    private val autoReflectionService: AutoRelationService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
        consumerRecord.topic().split("-").lastOrNull()
            ?.let { resourceName -> autoReflectionService.processEntity(consumerRecord.value(), resourceName) }
            ?: logger.error("Couldn't get resource name from entity topic")

}