package no.fintlabs.autorelation

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.model.resource.FintResource
import no.fintlabs.autorelation.model.EntityDescriptor
import no.fintlabs.autorelation.model.ResourceConversionException
import no.fintlabs.metamodel.MetamodelService
import org.springframework.stereotype.Service

@Service
class ResourceConverterService(
    private val metamodelService: MetamodelService,
    private val objectMapper: ObjectMapper
) {

    fun convertToFintResource(sourceEntity: EntityDescriptor, sourceData: Any): FintResource {
        val resourceClass = sourceEntity.getResourceClass()
            ?: throw ResourceConversionException("Metamodel not found for ${sourceEntity.resourceName}")

        return runCatching { objectMapper.convertValue(sourceData, resourceClass) }
            .getOrElse { error -> throw ResourceConversionException(error.message) }
    }

    private fun EntityDescriptor.getResourceClass() =
        metamodelService.getResource(domainName, packageName, resourceName)
            ?.resourceClass

}