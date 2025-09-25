package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.utils.KafkaUtils
import org.springframework.beans.factory.annotation.Autowired

class RelationRequestConsumerTest @Autowired constructor(
    private val kafkaUtils: KafkaUtils
) {
}