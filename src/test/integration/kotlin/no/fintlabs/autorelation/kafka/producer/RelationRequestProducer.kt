package no.fintlabs.autorelation.kafka.producer

import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.kafka.event.EventProducerFactory
import no.fintlabs.kafka.event.EventProducerRecord
import no.fintlabs.kafka.event.topic.EventTopicNameParameters
import no.fintlabs.kafka.event.topic.EventTopicService
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RelationRequestProducer(
    eventProducerFactory: EventProducerFactory,
    private val eventTopicService: EventTopicService
) {

    private val entityProducer = eventProducerFactory.createProducer(RelationRequest::class.java)
    private val topic = createTopic()

    fun produceEvent(relationRequest: RelationRequest) {
        ensureTopic(topic)
        entityProducer.send(
            EventProducerRecord.builder<RelationRequest>()
                .topicNameParameters(topic)
                .value(relationRequest)
                .build()
        )
    }

    private fun ensureTopic(topic: EventTopicNameParameters) =
        eventTopicService.ensureTopic(topic, Duration.ofSeconds(500).toMillis())

    private fun createTopic() =
        EventTopicNameParameters.builder()
            .orgId("fintlabs-no")
            .domainContext("fint-core")
            .eventName("relation-request")
            .build()

}