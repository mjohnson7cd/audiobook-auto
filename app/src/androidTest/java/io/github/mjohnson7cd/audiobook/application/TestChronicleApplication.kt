package io.github.mjohnson7cd.audiobook.application

import io.github.mjohnson7cd.audiobook.injection.components.AppComponent
import io.github.mjohnson7cd.audiobook.injection.components.DaggerUITestAppComponent
import io.github.mjohnson7cd.audiobook.injection.modules.UITestAppModule
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class TestChronicleApplication : ChronicleApplication() {
    override fun initializeComponent(): AppComponent {
        Timber.i("Test chronicle application component")
        return DaggerUITestAppComponent.builder()
            .uITestAppModule(UITestAppModule(applicationContext)).build()
    }
}
