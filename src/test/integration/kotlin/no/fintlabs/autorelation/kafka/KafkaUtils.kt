package no.fintlabs.autorelation.kafka

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class KafkaUtils(
    private val kafkaAdmin: KafkaAdmin
) {

    fun resetTopics(vararg topics: String) {
        if (topics.isEmpty()) return

        Admin.create(kafkaAdmin.configurationProperties).use { admin ->
            val existingTopics = admin.listTopics()
                .names()
                .get(TIMEOUT, TimeUnit.MILLISECONDS)
                .filterNot { it.startsWith("__") }

            val toReset = topics.filter { it in existingTopics }

            if (toReset.isNotEmpty()) {
                admin.deleteTopics(toReset).all()
                    .get(TIMEOUT, TimeUnit.MILLISECONDS)
            }

            val newTopics = topics.map {
                NewTopic(it, 1, 1.toShort())
                    .configs(mapOf(TopicConfig.RETENTION_MS_CONFIG to SEVEN_DAYS_MS.toString()))
            }

            admin.createTopics(newTopics).all()
                .get(TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val TIMEOUT = 30_000L
        private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
    }

}