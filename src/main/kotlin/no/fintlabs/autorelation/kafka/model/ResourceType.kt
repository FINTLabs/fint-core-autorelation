package no.fintlabs.autorelation.kafka.model

data class ResourceType(
    val domain: String,
    val pkg: String,
    val resource: String
) {
    val key: String = "$domain-$pkg-$resource".lowercase()

    companion object {
        fun parse(key: String): ResourceType =
            key.split("-", limit = 3).let { (domain, pkg, resource) ->
                ResourceType(domain, pkg, resource)
            }
    }
}