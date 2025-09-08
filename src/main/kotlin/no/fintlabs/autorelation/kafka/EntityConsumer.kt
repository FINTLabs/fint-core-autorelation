package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.kafka.model.RelationOperation
import no.fintlabs.autorelation.kafka.model.RelationRequest
import no.fintlabs.autorelation.kafka.model.ResourceType
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
    private val autoRelation: AutoRelationService
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
        createRelationRequest(consumerRecord.topic(), consumerRecord.value())
            .let { autoRelation.processRequest(it) }

    private fun createRelationRequest(topic: String, resource: Any) =
        RelationRequest(
            orgId = getOrgId(topic),
            type = getResourceType(topic),
            resource = resource,
            operation = RelationOperation.ADD
        )

    private fun getOrgId(topic: String) = topic.substringBefore(".")

    private fun getResourceType(topic: String) =
        topic.substringAfterLast(".")
            .split(".")
            .let { (domain, pkg, resource) -> ResourceType(domain, pkg, resource) }

    private fun getOrgIdAndResourceTypeFromTopic(topic: String): Pair<String, ResourceType>? =
        topic.substringBefore(".") to ResourceType.parse(topic.substringAfterLast("."))

}