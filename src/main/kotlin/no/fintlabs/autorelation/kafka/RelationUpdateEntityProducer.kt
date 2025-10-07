package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.model.RelationUpdate
import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.EntityProducerRecord
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.kafka.entity.topic.EntityTopicService
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RelationUpdateEntityProducer(
    entityTopicService: EntityTopicService,
    entityProducerFactory: EntityProducerFactory
) {

    private val entityTopic = createEntityTopic()
    private val entityProducer = entityProducerFactory.createProducer(RelationUpdate::class.java)

    init {
        entityTopicService.ensureTopic(entityTopic, Duration.ofHours(1).toMillis())
    }

    fun publishRelationUpdate(relationUpdate: RelationUpdate) =
        entityProducer.send(
            EntityProducerRecord.builder<RelationUpdate>()
                .topicNameParameters(entityTopic)
                .value(relationUpdate)
                .build()
        )

    private fun createEntityTopic() =
        EntityTopicNameParameters.builder()
            .orgId("fintlabs-no")
            .domainContext("fint-core")
            .resource("relation-update")
            .build()

}