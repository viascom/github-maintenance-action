package io.viascom.github.action.maintenance.util

import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UtilTest {

    @Test
    fun `fromCommaSeparatedValues should convert comma-separated string to enum list`() {
        // Given
        val input = "COMPLETED,FAILURE,SUCCESS"

        // When
        val result = input.fromCommaSeparatedValues<WorkflowRunStatus>()

        // Then
        assertEquals(3, result.size)
        assertEquals(WorkflowRunStatus.COMPLETED, result[0])
        assertEquals(WorkflowRunStatus.FAILURE, result[1])
        assertEquals(WorkflowRunStatus.SUCCESS, result[2])
    }

    @Test
    fun `fromCommaSeparatedValues should handle spaces in input`() {
        // Given
        val input = "COMPLETED, FAILURE, SUCCESS"

        // When
        val result = input.fromCommaSeparatedValues<WorkflowRunStatus>()

        // Then
        assertEquals(3, result.size)
        assertEquals(WorkflowRunStatus.COMPLETED, result[0])
        assertEquals(WorkflowRunStatus.FAILURE, result[1])
        assertEquals(WorkflowRunStatus.SUCCESS, result[2])
    }

    @Test
    fun `fromCommaSeparatedValues should ignore invalid enum values`() {
        // Given
        val input = "COMPLETED,INVALID_VALUE,SUCCESS"

        // When
        val result = input.fromCommaSeparatedValues<WorkflowRunStatus>()

        // Then
        assertEquals(2, result.size)
        assertEquals(WorkflowRunStatus.COMPLETED, result[0])
        assertEquals(WorkflowRunStatus.SUCCESS, result[1])
    }

    @Test
    fun `fromCommaSeparatedValues should handle empty string`() {
        // Given
        val input = ""

        // When
        val result = input.fromCommaSeparatedValues<WorkflowRunStatus>()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `fromCommaSeparatedValues should handle blank string`() {
        // Given
        val input = "   "

        // When
        val result = input.fromCommaSeparatedValues<WorkflowRunStatus>()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `splitCommaList should split comma-separated string to string list`() {
        // Given
        val input = "value1,value2,value3"

        // When
        val result = input.splitCommaList()

        // Then
        assertEquals(3, result.size)
        assertEquals("value1", result[0])
        assertEquals("value2", result[1])
        assertEquals("value3", result[2])
    }

    @Test
    fun `splitCommaList should handle spaces in input`() {
        // Given
        val input = "value1, value2, value3"

        // When
        val result = input.splitCommaList()

        // Then
        assertEquals(3, result.size)
        assertEquals("value1", result[0])
        assertEquals("value2", result[1])
        assertEquals("value3", result[2])
    }

    @Test
    fun `splitCommaList should handle empty string`() {
        // Given
        val input = ""

        // When
        val result = input.splitCommaList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `splitCommaList should handle blank string`() {
        // Given
        val input = "   "

        // When
        val result = input.splitCommaList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `splitCommaList should handle null input`() {
        // Given
        val input: String? = null

        // When
        val result = input.splitCommaList()

        // Then
        assertEquals(0, result.size)
    }
}
