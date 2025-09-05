package no.fintlabs.autorelation.kafka.model

import no.fintlabs.autorelation.cache.RelationSpec

data class RelationUpdate(
    val orgId: String,
    val domainName: String,
    val packageName: String,
    val resource: ResourceRef,
    val relation: RelationRef
) {
    companion object {
        fun from(orgId: String, relationSpec: RelationSpec, resourceId: ResourceId, relationIds: List<ResourceId>) =
            RelationUpdate(
                orgId = orgId,
                domainName = relationSpec.resourceType.domain,
                packageName = relationSpec.resourceType.pkg,
                resource = ResourceRef(relationSpec.resourceType.resource, resourceId),
                relation = RelationRef(relationSpec.inversedRelation.name, relationIds)
            )
    }
}