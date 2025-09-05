package no.fintlabs.autorelation.cache

import no.fint.model.FintMultiplicity

class ResourceRelation(
    val relation: String,
    val multiplicity: FintMultiplicity,
    componentResource: String
) {
    val componentResource: String = componentResource.lowercase()
}