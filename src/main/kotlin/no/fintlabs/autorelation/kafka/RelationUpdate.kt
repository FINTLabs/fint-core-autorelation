package no.fintlabs.autorelation.kafka

import no.fintlabs.autorelation.model.RelationRef
import no.fintlabs.autorelation.model.ResourceRef

data class RelationUpdate(
    val orgId: String,
    val domainName: String,
    val packageName: String,
    val resource: ResourceRef,
    val relation: RelationRef
)