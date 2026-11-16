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

data class MicrophoneAccess(
    val granted: Boolean,
    val ready: Boolean,
    val request: () -> Unit,
    val settings: () -> Unit,
)

@Composable
fun rememberMicrophoneAccess(
    active: Boolean,
    keepAwake: Boolean,
    onStop: () -> Unit,
): MicrophoneAccess {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var resumed by remember {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val stop by rememberUpdatedState(onStop)
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
    DisposableEffect(lifecycle, active) {
        if (!active) stop()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumed = true
                granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                resumed = false
                stop()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            stop()
        }
    }
    val awake = active && resumed && keepAwake
    DisposableEffect(view, awake) {
        val previous = view.keepScreenOn
        if (awake) view.keepScreenOn = true
        onDispose { if (awake) view.keepScreenOn = previous }
    }
    return MicrophoneAccess(granted, active && resumed && granted, handlePermission, handleSettings)
}
