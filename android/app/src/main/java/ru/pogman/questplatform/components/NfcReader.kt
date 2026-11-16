package ru.pogman.questplatform.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch
import ru.pogman.questplatform.R

@Composable
fun NfcReader(enabled: Boolean, onTag: (Tag) -> Unit) {
    val context = LocalContext.current
    val activity = context.activity()
    val adapter =
        remember(context) { context.getSystemService(NfcManager::class.java)?.defaultAdapter }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val onTagUpdated by rememberUpdatedState(onTag)
    var ready by remember { mutableStateOf(adapter?.isEnabled == true) }
    val handleSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }

    DisposableEffect(enabled, adapter, activity, lifecycle) {
        val listening = AtomicBoolean(false)
        val owner = Any()
        fun update() {
            ready = adapter?.isEnabled == true
            if (
                enabled &&
                    ready &&
                    activity != null &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                listening.set(true)
                NfcReaderOwner.claim(activity, owner)
                adapter?.enableReaderMode(
                    activity,
                    { tag ->
                        if (listening.get())
                            scope.launch {
                                if (listening.get() && NfcReaderOwner.isCurrent(activity, owner))
                                    onTagUpdated(tag)
                            }
                    },
                    NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V,
                    null,
                )
            } else {
                listening.set(false)
                if (activity != null && NfcReaderOwner.release(activity, owner))
                    adapter?.disableReaderMode(activity)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME ||
                    event == Lifecycle.Event.ON_PAUSE ||
                    event == Lifecycle.Event.ON_STOP
            )
                update()
        }
        lifecycle.addObserver(observer)
        update()
        onDispose {
            listening.set(false)
            lifecycle.removeObserver(observer)
            if (activity != null && NfcReaderOwner.release(activity, owner))
                adapter?.disableReaderMode(activity)
        }
    }
    when {
        adapter == null || activity == null -> Text(stringResource(R.string.nfc_unavailable))
        !ready -> {
            Text(stringResource(R.string.nfc_disabled))
            Button(onClick = handleSettings) { Text(stringResource(R.string.nfc_settings_action)) }
        }
        else ->
            Text(
                stringResource(if (enabled) R.string.nfc_tap_hint else R.string.nfc_processing_hint)
            )
    }
}

// ReaderMode belongs to the Activity: an outgoing animated scene must not disable its successor.
private object NfcReaderOwner {
    private val owners = WeakHashMap<Activity, Any>()

    fun claim(activity: Activity, owner: Any) {
        owners[activity] = owner
    }

    fun isCurrent(activity: Activity, owner: Any): Boolean = owners[activity] === owner

    fun release(activity: Activity, owner: Any): Boolean {
        if (!isCurrent(activity, owner)) return false
        owners.remove(activity)
        return true
    }
}

private tailrec fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
