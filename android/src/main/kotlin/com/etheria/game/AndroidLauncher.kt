package com.etheria.game

import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useWakelock = true
            useCompass = false
            useAccelerometer = false
            numSamples = 2
            r = 8; g = 8; b = 8; a = 8
            depth = 16
        }

        initialize(EtheriaGame(), config)
    }
}
