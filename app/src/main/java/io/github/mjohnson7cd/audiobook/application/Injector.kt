package io.github.mjohnson7cd.audiobook.application

import io.github.mjohnson7cd.audiobook.injection.components.AppComponent

class Injector private constructor() {
    companion object {
        fun get(): AppComponent = ChronicleApplication.get().appComponent
    }
}
