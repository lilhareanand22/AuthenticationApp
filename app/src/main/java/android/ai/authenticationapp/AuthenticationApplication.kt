package android.ai.authenticationapp

import android.ai.authenticationapp.auth.domain.lock.AppLifecycleLocker
import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Top-level Application class.
 * Annotated with [@HiltAndroidApp] to trigger Hilt's code generation and root dependency component.
 */
@HiltAndroidApp
class AuthenticationApplication : Application() {

    @Inject
    lateinit var appLifecycleLocker: AppLifecycleLocker

    override fun onCreate() {
        super.onCreate()
        
        // Register lifecycle observer for automatic background app locking
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleLocker)
    }
}
