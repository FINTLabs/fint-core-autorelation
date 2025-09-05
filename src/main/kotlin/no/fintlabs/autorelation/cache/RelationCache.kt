package no.fintlabs.autorelation.cache

import no.fintlabs.autorelation.cache.ResourceRelation
import org.springframework.stereotype.Repository

@Repository
class RelationCache(
    resourceRelationBuilder: ResourceRelationBuilder
) {

    private val cache: Map<String, List<ResourceRelation>> =
        resourceRelationBuilder.buildComponentResourcePair()

    fun keyExists(componentResource: String): Boolean =
        cache.containsKey(componentResource)

    fun getResourceRelations(componentResource: String) =
        cache.getOrDefault(componentResource, emptyList())

}