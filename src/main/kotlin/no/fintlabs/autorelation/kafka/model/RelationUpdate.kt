package no.fintlabs.autorelation.kafka.model

data class RelationUpdate(
    val orgId: String,
    val domainName: String,
    val packageName: String,
    val resource: ResourceRef,
    val relation: RelationRef
)