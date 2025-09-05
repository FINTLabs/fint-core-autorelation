package no.fintlabs.autorelation.cache

import no.fint.model.FintRelation
import no.fintlabs.autorelation.kafka.model.ResourceType

class RelationSpec(
    val resourceRelation: FintRelation,
    val inversedRelation: FintRelation,
    val resourceType: ResourceType
) {
    companion object {
        fun from(resourceRelation: FintRelation, inversedRelation: FintRelation, componentResource: String) =
            RelationSpec(
                resourceRelation,
                inversedRelation,
                ResourceType.parse(componentResource)
            )
    }
}