package android.ai.authenticationapp

import android.app.Application
import android.ai.authenticationapp.di.AuthContainer

class AuthenticationApplication : Application() {

    lateinit var authContainer: AuthContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Instantiate the manual dependency injection graph component
        authContainer = AuthContainer(this)
    }
}
