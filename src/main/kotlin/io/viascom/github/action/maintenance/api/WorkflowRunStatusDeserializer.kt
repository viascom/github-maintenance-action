package io.viascom.github.action.maintenance.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.viascom.github.action.maintenance.exception.WorkflowRunStatusDeserializationException
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import java.lang.reflect.Type

class WorkflowRunStatusDeserializer : JsonDeserializer<WorkflowRunStatus> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): WorkflowRunStatus {
        val value = json.asString.trim().uppercase()
        return WorkflowRunStatus.entries.find { it.name == value }
            ?: throw WorkflowRunStatusDeserializationException("Unknown WorkflowRunStatus value: '$value'")
    }
}
