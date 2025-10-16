package no.fintlabs.autorelation.kafka

import io.mockk.*
import no.fintlabs.autorelation.model.RelationUpdate
import no.fintlabs.kafka.entity.EntityProducer
import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.kafka.entity.topic.EntityTopicService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

class RelationUpdateProducerTest {

    val entityTopicService: EntityTopicService = mockk()
    val entityProducerFactory: EntityProducerFactory = mockk()
    val producer: EntityProducer<RelationUpdate> = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(entityTopicService, entityProducerFactory, producer)
    }


    @Test
    fun `init ensures topic with expected params and retention`() {
        val retentionTime = Duration.ofDays(7).toMillis()

        every { entityProducerFactory.createProducer(RelationUpdate::class.java) } returns producer

        val topicSlot = slot<EntityTopicNameParameters>()
        every { entityTopicService.ensureTopic(capture(topicSlot), retentionTime) } just Runs

        RelationUpdateProducer(entityTopicService, entityProducerFactory)

        verify(exactly = 1) { entityProducerFactory.createProducer(RelationUpdate::class.java) }
        verify(exactly = 1) { entityTopicService.ensureTopic(capture(topicSlot), retentionTime) }

        val capturedTopic = topicSlot.captured
        assertEquals("fintlabs-no", capturedTopic.orgId)
        assertEquals("fint-core", capturedTopic.domainContext)
        assertEquals("relation-update", capturedTopic.resource)

        confirmVerified(entityProducerFactory, entityTopicService)
    }

}
