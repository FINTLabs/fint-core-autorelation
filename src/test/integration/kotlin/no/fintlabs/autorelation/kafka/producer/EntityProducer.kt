package no.fintlabs.autorelation.kafka.producer

import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.EntityProducerRecord
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import org.springframework.stereotype.Component
import java.util.*

@Component
class EntityProducer(
    entityProducerFactory: EntityProducerFactory
) {

    private val entityProducer = entityProducerFactory.createProducer(Any::class.java)

    fun produceEntity(resource: String, resourceObject: Any) =
        entityProducer.send(
            EntityProducerRecord.builder<Any>()
                .topicNameParameters(createEntityTopic(resource))
                .key(UUID.randomUUID().toString())
                .value(resourceObject)
                .build()
        )

    private fun createEntityTopic(resource: String) =
        EntityTopicNameParameters.builder()
            .orgId("fintlabs-no")
            .domainContext("fint-core")
            .resource("utdanning-vurdering-$resource")
            .build()

}