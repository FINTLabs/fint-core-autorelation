package no.fintlabs.autorelation.kafka

import kotlinx.coroutines.runBlocking
import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.model.RelationEvent
import no.fintlabs.kafka.event.EventConsumerFactoryService
import no.fintlabs.kafka.event.topic.EventTopicNameParameters
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class RelationEventConsumer(
    private val autoRelationService: AutoRelationService
) {

    @Bean
    fun relationEventContainer(
        eventConsumerFactory: EventConsumerFactoryService,
    ): ConcurrentMessageListenerContainer<String?, RelationEvent> =
        eventConsumerFactory
            .createFactory(
                RelationEvent::class.java,
                this::consumeRecord
            )
            .createContainer(
                EventTopicNameParameters.builder()
                    .orgId("fintlabs-no")
                    .domainContext("fint-core")
                    .eventName("relation-request")
                    .build()
            )


    fun consumeRecord(consumerRecord: ConsumerRecord<String, RelationEvent>) = runBlocking {
        autoRelationService.processRequest(consumerRecord.value())
    }

}