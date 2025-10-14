package no.fintlabs.autorelation.kafka

import io.mockk.mockk
import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.metamodel.MetamodelService
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EntityConsumerTest {

    private lateinit var metamodelService: MetamodelService
    private lateinit var autoRelation: AutoRelationService
    private lateinit var consumer: EntityConsumer

    private val headers = RecordHeaders()

    @BeforeEach
    fun setUp() {
        metamodelService = mockk(relaxed = true)
        autoRelation = mockk(relaxed = true)
        consumer = EntityConsumer(metamodelService, autoRelation)
    }

    @Test
    fun `process non-null values`() {
        assertTrue(consumer.shouldBeProcessed("not null", headers))
    }

    @Test
    fun `dont process null values`() {
        assertFalse(consumer.shouldBeProcessed(null, headers))
    }

}