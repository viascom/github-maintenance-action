package io.viascom.github.action.maintenance.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import java.lang.reflect.Type

class WorkflowRunStatusDeserializer : JsonDeserializer<WorkflowRunStatus> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): WorkflowRunStatus {
        return json.asString.let { value -> WorkflowRunStatus.entries.first { it.name == value.uppercase() } }
    }
}
