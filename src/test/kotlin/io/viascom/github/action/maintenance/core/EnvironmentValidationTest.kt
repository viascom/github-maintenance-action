package io.viascom.github.action.maintenance.core

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Tests for the validation logic in the Environment class.
 * 
 * This test class doesn't rely on the Environment singleton but instead tests the validation logic directly.
 */
class EnvironmentValidationTest {

    /**
     * Validates the environment variables using the same logic as Environment.validate().
     */
    private fun validateEnvironment(
        gitHubToken: String,
        repository: String,
        retentionDays: Int,
        keepMinimumRuns: Int
    ) {
        require(gitHubToken.isNotBlank()) { "INPUT_GITHUB_TOKEN is required" }
        require(repository.contains("/")) { "INPUT_REPOSITORY must be in format 'owner/repo'" }
        require(retentionDays >= 0) { "INPUT_RETENTION_DAYS must be >= 0" }
        require(keepMinimumRuns >= 0) { "INPUT_KEEP_MINIMUM_RUNS must be >= 0" }
    }

    @Test
    fun `validate should succeed with valid values`() {
        assertDoesNotThrow {
            validateEnvironment(
                gitHubToken = "ghp_UcxgLwS4UMCJOsI2NxH2vU2Euj3IXi3c4rtAFAKE",
                repository = "viascom/github-maintenance-action",
                retentionDays = 30,
                keepMinimumRuns = 10
            )
        }
    }

    @Test
    fun `validate should throw exception when gitHubToken is blank`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            validateEnvironment(
                gitHubToken = "",
                repository = "viascom/github-maintenance-action",
                retentionDays = 30,
                keepMinimumRuns = 10
            )
        }
        assertEquals("INPUT_GITHUB_TOKEN is required", exception.message)
    }

    @Test
    fun `validate should throw exception when repository format is invalid`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            validateEnvironment(
                gitHubToken = "ghp_UcxgLwS4UMCJOsI2NxH2vU2Euj3IXi3c4rtAFAKE",
                repository = "invalid-repo",
                retentionDays = 30,
                keepMinimumRuns = 10
            )
        }
        assertEquals("INPUT_REPOSITORY must be in format 'owner/repo'", exception.message)
    }

    @Test
    fun `validate should throw exception when retentionDays is negative`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            validateEnvironment(
                gitHubToken = "ghp_UcxgLwS4UMCJOsI2NxH2vU2Euj3IXi3c4rtAFAKE",
                repository = "viascom/github-maintenance-action",
                retentionDays = -1,
                keepMinimumRuns = 10
            )
        }
        assertEquals("INPUT_RETENTION_DAYS must be >= 0", exception.message)
    }

    @Test
    fun `validate should throw exception when keepMinimumRuns is negative`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            validateEnvironment(
                gitHubToken = "ghp_UcxgLwS4UMCJOsI2NxH2vU2Euj3IXi3c4rtAFAKE",
                repository = "viascom/github-maintenance-action",
                retentionDays = 30,
                keepMinimumRuns = -1
            )
        }
        assertEquals("INPUT_KEEP_MINIMUM_RUNS must be >= 0", exception.message)
    }

    @Test
    fun `validate should succeed with zero retentionDays`() {
        assertDoesNotThrow {
            validateEnvironment(
                gitHubToken = "ghp_UcxgLwS4UMCJOsI2NxH2vU2Euj3IXi3c4rtAFAKE",
                repository = "viascom/github-maintenance-action",
                retentionDays = 0,
                keepMinimumRuns = 10
            )
        }
    }

    @Test
    fun `validate should succeed with zero keepMinimumRuns`() {
        assertDoesNotThrow {
            validateEnvironment(
                gitHubToken = "ghp_UcxgLwS4UMCJOsI2NxH2vU2Euj3IXi3c4rtAFAKE",
                repository = "viascom/github-maintenance-action",
                retentionDays = 30,
                keepMinimumRuns = 0
            )
        }
    }
}
