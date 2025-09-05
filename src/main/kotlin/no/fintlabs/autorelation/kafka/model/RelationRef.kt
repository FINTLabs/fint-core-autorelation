package no.fintlabs.autorelation.kafka.model

data class RelationRef(
    val name: String,
    val ids: List<ResourceId>
)
