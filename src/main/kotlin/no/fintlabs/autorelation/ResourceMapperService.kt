package no.fintlabs.autorelation

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.model.resource.FintResource
import no.fintlabs.autorelation.model.ResourceType
import no.fintlabs.metamodel.MetamodelService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ResourceMapperService(
    private val metamodelService: MetamodelService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun mapResource(resourceType: ResourceType, resourceObject: Any): FintResource? =
        metamodelService.getResource(resourceType.domain, resourceType.pkg, resourceType.resource)
            ?.let { mapResource(resourceObject, it.resourceType) }
            ?.onFailure { logger.error("Failed mapping resource: ${resourceType.resource}: ${it.message}") }
            ?.getOrNull()

    private fun mapResource(resourceObject: Any, resourceType: Class<out FintResource>): Result<FintResource> =
        runCatching { objectMapper.convertValue(resourceObject, resourceType) }
            .onFailure { logger.error(it.message) }

}