package org.example.common

import org.junit.jupiter.api.BeforeEach
import org.mockito.MockitoAnnotations

open class BaseUnitTest {
    
    private var autoCloseable: AutoCloseable? = null
    
    @BeforeEach
    open fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }
    
    open fun tearDown() {
        autoCloseable?.close()
    }
}
