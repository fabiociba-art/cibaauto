package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
  var startAnimation by remember { mutableStateOf(false) }
  val scale = animateFloatAsState(
      targetValue = if (startAnimation) 1.0f else 0.8f,
      animationSpec = tween(
          durationMillis = 1000,
          easing = FastOutSlowInEasing
      ),
      label = "splash_scale"
  )

  LaunchedEffect(key1 = true) {
    startAnimation = true
    delay(2200) // 2.2 seconds splash duration
    onSplashFinished()
  }

  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack),
      contentAlignment = Alignment.Center
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp).scale(scale.value)
    ) {
      // App Icon / Logo Image
      Surface(
          shape = RoundedCornerShape(24.dp),
          color = CockpitSurface,
          shadowElevation = 12.dp,
          modifier = Modifier.size(160.dp)
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
          Image(
              painter = painterResource(id = R.drawable.ciba_icon),
              contentDescription = "CIBA AUTO Logo",
              modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
          text = "CIBA AUTO",
          color = ElectricBlue,
          fontSize = 28.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 4.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
          text = "DIAGNÓSTICO AUTOMOTIVO & TELEMETRIA",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
      )

      Spacer(modifier = Modifier.height(40.dp))

      LinearProgressIndicator(
          modifier = Modifier
              .width(180.dp)
              .height(4.dp)
              .clip(CircleShape),
          color = ElectricBlue,
          trackColor = CockpitSurfaceVariant
      )
    }
  }
}
