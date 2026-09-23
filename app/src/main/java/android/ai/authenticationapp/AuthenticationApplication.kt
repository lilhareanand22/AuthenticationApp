package android.ai.authenticationapp

import android.app.Application
import android.ai.authenticationapp.di.AuthContainer
import androidx.lifecycle.ProcessLifecycleOwner

class AuthenticationApplication : Application() {

    lateinit var authContainer: AuthContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Instantiate the manual dependency injection graph component
        authContainer = AuthContainer(this)
        
        // Register lifecycle observer for automatic background app locking
        ProcessLifecycleOwner.get().lifecycle.addObserver(authContainer.appLifecycleLocker)
    }
}
