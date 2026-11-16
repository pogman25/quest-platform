package ru.pogman.questplatform.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import ru.pogman.questplatform.R

@Composable
fun QrScanner(active: Boolean, busy: Boolean, onToken: (String) -> Unit, onFailure: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var requested by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
        }
    val handlePermission = {
        requested = true
        permission.launch(Manifest.permission.CAMERA)
    }
    val handleSettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            )
        )
    }
    val handleRetry: () -> Unit = { retry++ }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME)
                granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(active, granted) { if (active && !granted && !requested) handlePermission() }
    if (!granted) {
        Text(stringResource(R.string.qr_permission_hint))
        Button(onClick = handlePermission) { Text(stringResource(R.string.qr_permission_action)) }
        TextButton(onClick = handleSettings) { Text(stringResource(R.string.app_settings_action)) }
    } else {
        QrPreview(active, busy, retry, onToken, onFailure)
        Text(stringResource(R.string.qr_scan_hint))
        TextButton(onClick = handleRetry, enabled = !busy) {
            Text(stringResource(R.string.scan_again_action))
        }
    }
}

@Composable
private fun QrPreview(
    active: Boolean,
    busy: Boolean,
    retry: Int,
    onToken: (String) -> Unit,
    onFailure: () -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val view =
        remember(context) {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }
    val controller =
        remember(context) {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    val onTokenUpdated by rememberUpdatedState(onToken)
    val onFailureUpdated by rememberUpdatedState(onFailure)
    val accepting by rememberUpdatedState(active && !busy)

    DisposableEffect(active, owner, controller, retry) {
        if (!active) onDispose {}
        else {
            val scanner =
                BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )
            var closed = false
            var lastToken: String? = null
            var reportedError = false
            val analyzer =
                MlKitAnalyzer(
                    listOf(scanner),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    executor,
                ) { result ->
                    if (!closed) {
                        val error = result.getThrowable(scanner)
                        if (error != null && !reportedError) {
                            reportedError = true
                            onFailureUpdated()
                        }
                        val raw =
                            result
                                .getValue(scanner)
                                ?.firstOrNull { !it.rawValue.isNullOrBlank() }
                                ?.rawValue
                        if (raw != null && accepting && raw != lastToken) {
                            lastToken = raw
                            onTokenUpdated(raw)
                        }
                    }
                }
            try {
                view.controller = controller
                controller.setImageAnalysisAnalyzer(executor, analyzer)
                controller.bindToLifecycle(owner)
                controller.initializationFuture.addListener(
                    {
                        if (!closed)
                            try {
                                controller.initializationFuture.get()
                            } catch (_: Exception) {
                                onFailureUpdated()
                            }
                    },
                    executor,
                )
            } catch (_: RuntimeException) {
                reportedError = true
                onFailureUpdated()
            }
            onDispose {
                closed = true
                controller.clearImageAnalysisAnalyzer()
                controller.unbind()
                view.controller = null
                scanner.close()
            }
        }
    }
    AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f))
}
