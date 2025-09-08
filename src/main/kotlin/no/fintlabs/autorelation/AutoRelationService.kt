package no.fintlabs.autorelation

import no.fint.model.resource.FintLinks
import no.fint.model.resource.FintResource
import no.fint.model.resource.Link
import no.fintlabs.autorelation.cache.RelationCache
import no.fintlabs.autorelation.cache.RelationSpec
import no.fintlabs.autorelation.kafka.RelationUpdateEventProducer
import no.fintlabs.autorelation.kafka.RelationUpdateMapper
import no.fintlabs.autorelation.kafka.model.RelationOperation
import no.fintlabs.autorelation.kafka.model.RelationRequest
import no.fintlabs.autorelation.kafka.model.RelationUpdate
import no.fintlabs.autorelation.kafka.model.ResourceId
import org.springframework.stereotype.Service

@Service
class AutoRelationService(
    private val mapper: RelationUpdateMapper,
    private val relationCache: RelationCache,
    private val resourceMapper: ResourceMapperService,
    private val eventPublisher: RelationUpdateEventProducer
) {

    fun processRequest(relationRequest: RelationRequest) =
        relationCache.getRelationSpecs(relationRequest.type)?.let { relationSpecs ->
            resourceMapper.mapResource(relationRequest.type, relationRequest.resource)
                ?.let { parseRelationSpecs(relationRequest.orgId, relationSpecs, it) }
        }

    private fun parseRelationSpecs(orgId: String, relationSpecs: List<RelationSpec>, resourceObject: FintResource) =
        relationSpecs.forEach { relationSpec ->
            getRelationLink(resourceObject, relationSpec.resourceRelation.name)
                ?.let(::createResourceIdFromLink)
                ?.let { resourceId ->
                    buildRelationUpdate(orgId, relationSpec, resourceObject, resourceId)
                        ?.let { eventPublisher.publishRelationUpdate(it) }
                }
        }

    private fun buildRelationUpdate(
        orgId: String,
        relationSpec: RelationSpec,
        resourceObject: FintResource,
        resourceId: ResourceId
    ): RelationUpdate? =
        createRelationIds(resourceObject)
            ?.let { relationIds ->
                mapper.map(orgId, RelationOperation.ADD, relationSpec, resourceId, relationIds)
            }

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