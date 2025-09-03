package no.fintlabs.autorelation

import no.fintlabs.metamodel.MetamodelGateway
import org.springframework.stereotype.Service

@Service
class FintContext(
    private val metamodelGateway: MetamodelGateway
) {

    // TODO: Make Component object contain resources?
    fun getComponentResourcePairs(): List<Pair<String, Set<String>>> =
        metamodelGateway.getComponents().map {
            it.name to metamodelGateway.getResourceNames(it.domainName, it.packageName!!)
        }

}