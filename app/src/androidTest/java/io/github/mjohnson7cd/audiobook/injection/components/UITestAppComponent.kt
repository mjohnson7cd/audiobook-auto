package io.github.mjohnson7cd.audiobook.injection.components

import dagger.Component
import io.github.mjohnson7cd.audiobook.injection.modules.UITestAppModule
import javax.inject.Singleton

@Component(modules = [UITestAppModule::class])
@Singleton
interface UITestAppComponent : AppComponent {
    // Test injection methods can be added here as needed
}
