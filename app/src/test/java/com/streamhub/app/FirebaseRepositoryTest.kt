package com.streamhub.app

import com.streamhub.app.data.repository.AdminOperationState
import com.streamhub.app.data.repository.CatalogState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseRepositoryTest {

    @Test
    fun catalogState_typesAreDistinct() {
        val loading: CatalogState = CatalogState.Loading
        val ready: CatalogState = CatalogState.Ready
        val error: CatalogState = CatalogState.Error("Network failure")

        assertTrue(loading is CatalogState.Loading)
        assertTrue(ready is CatalogState.Ready)
        assertTrue(error is CatalogState.Error)
        assertEquals("Network failure", (error as CatalogState.Error).message)
    }

    @Test
    fun adminOperationState_typesAreDistinct() {
        val idle: AdminOperationState = AdminOperationState.Idle
        val loading: AdminOperationState = AdminOperationState.Loading
        val success: AdminOperationState = AdminOperationState.Success(12345L)
        val error: AdminOperationState = AdminOperationState.Error("Write failed")

        assertTrue(idle is AdminOperationState.Idle)
        assertTrue(loading is AdminOperationState.Loading)
        assertTrue(success is AdminOperationState.Success)
        assertEquals(12345L, (success as AdminOperationState.Success).timestamp)
        assertTrue(error is AdminOperationState.Error)
        assertEquals("Write failed", (error as AdminOperationState.Error).message)
    }
}
