package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CIBA AUTO", appName)
  }

  @Test
  fun `test vehicle profile displacement and tank capacity calculations`() {
    val profile = com.example.model.VehicleProfile(
        id = "test_1",
        brand = "Toyota",
        model = "Corolla",
        version = "Altis",
        year = "2025",
        engine = "2.0 Dynamic Force",
        displacement = "1.987 cm³",
        fuelType = "Flex",
        horsepower = "175 cv",
        torque = "21.3 kgfm",
        transmission = "CVT",
        drivetrain = "FWD",
        tankCapacity = "50 litros",
        isHybrid = false
    )

    assertEquals(1.987f, profile.getDisplacementLiters(), 0.001f)
    assertEquals(50f, profile.getTankCapacityLiters(), 0.1f)
  }
}
