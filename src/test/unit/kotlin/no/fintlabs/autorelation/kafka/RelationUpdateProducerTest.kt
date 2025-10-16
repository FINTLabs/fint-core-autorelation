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
import java.util.UUID
import java.util.concurrent.CompletableFuture
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

    @Test
    fun `publish sends record with resource id as key and full value`() {
        val expectedKey = UUID.randomUUID().toString()
        val relationUpdate = mockk<RelationUpdate>()

        every { relationUpdate.resource.id } returns expectedKey
        every { producer.send(any()) } returns CompletableFuture.completedFuture(null)

        val sut = RelationUpdateProducer(entityTopicService, entityProducerFactory)

        sut.publishRelationUpdate(relationUpdate)

        verify(exactly = 1) {
            producer.send(withArg { rec ->
                assertEquals(expectedKey, rec.key)
                assertEquals(relationUpdate, rec.value)

                rec.topicNameParameters.let {
                    assertEquals(ORG, it.orgId)
                    assertEquals(DOMAIN, it.domainContext)
                    assertEquals(RESOURCE, it.resource)
                }

            })
        }
        verify(exactly = 1) { entityProducerFactory.createProducer(RelationUpdate::class.java) }
        verify(exactly = 1) { entityTopicService.ensureTopic(any(), RETENTION_MILLIS) }

        confirmVerified(entityProducerFactory, entityTopicService, producer)
    }
}


