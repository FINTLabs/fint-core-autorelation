package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.cache.RelationSpec
import no.fintlabs.autorelation.kafka.model.*
import org.springframework.stereotype.Component

@Component
class RelationUpdateMapper {

    fun map(
        orgId: String,
        operation: RelationOperation,
        relationSpec: RelationSpec,
        resourceId: ResourceId,
        relationIds: List<ResourceId>
    ): RelationUpdate =
        RelationUpdate(
            orgId = orgId,
            operation = operation,
            domainName = relationSpec.resourceType.domain,
            packageName = relationSpec.resourceType.pkg,
            resource = ResourceRef(
                name = relationSpec.resourceType.resource,
                id = resourceId
            ),
            relation = RelationRef(
                name = relationSpec.inversedRelation.name,
                ids = relationIds
            )
        )

}