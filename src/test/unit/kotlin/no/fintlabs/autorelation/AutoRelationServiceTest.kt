package no.fintlabs.autorelation

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import no.fint.model.resource.FintResource
import no.fintlabs.autorelation.cache.RelationRuleRegistry
import no.fintlabs.autorelation.kafka.RelationUpdateProducer
import no.fintlabs.autorelation.model.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

@ExtendWith(MockKExtension::class)
class AutoRelationServiceTest {

    @MockK
    lateinit var relationRuleRegistry: RelationRuleRegistry

    @MockK
    lateinit var resourceConverter: ResourceConverterService

    @MockK
    lateinit var relationUpdateProducer: RelationUpdateProducer

    @MockK(relaxed = true)
    lateinit var metricService: MetricService

    @InjectMockKs
    lateinit var autoRelationService: AutoRelationService

    @BeforeEach
    fun setup() {
        mockkStatic("no.fintlabs.autorelation.model.RelationUpdateKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("no.fintlabs.autorelation.model.RelationUpdateKt")
    }

    @Test
    fun `processRequest should return 0 and stop if conversion fails`() = runTest {
        val event = createEvent()

        every { resourceConverter.convertToFintResource(any(), any()) } throws
                ResourceConversionException("Bad JSON")

        val result = autoRelationService.processRequest(event)

        assertEquals(0, result)

        verify {
            metricService.incrementRelationFailure(
                sourceId = "123",
                resourceName = "elevfravar",
                reason = MetricReason.CONVERSION_FAILED
            )
        }

        verify(exactly = 0) { relationRuleRegistry.getRules(any()) }
    }

    @Test
    fun `processRequest should process all valid rules and publish updates`() = runTest {
        val event = createEvent()
        val dummyResource = mockk<FintResource>()

        val rule1 = mockk<RelationSyncRule>()
        val rule2 = mockk<RelationSyncRule>()
        val update1 = mockk<RelationUpdate>(relaxed = true) {
            every { targetEntity.resourceName } returns "target1"
        }
        val update2 = mockk<RelationUpdate>(relaxed = true) {
            every { targetEntity.resourceName } returns "target2"
        }

        every { resourceConverter.convertToFintResource(any(), any()) } returns dummyResource
        every { relationRuleRegistry.getRules(any()) } returns listOf(rule1, rule2)

        every { rule1.toRelationUpdate(event, dummyResource) } returns update1
        every { rule2.toRelationUpdate(event, dummyResource) } returns update2

        every { relationUpdateProducer.publishRelationUpdate(any()) } returns
                CompletableFuture.completedFuture(null)

        val result = autoRelationService.processRequest(event)

        assertEquals(2, result)

        verify(exactly = 1) { relationUpdateProducer.publishRelationUpdate(update1) }
        verify(exactly = 1) { relationUpdateProducer.publishRelationUpdate(update2) }
        verify(exactly = 1) { metricService.incrementRelationSuccess("target1") }
        verify(exactly = 1) { metricService.incrementRelationSuccess("target2") }
    }

    @Test
    fun `processRequest should handle partial failure (one rule fails, one succeeds)`() = runTest {
        val event = createEvent()
        val dummyResource = mockk<FintResource>()

        val validRule = mockk<RelationSyncRule>()
        val invalidRule = mockk<RelationSyncRule>() // This one will throw exception

        val validUpdate = mockk<RelationUpdate>(relaxed = true) {
            every { targetEntity.resourceName } returns "validTarget"
        }

        every { resourceConverter.convertToFintResource(any(), any()) } returns dummyResource
        every { relationRuleRegistry.getRules(any()) } returns listOf(validRule, invalidRule)

        // Valid Rule -> Success
        every { validRule.toRelationUpdate(event, dummyResource) } returns validUpdate

        // Invalid Rule -> Throws Domain Exception (e.g. Mandatory Link Missing)
        every { invalidRule.toRelationUpdate(event, dummyResource) } throws
                MissingMandatoryLinkException("someLink")

        every { relationUpdateProducer.publishRelationUpdate(any()) } returns
                CompletableFuture.completedFuture(null)

        val result = autoRelationService.processRequest(event)

        assertEquals(1, result) // Only 1 succeeded

        verify(exactly = 1) { relationUpdateProducer.publishRelationUpdate(validUpdate) }

        verify {
            metricService.incrementRelationFailure(
                sourceId = "123",
                resourceName = "elevfravar",
                reason = MetricReason.MISSING_MANDATORY_LINK
            )
        }
    }

    @Test
    fun `processRequest should handle infrastructure (Kafka) failure`() = runTest {
        val event = createEvent()
        val dummyResource = mockk<FintResource>()
        val rule = mockk<RelationSyncRule>()
        val update = mockk<RelationUpdate>(relaxed = true)

        every { resourceConverter.convertToFintResource(any(), any()) } returns dummyResource
        every { relationRuleRegistry.getRules(any()) } returns listOf(rule)
        every { rule.toRelationUpdate(event, dummyResource) } returns update

        val failedFuture = CompletableFuture<SendResult<String, RelationUpdate>>()
        failedFuture.completeExceptionally(RuntimeException("Kafka is down"))

        every { relationUpdateProducer.publishRelationUpdate(update) } returns failedFuture

        val result = autoRelationService.processRequest(event)

        assertEquals(0, result)

        verify(exactly = 1) { relationUpdateProducer.publishRelationUpdate(update) }

        verify {
            metricService.incrementRelationFailure(
                sourceId = "123",
                resourceName = "elevfravar",
                reason = any()
            )
        }
    }

    private fun createEvent() = mockk<RelationEvent> {
        every { sourceEntity.resourceName } returns "elevfravar"
        every { sourceData } returns "mockData"
        every { sourceId } returns "123"
    }
}