package no.fintlabs

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka

@SpringBootTest
@EmbeddedKafka(partitions = 1, controlledShutdown = true, count = 2)
class ApplicationTest {

    @Test
    fun contextLoads() {
    }

}