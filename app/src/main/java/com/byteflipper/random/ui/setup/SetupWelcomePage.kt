package com.byteflipper.random.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.ui.theme.ExpTitleTypography

@Composable
fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp),
            text = stringResource(R.string.setup_welcome_title),
            style = ExpTitleTypography.displayLarge.copy(
                fontSize = 42.sp,
                lineHeight = 1.1.em
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        SetupWelcomeArtwork()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.setup_welcome_description),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SetupWelcomeArtwork() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        MaterialYouVectorDrawable(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.logo)
        )
        SineWaveLine(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(32.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
            animate = true,
            color = MaterialTheme.colorScheme.surface,
            alpha = 0.95f,
            strokeWidth = 16.dp,
            amplitude = 4.dp,
            waves = 7.6f,
            phase = 0f
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(22.dp)
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp)
        )
        SineWaveLine(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(32.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
            animate = true,
            color = MaterialTheme.colorScheme.primary,
            alpha = 0.95f,
            strokeWidth = 4.dp,
            amplitude = 4.dp,
            waves = 7.6f,
            phase = 0f
        )
    }
}
