package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager

/**
 * Quantos itens uma lista pode ter no carro neste momento.
 * Em movimento o Android Auto costuma permitir só 6; passar disso derruba a tela.
 */
fun carListLimit(carContext: CarContext): Int {
    return try {
        carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
            .coerceAtLeast(2)
    } catch (e: Exception) {
        6
    }
}
