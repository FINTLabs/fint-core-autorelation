package no.fintlabs.autorelation

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import no.fint.model.resource.FintResource
import no.fintlabs.autorelation.cache.RelationRuleRegistry
import no.fintlabs.autorelation.kafka.RelationUpdateProducer
import no.fintlabs.autorelation.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutoRelationService(
    private val relationRuleRegistry: RelationRuleRegistry,
    private val resourceConverter: ResourceConverterService,
    private val relationUpdateProducer: RelationUpdateProducer,
    private val metricService: MetricService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun processRequest(event: RelationEvent): Int {
        val resource = runCatching { event.toFintResource() }
            .getOrElse { return handleFailure(it, event) }

        val rules = relationRuleRegistry.getRules(event.sourceEntity)

        return coroutineScope {
            rules
                .mapNotNull { rule -> tryCreateRelationUpdate(rule, event, resource) }
                .map { update ->
                    async { publishWithSuspend(update, event) }
                }
                .awaitAll()
                .count { it }
        }
    }

    private fun tryCreateRelationUpdate(
        rule: RelationSyncRule,
        event: RelationEvent,
        resource: FintResource
    ): RelationUpdate? =
        runCatching { rule.toRelationUpdate(event, resource) }
            .getOrElse {
                handleFailure(it, event)
                null
            }

    private suspend fun publishWithSuspend(update: RelationUpdate, event: RelationEvent): Boolean {
        return try {
            relationUpdateProducer.publishRelationUpdate(update).await()
            metricService.incrementRelationSuccess(update.targetEntity.resourceName)
            true
        } catch (e: Exception) {
            handleFailure(
                KafkaPublishException("Failed to publish update to target ${update.targetId}", e),
                event
            )
            false
        }
    }

    private fun handleFailure(exception: Throwable, event: RelationEvent): Int {
        val (metricReason, logAction) = when (exception) {
            is KafkaPublishException -> exception.metricReason to { msg: String ->
                logger.error("Infrastructure Error: $msg", exception)
            }
            is AutoRelationException -> exception.metricReason to { msg: String ->
                logger.warn("Skipping: $msg")
            }
            else -> MetricReason.UNEXPECTED_ERROR to { msg: String ->
                logger.error("Unexpected error: $msg", exception)
            }
        }

        logAction(exception.message ?: "Unknown error")

        metricService.incrementRelationFailure(
            sourceId = event.sourceId,
            resourceName = event.sourceEntity.resourceName,
            reason = metricReason
        )

        return 0
    }

    private fun RelationEvent.toFintResource() =
        resourceConverter.convertToFintResource(sourceEntity, sourceData)
}