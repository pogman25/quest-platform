package ru.pogman.questplatform.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.pogman.questplatform.screens.NoiseMeterScreen
import ru.pogman.questplatform.viewmodel.NoiseMeterViewModel

@Composable
fun NoiseMeterContent(viewModel: NoiseMeterViewModel, active: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var resumed by remember {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
        }
    val handlePermission = { permission.launch(Manifest.permission.RECORD_AUDIO) }
    val handleSettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            )
        )
    }
    val handleCalibrate = { if (active && resumed && granted) viewModel.calibrate() }
    val handleResume = { if (active && resumed && granted) viewModel.resume() }

    // Like an effect with cleanup: navigation and Android's foreground lifecycle both own access.
    DisposableEffect(lifecycle, active, viewModel) {
        if (!active) viewModel.stop()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumed = true
                granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                resumed = false
                viewModel.stop()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }
    val keepAwake = active && resumed && state.running
    DisposableEffect(view, keepAwake) {
        val previous = view.keepScreenOn
        if (keepAwake) view.keepScreenOn = true
        onDispose { if (keepAwake) view.keepScreenOn = previous }
    }
    NoiseMeterScreen(
        state,
        granted,
        handlePermission,
        handleSettings,
        handleCalibrate,
        handleResume,
        viewModel::stop,
        viewModel::setSensitivity,
        viewModel::resetCounter,
    )
}
