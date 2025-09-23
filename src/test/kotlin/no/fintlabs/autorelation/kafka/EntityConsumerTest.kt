package no.fintlabs.autorelation.kafka

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.utdanning.vurdering.ElevfravarResource
import no.fintlabs.autorelation.AutoRelationService
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
class EntityConsumerTest {

    private val topicResource = "fravarsregistrering"

    @Autowired
    lateinit var entityProducer: EntityProducer

    @MockitoSpyBean
    lateinit var entityConsumer: EntityConsumer

    @MockitoSpyBean
    lateinit var autoRelation: AutoRelationService

    @BeforeEach
    fun resetSpies() {
        reset(entityConsumer, autoRelation)
    }

    @Test
    fun `consumes entity when produced to topic`() {
        entityProducer.produceEntity(topicResource, createElevfravarResource())

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(entityConsumer, atLeastOnce()).consumeRecord(any())
            verify(autoRelation, atLeastOnce()).processRequest(any())
        }
    }

    @Test
    fun `does not consume entity if it was produced by consumer itself`() {
        entityProducer.produceEntity(topicResource, createElevfravarResource(), consumerProduced = true)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(entityConsumer, atLeastOnce()).consumeRecord(any())
            verify(autoRelation, never()).processRequest(any())
        }
    }

    private fun createElevfravarResource(id: String = "123") =
        ElevfravarResource().apply {
            systemId = Identifikator().apply {
                identifikatorverdi = id
            }
        }
}
