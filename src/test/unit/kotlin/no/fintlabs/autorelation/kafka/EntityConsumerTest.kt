package no.fintlabs.autorelation.kafka

import io.mockk.mockk
import no.fintlabs.autorelation.AutoRelationService
import no.fintlabs.autorelation.kafka.mapper.RelationRequestMapper
import no.fintlabs.metamodel.MetamodelService
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EntityConsumerTest {

    private lateinit var metamodelService: MetamodelService
    private lateinit var autoRelation: AutoRelationService
    private lateinit var relationRequestMapper: RelationRequestMapper
    private lateinit var consumer: EntityConsumer

    @BeforeEach
    fun setUp() {
        relationRequestMapper = mockk(relaxed = true)
        metamodelService = mockk(relaxed = true)
        autoRelation = mockk(relaxed = true)
        consumer = EntityConsumer(metamodelService, autoRelation, relationRequestMapper)
    }

    @Test
    fun `process non-null values and no consumer header`() {
        assertTrue(consumer.shouldBeProcessed("im not null", createHeaders(addConsumerHeader = false)))
    }

    @Test
    fun `dont process null values`() {
        assertFalse(consumer.shouldBeProcessed(null, createHeaders(addConsumerHeader = false)))
    }

    @Test
    fun `dont process consumer headers`() {
        assertFalse(consumer.shouldBeProcessed("not null", createHeaders(addConsumerHeader = true)))
    }

    private fun createHeaders(addConsumerHeader: Boolean = false) =
        RecordHeaders().apply {
            if (addConsumerHeader)
                add("consumer", byteArrayOf())
        }

}