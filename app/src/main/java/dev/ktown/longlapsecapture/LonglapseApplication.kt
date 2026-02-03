package dev.ktown.longlapsecapture

import android.app.Application
import dev.ktown.longlapsecapture.di.ServiceLocator

class LonglapseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
