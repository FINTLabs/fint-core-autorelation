package no.fintlabs.autorelation

import no.fintlabs.autorelation.kafka.producer.EntityProducer
import no.fintlabs.autorelation.kafka.KafkaUtils
import no.fintlabs.autorelation.kafka.RelationUpdateEventProducer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.concurrent.TimeUnit

@SpringBootTest
@EmbeddedKafka(partitions = 1, controlledShutdown = true, count = 2)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AutoRelationServiceTest @Autowired constructor(
    private val kafkaUtils: KafkaUtils,
    private val entityProducer: EntityProducer
) {

    @MockitoSpyBean
    private lateinit var autoRelation: AutoRelationService

    @MockitoSpyBean
    private lateinit var relationUpdateProducer: RelationUpdateEventProducer

    private val topicResource = "fravarsregistrering"

    @BeforeEach
    fun resetSetup() {
        kafkaUtils.resetTopics("fintlabs-no.fint-core.entity.utdanning-vurdering-$topicResource")
        reset(autoRelation, relationUpdateProducer)
    }

    @Test
    fun `valid resources gets processed`() {
        val resource = createFravarsregistreringResource(topicResource, "123")

        entityProducer.produceEntity(topicResource, resource, consumerProduced = false)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(autoRelation, atLeastOnce()).processRequest(any())
            verify(relationUpdateProducer, atLeastOnce()).publishRelationUpdate(any())
        }
    }

    @Test
    fun `linkless resource gets skipped`() {
        val resource = createFravarsregistreringResource(topicResource)

        entityProducer.produceEntity(topicResource, resource, consumerProduced = false)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(autoRelation, atLeastOnce()).processRequest(any())
            verify(relationUpdateProducer, never()).publishRelationUpdate(any())
        }
    }

    @Test
    fun `non-controlled resource gets skipped`() {
        val resource = createElevfravarResource(topicResource, elevforholdIds = arrayOf("1234"))

        entityProducer.produceEntity(topicResource, resource, consumerProduced = false)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(autoRelation, atLeastOnce()).processRequest(any())
            verify(relationUpdateProducer, never()).publishRelationUpdate(any())
        }
    }

}