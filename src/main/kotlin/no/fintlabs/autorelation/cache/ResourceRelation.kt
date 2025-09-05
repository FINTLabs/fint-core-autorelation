package no.fintlabs.autorelation.cache

class ResourceRelation(
    val relation: String,
    componentResource: String
) {
    val componentResource: String = componentResource.lowercase()
}