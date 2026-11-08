package com.example.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import com.example.BuildConfig

/**
 * Serviço principal do Android Auto.
 * Declarado no AndroidManifest — o Android Auto / DHU inicia esta sessão.
 */
class CibaAutoCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        return CibaAutoCarSession()
    }

    override fun onCreateSession(): Session {
        return CibaAutoCarSession()
    }
}
