package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.model.RelationUpdate
import no.fintlabs.kafka.event.EventProducerFactory
import no.fintlabs.kafka.event.EventProducerRecord
import no.fintlabs.kafka.event.topic.EventTopicNameParameters
import no.fintlabs.kafka.event.topic.EventTopicService
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RelationUpdateEventProducer(
    eventTopicService: EventTopicService,
    eventProducerFactory: EventProducerFactory
) {

    private val eventTopic = createEventTopic()
    private val eventProducer = eventProducerFactory.createProducer(RelationUpdate::class.java)

    init {
        eventTopicService.ensureTopic(eventTopic, Duration.ofHours(1).toMillis())
    }

    fun publishRelationUpdate(relationUpdate: RelationUpdate) =
        eventProducer.send(
            EventProducerRecord.builder<RelationUpdate>()
                .topicNameParameters(eventTopic)
                .value(relationUpdate)
                .build()
        )

    private fun createEventTopic() =
        EventTopicNameParameters.builder()
            .orgId("fintlabs-no")
            .domainContext("fint-core")
            .eventName("relation-update")
            .build()

}