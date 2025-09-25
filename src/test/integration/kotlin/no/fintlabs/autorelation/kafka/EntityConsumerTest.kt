package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.utils.createFravarsregistreringResource
import no.fintlabs.autorelation.kafka.producer.EntityProducer
import no.fintlabs.autorelation.utils.KafkaUtils
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.concurrent.TimeUnit

@SpringBootTest
@EmbeddedKafka(partitions = 1, controlledShutdown = true, count = 2)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EntityConsumerTest @Autowired constructor(
    private val kafkaUtils: KafkaUtils,
    private val entityProducer: EntityProducer
) {

    private val topicResource = "fravarsregistrering"

    @MockitoSpyBean
    lateinit var entityConsumer: EntityConsumer

    @MockitoSpyBean
    lateinit var autoRelation: AutoRelationService

    @BeforeEach
    fun resetSpies() {
        kafkaUtils.resetTopics("fintlabs-no.fint-core.entity.utdanning-vurdering-$topicResource")
        reset(entityConsumer, autoRelation)
    }

    @Test
    fun `consumes entity when produced to topic`() {
        entityProducer.produceEntity(topicResource, createFravarsregistreringResource())

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(entityConsumer, atLeastOnce()).consumeRecord(any())
            verify(autoRelation, atLeastOnce()).processRequest(any())
        }
    }

    @Test
    fun `does not consume entity if it was produced by consumer itself`() {
        entityProducer.produceEntity(topicResource, createFravarsregistreringResource(), consumerProduced = true)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(entityConsumer, atLeastOnce()).consumeRecord(any())
            verify(autoRelation, never()).processRequest(any())
        }
    }

}
