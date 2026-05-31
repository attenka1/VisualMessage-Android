package fi.attenka.VisualMessage.receive

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.attenka.VisualMessage.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiveScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ReceiveViewModel = viewModel()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraLayer(viewModel)
            AimBox()
        } else {
            PermissionPrompt(onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }

        Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            TopBar(onClose = onClose, onClear = viewModel::clear)
            CameraOptionsBar(
                preferHighFrameRate = viewModel.preferHighFrameRate,
                onPreferHighFrameRateChange = viewModel::updatePreferHighFrameRate,
            )
        }
        DecodedPanel(state = viewModel.state, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalCamera2Interop::class)
@Composable
private fun CameraLayer(viewModel: ReceiveViewModel) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(viewModel.preferHighFrameRate) {
        val provider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        cameraProvider = provider

        val fpsRange = MorseCameraConfigurator.selectFpsRange(context, viewModel.preferHighFrameRate)
        val preview = MorseCameraConfigurator.buildPreview(fpsRange).apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = MorseCameraConfigurator.buildAnalysis(fpsRange).apply {
            setAnalyzer(ContextCompat.getMainExecutor(context), viewModel.analyzer)
        }

        val owner = context.findLifecycleOwner()
        if (owner != null) {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            launch {
                runCatching { MorseCameraConfigurator.applyManualExposure(camera) }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CameraOptionsBar(
    preferHighFrameRate: Boolean,
    onPreferHighFrameRateChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.high_frame_rate),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.manual_exposure_hint),
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = preferHighFrameRate,
            onCheckedChange = onPreferHighFrameRateChange,
        )
    }
}

@Composable
private fun AimBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(LuminanceAnalyzer.ROI_FRACTION)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
        )
    }
}

@Composable
private fun TopBar(onClose: () -> Unit, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReceiveBarButton(onClick = onClose, label = "\u2190  " + stringResource(R.string.close))
        Text(
            stringResource(R.string.receiver_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        ReceiveBarButton(onClick = onClear, label = stringResource(R.string.clear))
    }
}

@Composable
private fun ReceiveBarButton(onClick: () -> Unit, label: String) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 88.dp, minHeight = 52.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DecodedPanel(state: MorseReceiverState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (state.lightOn) Color(0xFF4CD964) else Color.White.copy(alpha = 0.25f)),
            )
            Text(
                if (state.partial.isEmpty()) stringResource(R.string.waiting_for_signal) else state.partial,
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val decoded = state.text.ifBlank { stringResource(R.string.point_camera) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                decoded,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.camera_permission_needed),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onGrant) { Text(stringResource(R.string.grant_permission)) }
    }
}

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
