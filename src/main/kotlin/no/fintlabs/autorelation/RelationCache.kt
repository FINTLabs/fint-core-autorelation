package no.fintlabs.autorelation

import no.fint.model.FintMultiplicity
import no.fint.model.FintRelation
import no.fintlabs.metamodel.MetamodelService
import no.fintlabs.metamodel.model.Resource
import org.springframework.stereotype.Repository

@Repository
class RelationCache(
    private val metamodelService: MetamodelService
) {

    val cache: Map<String, List<ResourceRelation>> by lazy { buildCache() }

    private fun buildCache(): Map<String, List<ResourceRelation>> =
        metamodelService.getComponents()
            .flatMap { component ->
                component.resources.flatMap { resource ->
                    resource.relations
                        .filter { isOneOrNoneToMany(it.multiplicity) }
                        .mapNotNull { relation ->
                            buildResourceRelations(component.name, relation, resource.packageName)
                        }
                }
            }
            .toList()
            .groupBy { it.componentResource }

    private fun buildResourceRelations(
        component: String,
        relation: FintRelation,
        resourcePackage: String
    ): ResourceRelation? =
        formatComponentResource(component, relation.packageName)
            .let { componentResource ->
                getRelationResource(componentResource)
                    ?.takeIf { referencesBack(it, resourcePackage) }
                    ?.let { ResourceRelation(relation.name, componentResource) }
            }

    private fun getRelationResource(componentName: String): Resource? =
        componentName.split("-").takeLast(3).let { (domain, pkg, resource) ->
            metamodelService.getResource(domain, pkg, resource)
        }

    private fun referencesBack(relationResource: Resource, resourcePackageName: String) =
        relationResource.relations.any { it.packageName == resourcePackageName }

    private fun isOneOrNoneToMany(multiplicity: FintMultiplicity) =
        multiplicity in setOf(FintMultiplicity.ONE_TO_MANY, FintMultiplicity.NONE_TO_MANY)

    private fun formatComponentResource(component: String, packageName: String): String =
        if (isCommon(packageName))
            "${component}-${getResourceName(packageName)}"
        else getComponentResource(packageName)

    private fun getResourceName(packageName: String): String =
        packageName.split(".").last()

    private fun getComponentResource(packageName: String): String =
        packageName.split(".")
            .takeLast(3)
            .joinToString("-")

    private fun isCommon(packageName: String) =
        packageName.startsWith("no.fint.model.felles")

}