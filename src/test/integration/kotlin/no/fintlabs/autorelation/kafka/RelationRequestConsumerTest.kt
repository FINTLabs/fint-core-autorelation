package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.kafka.producer.RelationRequestProducer
import no.fintlabs.autorelation.model.RelationOperation
import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.autorelation.model.ResourceType
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
class RelationRequestConsumerTest @Autowired constructor(
    private val relationRequestProducer: RelationRequestProducer
) {

    @MockitoSpyBean
    private lateinit var autoRelationService: AutoRelationService

    @MockitoSpyBean
    private lateinit var relationRequestConsumer: RelationRequestConsumer

    @BeforeEach
    fun setup() {
        reset(autoRelationService, relationRequestConsumer)
    }

    @Test
    fun `process message that matches topic`() {
        val relationRequest = createRelationRequest()

        relationRequestProducer.produceEvent(relationRequest)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            verify(relationRequestConsumer, times(1)).consumeRecord(any())
            verify(autoRelationService, times(1)).processRequest(relationRequest)
        }
    }

    private fun createRelationRequest() =
        RelationRequest(
            type = ResourceType("utdanning", "vurdering", "elevfravar"),
            orgId = "fintlabs.no",
            resource = "anything",
            operation = RelationOperation.DELETE,
            entityRetentionTime = null
        )

}