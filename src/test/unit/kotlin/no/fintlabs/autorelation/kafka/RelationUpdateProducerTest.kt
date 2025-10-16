package no.fintlabs.autorelation.kafka

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.fintlabs.autorelation.kafka.RelationUpdateProducer.Companion.RETENTION_TIME_IN_DAYS
import no.fintlabs.autorelation.model.RelationUpdate
import no.fintlabs.kafka.entity.EntityProducer
import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.kafka.entity.topic.EntityTopicService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class RelationUpdateProducerTest {

    @MockK
    lateinit var entityTopicService: EntityTopicService
    @MockK
    lateinit var entityProducerFactory: EntityProducerFactory
    @MockK
    lateinit var producer: EntityProducer<RelationUpdate>

    private companion object {
        const val ORG = "fintlabs-no"
        const val DOMAIN = "fint-core"
        const val RESOURCE = "relation-update"
        val RETENTION_MILLIS = Duration.ofDays(RETENTION_TIME_IN_DAYS).toMillis()
    }

    @BeforeEach
    fun setUp() {
        every { entityProducerFactory.createProducer(RelationUpdate::class.java) } returns producer
        every { entityTopicService.ensureTopic(any(), any()) } just Runs
    }

    @AfterEach
    fun cleanup() {
        clearMocks(entityTopicService, entityProducerFactory, producer)
    }


    @Test
    fun `init ensures topic with expected params and retention`() {
        RelationUpdateProducer(entityTopicService, entityProducerFactory)

        verifyOrder {
            entityProducerFactory.createProducer(RelationUpdate::class.java)
            entityTopicService.ensureTopic(
                withArg<EntityTopicNameParameters> {
                    assertEquals(ORG, it.orgId)
                    assertEquals(DOMAIN, it.domainContext)
                    assertEquals(RESOURCE, it.resource)
                },
                RETENTION_MILLIS
            )
        }

        confirmVerified(entityProducerFactory, entityTopicService, producer)
    }

}
