package io.viascom.github.action.maintenance.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.viascom.github.action.maintenance.api.WorkflowRunStatusDeserializer
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GsonConfig {
    @Bean
    open fun gson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(WorkflowRunStatus::class.java, WorkflowRunStatusDeserializer())
            .create()
    }
}
