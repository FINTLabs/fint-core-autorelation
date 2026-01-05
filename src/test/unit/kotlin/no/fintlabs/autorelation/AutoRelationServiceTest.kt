package no.fintlabs.autorelation

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.fint.model.resource.FintResource
import no.fintlabs.autorelation.cache.RelationCache
import no.fintlabs.autorelation.kafka.RelationUpdateProducer
import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.autorelation.model.RelationSyncRule
import no.fintlabs.autorelation.model.RelationUpdate
import no.fintlabs.autorelation.model.ResourceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AutoRelationServiceTest {

    @MockK lateinit var relationCache: RelationCache
    @MockK lateinit var resourceMapper: ResourceMapperService
    @MockK lateinit var producer: RelationUpdateProducer

    @InjectMockKs
    lateinit var sut: AutoRelationService

    @Test
    fun `returns 0 and does not publish when type is not a trigger`() {
        val request = mockk<RelationRequest>()
        val type = mockk<ResourceType>()
        every { request.type } returns type
        every { relationCache.isTriggerResourceType(type) } returns false

        val result = sut.processRequest(request)

        assertEquals(0, result)
        verify(exactly = 1) { relationCache.isTriggerResourceType(type) }
        verify(exactly = 0) {
            resourceMapper.mapResource(any(), any())
            relationCache.rulesForTrigger(any())
            producer.publishRelationUpdate(any())
        }
    }

    @Test
    fun `returns 0 and does not publish when mapper returns null`() {
        val type = mockk<ResourceType>()
        val request = request(type, mapOf("id" to "1"))
        every { relationCache.isTriggerResourceType(type) } returns true
        every { resourceMapper.mapResource(type, request.resource) } returns null

        val result = sut.processRequest(request)

        assertEquals(0, result)
        verify(exactly = 1) {
            relationCache.isTriggerResourceType(type)
            resourceMapper.mapResource(type, request.resource)
        }
        verify(exactly = 0) {
            relationCache.rulesForTrigger(any())
            producer.publishRelationUpdate(any())
        }
    }

    @Test
    fun `returns 0 and does not publish when there are no rules`() {
        val type = mockk<ResourceType>()
        val request = request(type, mapOf("id" to "1"))
        val fintResource = mockk<FintResource>()
        every { relationCache.isTriggerResourceType(type) } returns true
        every { resourceMapper.mapResource(type, request.resource) } returns fintResource
        every { relationCache.rulesForTrigger(type) } returns emptyList()

        val result = sut.processRequest(request)

        assertEquals(0, result)
        verify(exactly = 1) {
            relationCache.isTriggerResourceType(type)
            resourceMapper.mapResource(type, request.resource)
            relationCache.rulesForTrigger(type)
        }
        verify(exactly = 0) { producer.publishRelationUpdate(any()) }
    }

    @Test
    fun `publishes one update per non-null factory result and returns that count`() {
        val type = mockk<ResourceType>()
        val request = request(type, mapOf("id" to "1"))
        val fintResource = mockk<FintResource>()
        val rule1 = mockk<RelationSyncRule>()
        val rule2 = mockk<RelationSyncRule>()
        val rule3 = mockk<RelationSyncRule>()
        val update1 = mockk<RelationUpdate>()
        val update3 = mockk<RelationUpdate>()

        every { relationCache.isTriggerResourceType(type) } returns true
        every { resourceMapper.mapResource(type, request.resource) } returns fintResource
        every { relationCache.rulesForTrigger(type) } returns listOf(rule1, rule2, rule3)

        mockkObject(RelationUpdate.Companion)
        try {
            every { RelationUpdate.from(request, fintResource, rule1) } returns update1
            every { RelationUpdate.from(request, fintResource, rule2) } returns null
            every { RelationUpdate.from(request, fintResource, rule3) } returns update3
            every { producer.publishRelationUpdate(any()) } returns CompletableFuture.completedFuture(null)

            val result = sut.processRequest(request)

            assertEquals(2, result)
            verify(exactly = 1) {
                relationCache.isTriggerResourceType(type)
                resourceMapper.mapResource(type, request.resource)
                relationCache.rulesForTrigger(type)
                RelationUpdate.from(request, fintResource, rule1)
                RelationUpdate.from(request, fintResource, rule2)
                RelationUpdate.from(request, fintResource, rule3)
                producer.publishRelationUpdate(update1)
                producer.publishRelationUpdate(update3)
            }
            verify(exactly = 0) { producer.publishRelationUpdate(match { it !== update1 && it !== update3 }) }
        } finally {
            unmockkObject(RelationUpdate.Companion)
        }
    }

    private fun request(type: ResourceType, raw: Map<String, Any>): RelationRequest =
        mockk<RelationRequest>().apply {
            every { this@apply.type } returns type
            every { this@apply.resource } returns raw
        }
}
