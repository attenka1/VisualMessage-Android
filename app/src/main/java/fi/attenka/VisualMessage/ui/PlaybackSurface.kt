package fi.attenka.VisualMessage.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.FrameKind
import fi.attenka.VisualMessage.model.TransitionStyle
import fi.attenka.VisualMessage.model.TransmissionFrame
import fi.attenka.VisualMessage.model.TransmissionMode
import fi.attenka.VisualMessage.model.TransmissionSettings

@Composable
fun PlaybackSurface(
    frame: TransmissionFrame,
    settings: TransmissionSettings,
    progressText: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(backgroundColor(frame, settings))) {
        AnimatedContent(
            targetState = frame,
            transitionSpec = {
                // Morse must switch instantly; a fade would smear the short white flashes
                // over the gaps, making letter/word spacing impossible to read.
                val style = if (settings.mode == TransmissionMode.VISUAL) settings.transitionStyle else TransitionStyle.INSTANT
                transitionFor(style)
            },
            label = "frame",
        ) { current ->
            FrameContent(current, settings)
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = progressText,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onStop) {
                Text(stringResource(R.string.stop))
            }
        }
    }
}

@Composable
private fun FrameContent(frame: TransmissionFrame, settings: TransmissionSettings) {
    when (val kind = frame.kind) {
        is FrameKind.Character -> BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxWidth.value, maxHeight.value)
            Text(
                text = kind.value,
                color = settings.activeTheme.foreground,
                fontSize = (side * 0.78f).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }

        FrameKind.AppLogo -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.launch_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.72f)
                    .clip(RoundedCornerShape(24.dp)),
            )
        }

        FrameKind.MorseSignal -> Box(Modifier.fillMaxSize().background(Color.White))

        FrameKind.Blank -> Box(Modifier.fillMaxSize())
    }
}

private fun backgroundColor(frame: TransmissionFrame, settings: TransmissionSettings): Color =
    when (frame.kind) {
        FrameKind.MorseSignal -> Color.White
        FrameKind.AppLogo -> Color.Black
        FrameKind.Blank -> if (settings.mode == TransmissionMode.MORSE) Color.Black else settings.activeTheme.background
        is FrameKind.Character -> settings.activeTheme.background
    }

private fun transitionFor(style: TransitionStyle) = when (style) {
    TransitionStyle.INSTANT -> (fadeIn(tween(0)) togetherWith fadeOut(tween(0)))
    TransitionStyle.FADE -> (fadeIn(tween(180)) togetherWith fadeOut(tween(180)))
    TransitionStyle.SLIDE ->
        (slideInHorizontally(tween(220)) { it } + fadeIn(tween(220))) togetherWith
            (slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(220)))
    TransitionStyle.SCALE ->
        (scaleIn(tween(220), initialScale = 0.82f) + fadeIn(tween(220))) togetherWith
            (scaleOut(tween(220), targetScale = 0.82f) + fadeOut(tween(220)))
}
