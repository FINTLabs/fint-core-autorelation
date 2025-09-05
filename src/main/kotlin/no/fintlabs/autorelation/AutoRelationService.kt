package no.fintlabs.autorelation

import no.fint.model.resource.FintLinks
import no.fint.model.resource.FintResource
import no.fint.model.resource.Link
import no.fintlabs.autorelation.cache.RelationCache
import no.fintlabs.autorelation.cache.RelationSpec
import no.fintlabs.autorelation.kafka.model.RelationUpdate
import no.fintlabs.autorelation.kafka.model.ResourceId
import no.fintlabs.autorelation.kafka.model.ResourceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutoRelationService(
    private val relationCache: RelationCache,
    private val resourceMapper: ResourceMapperService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun processEntity(orgId: String, resourceType: ResourceType, resourceObject: Any) =
        relationCache.getRelationSpecs(resourceType)?.let { relationSpecs ->
            resourceMapper.mapResource(resourceType, resourceObject)
                ?.let { parseRelationSpecs(orgId, relationSpecs, it) }
        }

    fun parseRelationSpecs(orgId: String, relationSpecs: List<RelationSpec>, resourceObject: FintResource) =
        relationSpecs.forEach { relationSpec ->
            getRelationLink(resourceObject, relationSpec.resourceRelation.name)
                ?.let(::createResourceIdFromLink)
                ?.let { resourceId ->
                    buildRelationUpdate(orgId, relationSpec, resourceObject, resourceId)
                }
        }

    private fun buildRelationUpdate(
        orgId: String,
        relationSpec: RelationSpec,
        resourceObject: FintResource,
        resourceId: ResourceId
    ): RelationUpdate? =
        createRelationIds(resourceObject)
            ?.let { relationIds -> RelationUpdate.from(orgId, relationSpec, resourceId, relationIds) }

    // TODO: Log required relations missing?
    private fun getRelationLink(fintLinks: FintLinks, relationName: String): Link? =
        fintLinks.links?.get(relationName)?.firstOrNull()

    private fun createRelationIds(resourceObject: FintResource): List<ResourceId>? =
        resourceObject.identifikators
            .filterValues { it != null }
            .map { (idField, identifikator) ->
                ResourceId(idField, identifikator!!.identifikatorverdi)
            }
            .takeIf { it.isNotEmpty() }

    private fun createResourceIdFromLink(link: Link) =
        getIdPair(link.href).let { (idField, idValue) -> ResourceId(idField, idValue) }

    private fun getIdPair(href: String) =
        href.split("/").takeLast(2)

}