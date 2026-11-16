package ru.pogman.questplatform

import android.content.Context
import androidx.room.Room
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.pogman.questplatform.data.CameraPhotoStore
import ru.pogman.questplatform.data.MaterialStore
import ru.pogman.questplatform.data.NfcTagStore
import ru.pogman.questplatform.data.OrganizerAccess
import ru.pogman.questplatform.data.PhotoStore
import ru.pogman.questplatform.data.QuestDatabase
import ru.pogman.questplatform.data.QuestRepository

class AppContainer(
    context: Context,
    val database: QuestDatabase =
        Room.databaseBuilder(context, QuestDatabase::class.java, "quest-platform.db").build(),
    val clock: Clock = Clock.systemUTC(),
) {
    val access = OrganizerAccess(database)
    val photos = PhotoStore(context)
    val materials = MaterialStore(context, photos)
    val cameraPhotos = CameraPhotoStore(context)
    val nfc = NfcTagStore()
    val repository = QuestRepository(database, access, photos, clock)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val initialization = scope.launch {
        val repairs = context.getSharedPreferences("data_repairs", Context.MODE_PRIVATE)
        val key = "deleted_game_activation_v1:${database.openHelper.databaseName}"
        if (repairs.getBoolean(key, false)) {
            repository.initialize()
        } else {
            // Repair old activations once, so an explicitly reopened archive can persist later.
            repository.initialize(repairDeletedGameActivation = true)
            repairs.edit().putBoolean(key, true).commit()
        }
    }
}
