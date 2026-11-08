package com.example.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class CibaAutoCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return CibaAutoCarScreen(carContext)
    }
}
