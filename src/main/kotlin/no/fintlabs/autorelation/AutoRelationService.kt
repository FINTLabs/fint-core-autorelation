package no.fintlabs.autorelation

import jakarta.annotation.PostConstruct
import no.fint.model.FintMultiplicity
import org.springframework.stereotype.Service

@Service
class AutoRelationService {

    fun processEntity(resource: Any, resourceName: String): Unit = TODO()

    @PostConstruct
    fun resourceShouldBeProcessed() {
        val component = "utdanning.vurdering"
        val resource = ""

        // If relation thats added has resource that's going to be updated, proceed
        // Map the object to an actual Resource
        // Map the idfields to an object

        // If resource has NONE_TO_MANY or ONE_TO_MANY relation, we will update it
        // add resource that should be updated to Map<relationThatsAdded, List<FutureUpdatedResources>>

//        FintMultiplicity.ONE_TO_ONE,
//        FintMultiplicity.ONE_TO_MANY,
//        FintMultiplicity.NONE_TO_ONE,
//        FintMultiplicity.NONE_TO_MANY;
    }

}