package no.fintlabs.autorelation

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.fintlabs.autorelation.model.ResourceType
import no.fintlabs.metamodel.MetamodelService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class ResourceMapperServiceTest {

    @MockK lateinit var metamodelService: MetamodelService
    @MockK lateinit var objectMapper: ObjectMapper
    val resourceType = mockk<ResourceType> {
        every { domain } returns "utdanning"
        every { pkg } returns "vurdering"
        every { resource } returns "elevfravar"
    }

    @InjectMockKs
    lateinit var service: ResourceMapperService

    @Test
    fun `returns null when metamodel returns null`() {
        every { metamodelService.getResource("utdanning", "vurdering", "elevfravar") } returns null

        val result = service.mapResource(resourceType, mapOf("systemId" to mapOf("identifikatorverdi" to "1")))

        assertNull(result)
    }

}
