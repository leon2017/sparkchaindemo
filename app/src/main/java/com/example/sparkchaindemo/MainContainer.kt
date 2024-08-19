package com.example.sparkchaindemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.sparkchaindemo.ui.widget.RippleEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainContainer(
    modifier: Modifier,
    videModel: MainViewModel = viewModel()
) {

    val audioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    if (!audioPermissionState.status.isGranted) {
        LaunchedEffect(key1 = Unit) {
            audioPermissionState.launchPermissionRequest()
        }
    }

    val state = videModel.uiState.collectAsState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(visible = state.value.ttsStatus == TtsStatus.TTS) {
                RippleEffect(
                    modifier = Modifier,
                    size = 220.dp
                )
            }
            Image(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                painter = painterResource(id = R.mipmap.icon_avatar),
                contentDescription = "avatar"
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = state.value.asrText)
        Spacer(modifier = Modifier.height(10.dp))
        Image(
            modifier = Modifier
                .size(65.dp)
                .clickable {
                    videModel.asrToggle()
                },
            imageVector = ImageVector.vectorResource(id = if (state.value.ttsStatus != TtsStatus.IDLE)
                R.drawable.baseline_pause_circle_24 else R.drawable.baseline_play_circle_filled_24),
            contentDescription = "logo"
        )
        Text(text = if (state.value.ttsStatus != TtsStatus.IDLE) "挂断" else "开始")
        Spacer(modifier = Modifier.height(100.dp))
    }

}