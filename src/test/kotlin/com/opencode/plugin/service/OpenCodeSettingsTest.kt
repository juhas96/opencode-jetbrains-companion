package com.opencode.plugin.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class OpenCodeSettingsTest {
    
    @Nested
    inner class StateDefaultsTests {
        
        @Test
        fun `State has correct default serverUrl`() {
            val state = OpenCodeSettings.State()
            
            assertEquals("http://localhost:4096", state.serverUrl)
        }
        
        @Test
        fun `State has empty default sessionId`() {
            val state = OpenCodeSettings.State()
            
            assertEquals("", state.sessionId)
        }
        
        @Test
        fun `State has autoDetectSession enabled by default`() {
            val state = OpenCodeSettings.State()
            
            assertTrue(state.autoDetectSession)
        }
        
        @Test
        fun `State has correct default username`() {
            val state = OpenCodeSettings.State()
            
            assertEquals("opencode", state.username)
        }
        
        @Test
        fun `State has empty default password`() {
            val state = OpenCodeSettings.State()
            
            assertEquals("", state.password)
        }
        
        @Test
        fun `State has useTuiMode enabled by default`() {
            val state = OpenCodeSettings.State()
            
            assertTrue(state.useTuiMode)
        }
        
        @Test
        fun `State has autoSubmitTui disabled by default`() {
            val state = OpenCodeSettings.State()
            
            assertFalse(state.autoSubmitTui)
        }
        
        @Test
        fun `State has empty default agent`() {
            val state = OpenCodeSettings.State()
            
            assertEquals("", state.defaultAgent)
        }
    }
    
    @Nested
    inner class StateMutabilityTests {
        
        private lateinit var state: OpenCodeSettings.State
        
        @BeforeEach
        fun setup() {
            state = OpenCodeSettings.State()
        }
        
        @Test
        fun `serverUrl can be changed`() {
            state.serverUrl = "http://localhost:5000"
            
            assertEquals("http://localhost:5000", state.serverUrl)
        }
        
        @Test
        fun `sessionId can be changed`() {
            state.sessionId = "ses_abc123"
            
            assertEquals("ses_abc123", state.sessionId)
        }
        
        @Test
        fun `autoDetectSession can be disabled`() {
            state.autoDetectSession = false
            
            assertFalse(state.autoDetectSession)
        }
        
        @Test
        fun `username can be changed`() {
            state.username = "custom_user"
            
            assertEquals("custom_user", state.username)
        }
        
        @Test
        fun `password can be set`() {
            state.password = "secret123"
            
            assertEquals("secret123", state.password)
        }
        
        @Test
        fun `useTuiMode can be disabled`() {
            state.useTuiMode = false
            
            assertFalse(state.useTuiMode)
        }
        
        @Test
        fun `autoSubmitTui can be enabled`() {
            state.autoSubmitTui = true
            
            assertTrue(state.autoSubmitTui)
        }
        
        @Test
        fun `defaultAgent can be set`() {
            state.defaultAgent = "oracle"
            
            assertEquals("oracle", state.defaultAgent)
        }
    }
    
    @Nested
    inner class StateEqualityTests {
        
        @Test
        fun `two default States are equal`() {
            val state1 = OpenCodeSettings.State()
            val state2 = OpenCodeSettings.State()
            
            assertEquals(state1, state2)
        }
        
        @Test
        fun `States with same values are equal`() {
            val state1 = OpenCodeSettings.State(
                serverUrl = "http://custom:8080",
                sessionId = "ses_123",
                autoDetectSession = false,
                username = "user",
                password = "pass",
                useTuiMode = false,
                autoSubmitTui = true,
                defaultAgent = "oracle"
            )
            val state2 = OpenCodeSettings.State(
                serverUrl = "http://custom:8080",
                sessionId = "ses_123",
                autoDetectSession = false,
                username = "user",
                password = "pass",
                useTuiMode = false,
                autoSubmitTui = true,
                defaultAgent = "oracle"
            )
            
            assertEquals(state1, state2)
        }
        
        @Test
        fun `State copy preserves values`() {
            val original = OpenCodeSettings.State(
                serverUrl = "http://custom:8080",
                defaultAgent = "explore"
            )
            
            val copy = original.copy()
            
            assertEquals(original.serverUrl, copy.serverUrl)
            assertEquals(original.defaultAgent, copy.defaultAgent)
        }
        
        @Test
        fun `State copy can override values`() {
            val original = OpenCodeSettings.State(defaultAgent = "oracle")
            
            val modified = original.copy(defaultAgent = "explore")
            
            assertEquals("oracle", original.defaultAgent)
            assertEquals("explore", modified.defaultAgent)
        }
    }
    
    @Nested
    inner class ServerUrlValidationTests {
        
        @Test
        fun `accepts localhost URL`() {
            val state = OpenCodeSettings.State(serverUrl = "http://localhost:4096")
            
            assertEquals("http://localhost:4096", state.serverUrl)
        }
        
        @Test
        fun `accepts IP address URL`() {
            val state = OpenCodeSettings.State(serverUrl = "http://192.168.1.100:4096")
            
            assertEquals("http://192.168.1.100:4096", state.serverUrl)
        }
        
        @Test
        fun `accepts URL with different port`() {
            val state = OpenCodeSettings.State(serverUrl = "http://localhost:8080")
            
            assertEquals("http://localhost:8080", state.serverUrl)
        }
        
        @Test
        fun `accepts https URL`() {
            val state = OpenCodeSettings.State(serverUrl = "https://opencode.example.com:443")
            
            assertEquals("https://opencode.example.com:443", state.serverUrl)
        }
    }
}
