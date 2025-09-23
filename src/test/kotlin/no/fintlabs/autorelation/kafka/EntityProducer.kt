package no.fintlabs.autorelation.kafka

import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.EntityProducerRecord
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.kafka.entity.topic.EntityTopicService
import org.apache.kafka.common.header.internals.RecordHeaders
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class EntityProducer(
    private val entityTopicService: EntityTopicService,
    entityProducerFactory: EntityProducerFactory
) {

    private val entityProducer = entityProducerFactory.createProducer(Any::class.java)

    fun produceEntity(resource: String, resourceObject: Any, consumerProduced: Boolean = false) =
        createEntityTopic(resource).let { topic ->
            ensureTopic(topic)

            entityProducer.send(
                EntityProducerRecord.builder<Any>()
                    .topicNameParameters(topic)
                    .key(UUID.randomUUID().toString())
                    .headers(createConsumerHeader(consumerProduced))
                    .value(resourceObject)
                    .build()
            )
        }

    private fun createConsumerHeader(addConsumer: Boolean) =
        RecordHeaders().apply {
            if (addConsumer)
                add("consumer", byteArrayOf())
        }

    private fun ensureTopic(topic: EntityTopicNameParameters) =
        entityTopicService.ensureTopic(topic, Duration.ofSeconds(500).toMillis())

    private fun createEntityTopic(resource: String) =
        EntityTopicNameParameters.builder()
            .orgId("fintlabs-no")
            .domainContext("fint-core")
            .resource("utdanning-vurdering-$resource")
            .build()

}