package com.example.sparkchaindemo.ui.widget

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun RippleEffect(
    modifier: Modifier,
    size: Dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    @Composable
    fun animateScaleWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000 + delay,
//                easing = LinearOutSlowInEasing
            ),
        ),
        label = ""
    )

    @Composable
    fun animateAlphaWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000 + delay,
//                easing = LinearOutSlowInEasing
            ),
        ),
        label = ""
    )
    Box(
        modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier
                .scale(animateScaleWithDelay(1).value)
                .alpha(animateAlphaWithDelay(1).value)
                .clip(shape = CircleShape)
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xffC0D7FF),
                            Color(0xffCFBBFF),
                        )
                    )
                ),
        ) {}
        Box(
            modifier
                .scale(animateScaleWithDelay(500).value)
                .alpha(animateAlphaWithDelay(500).value)
                .clip(shape = CircleShape)
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xffC0D7FF),
                            Color(0xffCFBBFF),
                        )
                    )
                ),
        ) {}
    }
}