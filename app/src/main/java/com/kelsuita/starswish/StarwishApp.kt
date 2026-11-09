package com.kelsuita.starswish

import android.app.Application
import com.kelsuita.starswish.data.Languages

/** Loads the packaged language files before any Activity, service or store can look up copy. */
class StarwishApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Languages.attach(this)
    }
}
