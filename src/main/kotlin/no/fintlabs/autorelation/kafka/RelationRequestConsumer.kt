package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.kafka.event.EventConsumerFactoryService
import no.fintlabs.kafka.event.topic.EventTopicNameParameters
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class RelationRequestConsumer(
    private val autoRelationService: AutoRelationService
) {

    @Bean
    fun relationRequestContainer(
        eventConsumerFactory: EventConsumerFactoryService,
    ): ConcurrentMessageListenerContainer<String?, RelationRequest> =
        eventConsumerFactory
            .createFactory(
                RelationRequest::class.java,
                this::consumeRecord
            )
            .createContainer(
                EventTopicNameParameters.builder()
                    .orgId("fintlabs-no")
                    .domainContext("fint-core")
                    .eventName("relation-request")
                    .build()
            )


    fun consumeRecord(consumerRecord: ConsumerRecord<String, RelationRequest>) =
        autoRelationService.processRequest(consumerRecord.value())

}