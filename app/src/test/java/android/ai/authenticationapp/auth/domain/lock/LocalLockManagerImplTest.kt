package android.ai.authenticationapp.auth.domain.lock

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalLockManagerImplTest {

    @Test
    fun `initial state is Unlocked`() {
        val lockManager = LocalLockManagerImpl()
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }

    @Test
    fun `lock changes state to Locked`() {
        val lockManager = LocalLockManagerImpl()
        lockManager.lock()
        assertEquals(LocalUnlockState.Locked, lockManager.state.value)
    }

    @Test
    fun `unlock changes state to Unlocked`() {
        val lockManager = LocalLockManagerImpl()
        lockManager.lock()
        lockManager.unlock()
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }

    @Test
    fun `repeated lock and unlock calls are safe`() {
        val lockManager = LocalLockManagerImpl()
        
        // Repeated locks
        lockManager.lock()
        lockManager.lock()
        assertEquals(LocalUnlockState.Locked, lockManager.state.value)
        
        // Repeated unlocks
        lockManager.unlock()
        lockManager.unlock()
        assertEquals(LocalUnlockState.Unlocked, lockManager.state.value)
    }
}
