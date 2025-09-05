package no.fintlabs.autorelation.cache

import no.fint.model.FintMultiplicity
import no.fint.model.FintRelation
import no.fintlabs.metamodel.MetamodelService
import no.fintlabs.metamodel.model.Resource
import org.springframework.stereotype.Component

@Component
class ResourceRelationBuilder(
    private val metamodelService: MetamodelService
) {

    // TODO: Move some of the Fint logic into metamodel?

    fun buildComponentResourcePair(): Map<String, List<ResourceRelation>> =
        metamodelService.getComponents()
            .flatMap { component ->
                component.resources.flatMap { resource ->
                    resource.relations
                        .filter { isOneOrNoneToMany(it.multiplicity) }
                        .mapNotNull { relation ->
                            buildComponentResourcePair(component.name, relation, resource.packageName)
                        }
                }
            }
            .groupBy(
                keySelector = { (componentResource, _) -> componentResource },
                valueTransform = { (_, resourceRelations) -> resourceRelations }
            )

    private fun buildComponentResourcePair(
        component: String,
        relation: FintRelation,
        resourcePackage: String
    ): Pair<String, ResourceRelation>? =
        formatComponentResource(component, relation.packageName)
            .let { componentResource ->
                getRelationResource(componentResource)
                    ?.let { findFirstBackReference(it, resourcePackage) }
                    ?.let { componentResource to createResourceRelation(it) }
            }

    private fun formatComponentResource(component: String, packageName: String): String =
        if (isCommon(packageName))
            "${component}-${getResourceName(packageName)}"
        else getComponentResource(packageName)

    private fun isOneOrNoneToMany(multiplicity: FintMultiplicity) =
        multiplicity in setOf(FintMultiplicity.ONE_TO_MANY, FintMultiplicity.NONE_TO_MANY)

    private fun getRelationResource(componentName: String): Resource? =
        componentName.split("-").takeLast(3).let { (domain, pkg, resource) ->
            metamodelService.getResource(domain, pkg, resource)
        }

    private fun findFirstBackReference(relationResource: Resource, resourcePackageName: String) =
        relationResource.relations.firstOrNull { it.packageName == resourcePackageName }

    private fun createResourceRelation(relation: FintRelation) =
        ResourceRelation(
            relation = relation.name,
            multiplicity = relation.multiplicity,
            componentResource = getComponentResource(relation.packageName)
        )

    private fun getResourceName(packageName: String): String =
        packageName.split(".").last()

    private fun getComponentResource(packageName: String): String =
        packageName.split(".")
            .takeLast(3)
            .joinToString("-")

    private fun isCommon(packageName: String) =
        packageName.startsWith("no.fint.model.felles")

}