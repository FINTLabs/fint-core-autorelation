package no.fintlabs.autorelation

import kotlinx.coroutines.test.runTest
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.FintResource
import no.fint.model.resource.Link
import no.fint.model.resource.utdanning.vurdering.ElevfravarResource
import no.fintlabs.autorelation.model.EntityDescriptor
import no.fintlabs.autorelation.model.RelationEvent
import no.fintlabs.autorelation.model.RelationOperation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import kotlin.test.assertEquals

@SpringBootTest
@EmbeddedKafka
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutoRelationServiceTest {

    @Autowired
    private lateinit var autoRelation: AutoRelationService

    @MockitoSpyBean
    private lateinit var metricService: MetricService

    @BeforeEach
    fun resetMocks() {
        reset(metricService)
    }

    @CsvSource(
        "321, https://api.felleskomponent.no/utdanning/fravarsregistrering/systemid/123",
        "123, fravarsregistrering/systemid/123",
        "213, /systemid/123",
        "213, systemid/123",
    )
    @ParameterizedTest(name = "successful event processed: {0}")
    fun `successful event scenarios`(sourceId: String, href: String) = runTest {
        val event = createElevfravarEvent(
            sourceId = sourceId,
            href = href
        )

        val publishedRelationUpdates = autoRelation.processRequest(event)

        assertEquals(1, publishedRelationUpdates)
        verify(metricService, never()).incrementRelationFailure(any(), any(), any())
    }

    @CsvSource(
        "systemid123", // only idValue, idField is required
        "/84293",
        "NULL",
        "''",
        nullValues = ["NULL"]
    )
    @ParameterizedTest(name = "required link that is invalid throws exception and increments metric: {0}")
    fun `invalid link scenarios`(href: String?) = runTest {
        val event = createElevfravarEvent(
            sourceId = "123",
            href = href
        )

        val publishedRelationUpdates = autoRelation.processRequest(event)

        assertEquals(0, publishedRelationUpdates)
        verify(metricService, times(1)).incrementRelationFailure(
            any(),
            any(),
            any()
        )
    }

    private fun createElevfravarEvent(
        sourceId: String,
        href: String?,
        systemId: String = sourceId,
    ) = createRelationEvent(
        domainName = "utdanning",
        packageName = "vurdering",
        resourceName = "elevfravar",
        sourceId = sourceId,
        resource = ElevfravarResource().apply {
            this.systemId = Identifikator().apply {
                identifikatorverdi = systemId
            }
            addFravarsregistrering(Link.with(href))
        },
    )

    private fun createRelationEvent(
        domainName: String,
        packageName: String,
        resourceName: String,
        resource: FintResource,
        sourceId: String,
        operation: RelationOperation = RelationOperation.ADD
    ) = RelationEvent(
        sourceEntity = EntityDescriptor(
            domainName = domainName,
            packageName = packageName,
            resourceName = resourceName
        ),
        orgId = "fintlabs.no",
        sourceData = resource,
        sourceId = sourceId,
        operation = operation
    )
}