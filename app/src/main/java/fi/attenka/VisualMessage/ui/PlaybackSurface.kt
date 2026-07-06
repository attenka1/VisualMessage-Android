package fi.attenka.VisualMessage.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.FrameKind
import fi.attenka.VisualMessage.model.SlideDirection
import fi.attenka.VisualMessage.model.SlideImageBehavior
import fi.attenka.VisualMessage.model.SlideItem
import fi.attenka.VisualMessage.model.TransitionStyle
import fi.attenka.VisualMessage.model.TransmissionFrame
import fi.attenka.VisualMessage.model.TransmissionMode
import fi.attenka.VisualMessage.model.TransmissionSettings
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun PlaybackSurface(
    frame: TransmissionFrame,
    settings: TransmissionSettings,
    progressText: String,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = settings.slideDirection.layoutDirectionOr(LocalLayoutDirection.current)
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = modifier.fillMaxSize().background(backgroundColor(frame, settings))) {
        if (settings.mode == TransmissionMode.VISUAL &&
            settings.transitionStyle.isContinuousSlide &&
            frame.kind is FrameKind.SlideMessage
        ) {
            FrameContent(frame, settings)
        } else {
            AnimatedContent(
                targetState = frame,
                transitionSpec = {
                    // Morse must switch instantly; a fade would smear the short white flashes
                    // over the gaps, making letter/word spacing impossible to read.
                    // Static image frames stay centered; slide transitions would move them off-screen.
                    val style = when {
                        settings.mode != TransmissionMode.VISUAL -> TransitionStyle.INSTANT
                        initialState.kind is FrameKind.Image || targetState.kind is FrameKind.Image ->
                            TransitionStyle.INSTANT
                        else -> settings.transitionStyle
                    }
                    transitionFor(style, layoutDirection)
                },
                label = "frame",
            ) { current ->
                FrameContent(current, settings)
            }
        }

        Text(
            text = progressText,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
        )

        morseLetter(frame)?.let { letter ->
            Text(
                text = letter.uppercase(),
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
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
            Text(
                text = kind.value,
                color = colorForText(kind.value, settings, kind.color),
                fontSize = settings.playbackSingleCharacterFontSizeSp(maxWidth.value, maxHeight.value).sp,
                fontFamily = settings.messageFontFamily.composeFontFamily(),
                fontStyle = settings.messageFontStyle.composeFontStyle(),
                fontWeight = settings.messageFontStyle.composeFontWeight(),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }

        is FrameKind.Whitespace -> Box(Modifier.fillMaxSize())

        is FrameKind.SlideMessage -> SlideMessageContent(kind, frame.durationSeconds, settings)

        is FrameKind.Image -> ImageFrameContent(kind.uri, settings)

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

        is FrameKind.MorseSignal -> {
            if (settings.morseOutputMode.usesScreen) {
                Box(Modifier.fillMaxSize().background(Color.White))
            } else {
                Box(Modifier.fillMaxSize())
            }
        }

        is FrameKind.MorseLetterGap -> Box(Modifier.fillMaxSize())

        FrameKind.Blank -> Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun ImageFrameContent(uri: String, settings: TransmissionSettings) {
    val context = LocalContext.current
    val bitmap = remember(uri) { loadMessageImageBitmap(context, uri) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(settings.playbackImageFillFraction())
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun SlideMessageContent(
    kind: FrameKind.SlideMessage,
    durationSeconds: Double,
    settings: TransmissionSettings,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val side = minOf(maxWidth.value, maxHeight.value)
        val isVerticalSlide = settings.transitionStyle == TransitionStyle.SLIDE_VERTICAL
        val baseFraction = if (isVerticalSlide) 0.46f else 0.62f
        val fontSize = settings.playbackSlideFontSizeSp(side, baseFraction)
        val textStyle = MaterialTheme.typography.displayLarge.copy(
            color = settings.activeTheme.foreground,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 0.96f).sp,
            fontFamily = settings.messageFontFamily.composeFontFamily(),
            fontStyle = settings.messageFontStyle.composeFontStyle(),
            fontWeight = settings.messageFontStyle.composeFontWeight(),
            textAlign = TextAlign.Center,
        )
        val context = LocalContext.current
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val containerWidth = with(density) { maxWidth.toPx() }
        val containerHeight = with(density) { maxHeight.toPx() }
        val slideItems = when (settings.slideImageBehavior) {
            SlideImageBehavior.STATIC_BETWEEN_TEXT -> kind.items.filterIsInstance<SlideItem.Text>()
            SlideImageBehavior.SLIDE_WITH_TEXT -> kind.items
        }
        val text = if (isVerticalSlide) verticalSlideText(slideItems) else null
        val textLayout = text?.let {
            textMeasurer.measure(
                text = it,
                style = textStyle,
                softWrap = false,
            )
        }
        val slideElements = if (isVerticalSlide) {
            emptyList()
        } else {
            slideElements(
                context = context,
                items = slideItems,
                textStyle = textStyle,
                textMeasurer = textMeasurer,
                imageHeightPx = with(density) { (fontSize * 0.95f).sp.toPx() },
                imageGapPx = with(density) { 14.dp.toPx() },
                settings = settings,
            )
        }
        val contentWidth = textLayout?.size?.width?.toFloat() ?: slideElements.sumOf { it.width.toDouble() }.toFloat()
        val contentHeight = textLayout?.size?.height?.toFloat() ?: slideElements.maxOfOrNull { it.height } ?: 0f
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val startOffset = when {
            isVerticalSlide && isRtl -> -contentHeight
            isVerticalSlide -> containerHeight
            isRtl -> -contentWidth
            else -> containerWidth
        }
        val targetOffset = when {
            isVerticalSlide && isRtl -> containerHeight
            isVerticalSlide -> -contentHeight
            isRtl -> containerWidth
            else -> -contentWidth
        }
        val animationKey = slideItems.joinToString("|") {
            when (it) {
                is SlideItem.Text -> it.value
                is SlideItem.Image -> it.uri
            }
        }
        val playbackKey = "$animationKey#${kind.index}"
        val animatedOffset = remember(playbackKey, startOffset, layoutDirection) { Animatable(startOffset) }
        LaunchedEffect(playbackKey, startOffset, targetOffset, durationSeconds, layoutDirection, isVerticalSlide) {
            val animationSpec = tween<Float>(
                durationMillis = (durationSeconds * 1000).toInt().coerceAtLeast(220),
                easing = LinearEasing,
            )
            if (settings.repeatForever &&
                settings.transitionStyle == TransitionStyle.SLIDE &&
                settings.slideImageBehavior == SlideImageBehavior.SLIDE_WITH_TEXT
            ) {
                while (isActive) {
                    animatedOffset.snapTo(startOffset)
                    animatedOffset.animateTo(
                        targetValue = targetOffset,
                        animationSpec = animationSpec,
                    )
                }
            } else {
                animatedOffset.snapTo(startOffset)
                animatedOffset.animateTo(
                    targetValue = targetOffset,
                    animationSpec = animationSpec,
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (textLayout != null) {
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        x = (size.width - contentWidth) / 2f,
                        y = animatedOffset.value,
                    ),
                )
            } else {
                var cursor = animatedOffset.value
                slideElements.forEach { element ->
                    val y = (size.height - element.height) / 2f
                    when (element) {
                        is SlideDrawable.Text -> drawText(
                            textLayoutResult = element.layout,
                            topLeft = Offset(cursor, y),
                        )
                        is SlideDrawable.Image -> drawImage(
                            image = element.bitmap,
                            dstOffset = IntOffset((cursor + element.leadingGap).roundToInt(), y.roundToInt()),
                            dstSize = IntSize(element.imageWidth.roundToInt(), element.height.roundToInt()),
                        )
                    }
                    cursor += element.width
                }
            }
        }
    }
}

private sealed interface SlideDrawable {
    val width: Float
    val height: Float

    data class Text(val layout: androidx.compose.ui.text.TextLayoutResult) : SlideDrawable {
        override val width: Float = layout.size.width.toFloat()
        override val height: Float = layout.size.height.toFloat()
    }

    data class Image(
        val bitmap: ImageBitmap,
        val imageWidth: Float,
        val leadingGap: Float,
        override val width: Float,
        override val height: Float,
    ) : SlideDrawable
}

private fun slideElements(
    context: android.content.Context,
    items: List<SlideItem>,
    textStyle: androidx.compose.ui.text.TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    imageHeightPx: Float,
    imageGapPx: Float,
    settings: TransmissionSettings,
): List<SlideDrawable> =
    items.mapNotNull { item ->
        when (item) {
            is SlideItem.Text -> SlideDrawable.Text(
                textMeasurer.measure(
                    text = item.value,
                    style = textStyle.copy(color = colorForText(item.value, settings, item.color)),
                    softWrap = false,
                )
            )
            is SlideItem.Image -> {
                val bitmap = loadMessageImageBitmap(context, item.uri) ?: return@mapNotNull null
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                val imageWidth = imageHeightPx * aspect
                SlideDrawable.Image(
                    bitmap = bitmap,
                    imageWidth = imageWidth,
                    leadingGap = imageGapPx,
                    width = imageWidth + (imageGapPx * 2f),
                    height = imageHeightPx,
                )
            }
        }
    }

private fun verticalSlideText(items: List<SlideItem>): String {
    val words = mutableListOf<List<String>>()
    val currentWord = mutableListOf<String>()

    fun flushWord() {
        if (currentWord.isNotEmpty()) {
            words += currentWord.toList()
            currentWord.clear()
        }
    }

    items.filterIsInstance<SlideItem.Text>().forEach { item ->
        val character = item.value
        if (character.isBlank()) {
            flushWord()
        } else {
            currentWord += character
        }
    }
    flushWord()

    return words.joinToString(separator = "\n\n") { word ->
        word.joinToString(separator = "\n")
    }
}

private fun backgroundColor(frame: TransmissionFrame, settings: TransmissionSettings): Color =
    when (frame.kind) {
        is FrameKind.MorseSignal -> if (settings.morseOutputMode.usesScreen) Color.White else Color.Black
        is FrameKind.MorseLetterGap -> Color.Black
        FrameKind.AppLogo -> Color.Black
        FrameKind.Blank -> if (settings.mode == TransmissionMode.MORSE) Color.Black else settings.activeTheme.background
        is FrameKind.Character,
        is FrameKind.Whitespace,
        is FrameKind.Image,
        is FrameKind.SlideMessage,
        -> settings.activeTheme.background
    }

private fun morseLetter(frame: TransmissionFrame): String? =
    when (val kind = frame.kind) {
        is FrameKind.MorseSignal -> kind.letter
        is FrameKind.MorseLetterGap -> kind.letter
        else -> null
    }

private val multicolorPalette = listOf(
    Color(0xFFFF3B30),
    Color(0xFFFF9500),
    Color(0xFFFFCC00),
    Color(0xFF34C759),
    Color(0xFF007AFF),
    Color(0xFFAF52DE),
)

private fun colorForText(value: String, settings: TransmissionSettings, manualColor: Color? = null): Color {
    if (manualColor != null) {
        return manualColor
    }
    if (!settings.multicolorLettersEnabled || value.isBlank()) {
        return settings.activeTheme.foreground
    }
    val codePoint = value.codePointAt(0)
    return multicolorPalette[kotlin.math.abs(codePoint) % multicolorPalette.size]
}

private fun transitionFor(style: TransitionStyle, layoutDirection: LayoutDirection) = when (style) {
    TransitionStyle.INSTANT -> (fadeIn(tween(0)) togetherWith fadeOut(tween(0)))
    TransitionStyle.FADE -> (fadeIn(tween(180)) togetherWith fadeOut(tween(180)))
    TransitionStyle.SLIDE -> {
        val direction = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        (slideInHorizontally(tween(220)) { direction * it } + fadeIn(tween(220))) togetherWith
            (slideOutHorizontally(tween(220)) { -direction * it } + fadeOut(tween(220)))
    }
    TransitionStyle.SLIDE_VERTICAL -> {
        val direction = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        (slideInVertically(tween(220)) { direction * it } + fadeIn(tween(220))) togetherWith
            (slideOutVertically(tween(220)) { -direction * it } + fadeOut(tween(220)))
    }
    TransitionStyle.SCALE ->
        (scaleIn(tween(220), initialScale = 0.82f) + fadeIn(tween(220))) togetherWith
            (scaleOut(tween(220), targetScale = 0.82f) + fadeOut(tween(220)))
}

private fun SlideDirection.layoutDirectionOr(fallback: LayoutDirection): LayoutDirection =
    when (isRightToLeft) {
        true -> LayoutDirection.Rtl
        false -> LayoutDirection.Ltr
        null -> fallback
    }
