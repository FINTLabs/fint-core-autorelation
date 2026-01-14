package no.fintlabs.autorelation

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.fint.model.resource.utdanning.vurdering.ElevfravarResource
import no.fintlabs.autorelation.model.EntityDescriptor
import no.fintlabs.autorelation.model.ResourceConversionException
import no.fintlabs.metamodel.MetamodelService
import no.fintlabs.metamodel.model.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ResourceConverterServiceTest {

    @MockK
    lateinit var metamodelService: MetamodelService

    @MockK
    lateinit var objectMapper: ObjectMapper

    @InjectMockKs
    lateinit var service: ResourceConverterService

    @Test
    fun `should convert successfully when metamodel exists and mapping works`() {
        val (domainName, packageName, resourceName) = Triple("utdanning", "vurdering", "elevfravar")
        val descriptor = EntityDescriptor(domainName, packageName, resourceName)
        val sourceData = mapOf("some" to "data")
        val expectedResource = ElevfravarResource()

        val resourceSpec = mockk<Resource> { every { resourceClass } returns ElevfravarResource::class.java }

        every { metamodelService.getResource(domainName, packageName, resourceName) } returns resourceSpec
        every { objectMapper.convertValue(sourceData, ElevfravarResource::class.java) } returns expectedResource

        val result = service.convertToFintResource(descriptor, sourceData)

        assertEquals(expectedResource, result)

        verify(exactly = 1) { objectMapper.convertValue(sourceData, ElevfravarResource::class.java) }
    }

    @Test
    fun `should throw ResourceConversionException when metamodel is missing`() {
        val descriptor = EntityDescriptor("utdanning", "vurdering", "unknown")

        every { metamodelService.getResource(any(), any(), any()) } returns null

        val exception = assertThrows<ResourceConversionException> {
            service.convertToFintResource(descriptor, mapOf("data" to "value"))
        }

        assertEquals("Resource conversion failed: Metamodel not found for unknown", exception.message)
    }

    @Test
    fun `should throw ResourceConversionException when Jackson conversion fails`() {
        val descriptor = EntityDescriptor("utdanning", "vurdering", "elevfravar")
        val sourceData = mapOf("bad" to "data")

        val resourceSpec = mockk<Resource> {
            every { resourceClass } returns ElevfravarResource::class.java
        }

        every {
            metamodelService.getResource("utdanning", "vurdering", "elevfravar")
        } returns resourceSpec

        every {
            objectMapper.convertValue(sourceData, ElevfravarResource::class.java)
        } throws IllegalArgumentException("Jackson parsing error")

        val exception = assertThrows<ResourceConversionException> {
            service.convertToFintResource(descriptor, sourceData)
        }

        assertEquals("Resource conversion failed: Jackson parsing error", exception.message)
    }
}