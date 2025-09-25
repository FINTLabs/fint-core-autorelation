package no.fintlabs.autorelation.kafka.mapper

import no.fintlabs.autorelation.model.RelationRef
import no.fintlabs.autorelation.model.RelationRequest
import no.fintlabs.autorelation.model.RelationSpec
import no.fintlabs.autorelation.model.RelationUpdate
import no.fintlabs.autorelation.model.ResourceId
import no.fintlabs.autorelation.model.ResourceRef
import org.springframework.stereotype.Component

@Component
class RelationUpdateMapper {

    fun map(
        request: RelationRequest,
        relationSpec: RelationSpec,
        resourceId: ResourceId,
        relationIds: List<ResourceId>
    ): RelationUpdate =
        RelationUpdate(
            orgId = request.orgId,
            operation = request.operation,
            domainName = relationSpec.resourceType.domain,
            packageName = relationSpec.resourceType.pkg,
            resource = ResourceRef(
                name = relationSpec.resourceType.resource,
                id = resourceId
            ),
            relation = RelationRef(
                name = relationSpec.inversedRelation.name,
                ids = relationIds
            ),
            entityRetentionTime = request.entityRetentionTime
        )

}