package no.fintlabs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FintCoreAutorelationApplication

fun main(args: Array<String>) {
    runApplication<FintCoreAutorelationApplication>(*args)
}
