package no.fintlabs.autorelation

class ResourceRelation(
    val relation: String,
    componentResource: String
) {
    val componentResource: String = componentResource.lowercase()
}