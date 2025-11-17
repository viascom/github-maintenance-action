package io.viascom.github.action.maintenance.config

import com.google.gson.*
import io.viascom.github.action.maintenance.api.WorkflowRunStatusDeserializer
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.lang.reflect.Modifier
import java.time.Instant
import java.time.format.DateTimeFormatter

@Configuration
open class GsonConfig {

    @Primary
    @Bean
    open fun gson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(WorkflowRunStatus::class.java, WorkflowRunStatusDeserializer())
            .registerTypeAdapter(Instant::class.java, JsonDeserializer { json, _, _ ->
                Instant.from(DateTimeFormatter.ISO_INSTANT.parse(json.asString))
            })
            .registerTypeAdapter(Instant::class.java, JsonSerializer<Instant> { src, _, _ ->
                JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(src))
            })
            .serializeNulls()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .setPrettyPrinting()
            .create()
    }
}
