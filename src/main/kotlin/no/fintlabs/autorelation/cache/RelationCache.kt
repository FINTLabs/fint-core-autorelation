package no.fintlabs.autorelation.cache

import no.fintlabs.autorelation.kafka.model.ResourceType
import org.springframework.stereotype.Repository

@Repository
class RelationCache(
    relationSpecBuilder: RelationSpecBuilder
) {

    private val cache: Map<ResourceType, List<RelationSpec>> =
        relationSpecBuilder.buildResourceTypeToRelationSpecs()

    fun getRelationSpecs(resourceType: ResourceType): List<RelationSpec>? = cache[resourceType]

}