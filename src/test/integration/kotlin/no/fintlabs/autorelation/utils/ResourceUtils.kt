package no.fintlabs.autorelation.utils

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.Link
import no.fint.model.resource.utdanning.vurdering.ElevfravarResource
import no.fint.model.resource.utdanning.vurdering.FravarsregistreringResource

fun createElevfravarResource(id: String = "123", vararg elevforholdIds: String) =
    ElevfravarResource().apply {
        systemId = Identifikator().apply {
            identifikatorverdi = id
        }

        for (elevforholdId in elevforholdIds) {
            addElevforhold(Link.with("systemid/$elevforholdId"))
        }
    }

fun createFravarsregistreringResource(resourceId: String = "123", vararg elevfravarIds: String) =
    FravarsregistreringResource().apply {
        systemId = Identifikator().apply {
            identifikatorverdi = resourceId
        }

        for (elevfravarId in elevfravarIds) {
            addElevfravar(Link.with("systemid/$elevfravarId"))
        }
    }