package no.fintlabs.autorelation.kafka

import kotlinx.coroutines.runBlocking // Add this import
import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.kafka.producer.RelationEventProducer
import no.fintlabs.autorelation.model.EntityDescriptor
import no.fintlabs.autorelation.model.RelationEvent
import no.fintlabs.autorelation.model.RelationOperation
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
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
class RelationEventConsumerTest @Autowired constructor(
    private val relationRequestProducer: RelationEventProducer
) {

    @MockitoSpyBean
    private lateinit var autoRelationService: AutoRelationService

    @MockitoSpyBean
    private lateinit var relationRequestConsumer: RelationEventConsumer

    @BeforeEach
    fun setup() {
        reset(autoRelationService, relationRequestConsumer)
    }

    @Test
    fun `process message that matches topic`() {
        val relationRequest = createRelationEvent()

        relationRequestProducer.produceEvent(relationRequest)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            // 1. Standard function: Verify normally
            verify(relationRequestConsumer, times(1)).consumeRecord(any())

            // 2. Suspend function: Verify inside runBlocking
            runBlocking {
                verify(autoRelationService, times(1)).processRequest(relationRequest)
            }
        }
    }

    private fun createRelationEvent() =
        RelationEvent(
            sourceEntity = EntityDescriptor("utdanning", "vurdering", "elevfravar"),
            orgId = "fintlabs.no",
            sourceData = "anything",
            sourceId = "123",
            operation = RelationOperation.DELETE
        )

}