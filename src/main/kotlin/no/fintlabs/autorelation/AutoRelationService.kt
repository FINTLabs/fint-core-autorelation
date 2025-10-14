package no.fintlabs.autorelation

import no.fint.model.resource.FintResource
import no.fintlabs.autorelation.cache.RelationCache
import no.fintlabs.autorelation.kafka.RelationUpdateEntityProducer
import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.autorelation.model.RelationUpdate
import org.springframework.stereotype.Service

@Service
class AutoRelationService(
    private val relationCache: RelationCache,
    private val resourceMapper: ResourceMapperService,
    private val entityProducer: RelationUpdateEntityProducer
) {

    fun processRequest(request: RelationRequest): Int =
        takeIf { relationCache.isTriggerResourceType(request.type) }
            ?.let { resourceMapper.mapResource(request.type, request.resource) }
            ?.let { createRelationUpdates(request, it) }
            ?.let { publishRelationUpdates(it) }
            ?: 0


    private fun createRelationUpdates(request: RelationRequest, resourceObject: FintResource): List<RelationUpdate> =
        relationCache.rulesForTrigger(request.type)
            .mapNotNull { RelationUpdate.from(request, resourceObject, it) }

    private fun publishRelationUpdates(relationUpdates: List<RelationUpdate>): Int =
        relationUpdates
            .onEach { entityProducer.publishRelationUpdate(it) }
            .size

}