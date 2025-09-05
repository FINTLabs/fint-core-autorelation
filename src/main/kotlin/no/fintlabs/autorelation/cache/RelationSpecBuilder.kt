package no.fintlabs.autorelation.cache

import no.fint.model.FintMultiplicity
import no.fint.model.FintRelation
import no.fintlabs.autorelation.kafka.model.ResourceType
import no.fintlabs.metamodel.MetamodelService
import no.fintlabs.metamodel.model.Resource
import org.springframework.stereotype.Component

@Component
class RelationSpecBuilder(
    private val metamodelService: MetamodelService
) {

    // TODO: Move some of the Fint logic into metamodel?

    fun buildResourceTypeToRelationSpecs(): Map<ResourceType, List<RelationSpec>> =
        metamodelService.getComponents()
            .flatMap { component ->
                component.resources.flatMap { resource ->
                    resource.relations
                        .filter { isOneOrNoneToMany(it.multiplicity) }
                        .mapNotNull { relation ->
                            buildResourceTypeToRelationSpecs(
                                component = component.name,
                                originalRelation = relation,
                                resourcePackage = resource.packageName
                            )
                        }
                }
            }
            .groupBy(
                keySelector = { (resourceType, _) -> resourceType },
                valueTransform = { (_, resourceRelations) -> resourceRelations }
            )

    private fun buildResourceTypeToRelationSpecs(
        component: String,
        originalRelation: FintRelation,
        resourcePackage: String
    ): Pair<ResourceType, RelationSpec>? =
        formatComponentResource(component, originalRelation.packageName)
            .let { componentResource -> ResourceType.parse(componentResource) }
            .let { resourceType ->
                metamodelService.getResource(resourceType.domain, resourceType.pkg, resourceType.resource)
                    ?.let { findFirstBackReference(it, resourcePackage) }
                    ?.let { resourceRelation ->
                        resourceType to createResourceSpec(resourceRelation, originalRelation)
                    }
            }

    private fun createResourceSpec(
        resourceRelation: FintRelation,
        originalRelation: FintRelation
    ) = RelationSpec.from(
        resourceRelation = resourceRelation,
        inversedRelation = originalRelation,
        componentResource = getComponentResource(resourceRelation.packageName)
    )

    fun formatComponentResource(component: String, packageName: String): String =
        if (isCommon(packageName))
            "${component}-${getResourceName(packageName)}"
        else getComponentResource(packageName)

    private fun isOneOrNoneToMany(multiplicity: FintMultiplicity) =
        multiplicity in setOf(FintMultiplicity.ONE_TO_MANY, FintMultiplicity.NONE_TO_MANY)

    private fun findFirstBackReference(relationResource: Resource, resourcePackageName: String) =
        relationResource.relations.firstOrNull { it.packageName == resourcePackageName }

    private fun getResourceName(packageName: String): String =
        packageName.split(".").last()

    private fun getComponentResource(packageName: String): String =
        packageName.split(".")
            .takeLast(3)
            .joinToString("-")

    private fun isCommon(packageName: String) =
        packageName.startsWith("no.fint.model.felles")

}