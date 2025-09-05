package no.fintlabs.autorelation.kafka.model

data class ResourceType(
    val domain: String,
    val pkg: String,
    val resource: String
) {
    companion object {
        fun of(domain: String, pkg: String, resource: String): ResourceType =
            ResourceType(
                domain.lowercase(),
                pkg.lowercase(),
                resource.lowercase()
            )

        fun parse(key: String): ResourceType =
            key.split("-", limit = 3).let { (domain, pkg, resource) -> of(domain, pkg, resource) }
    }
}