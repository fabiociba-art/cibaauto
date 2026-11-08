package com.example.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator

/**
 * Serviço principal do Android Auto.
 * Declarado no AndroidManifest — o Android Auto / DHU inicia esta sessão.
 */
class CibaAutoCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Aceita hosts de desenvolvimento e produção Google
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        return CibaAutoCarSession()
    }

    override fun onCreateSession(): Session {
        return CibaAutoCarSession()
    }
}
