package ru.pogman.questplatform

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import ru.pogman.questplatform.data.PhotoStore
import ru.pogman.questplatform.domain.photoConfig
import ru.pogman.questplatform.screens.GameEditorScreen
import ru.pogman.questplatform.theme.QuestTheme
import ru.pogman.questplatform.viewmodel.OrganizerViewModel

class PhotoActionsTest {
    private val fixture = IsolatedAppRule()
    private val composeRule = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixture).around(composeRule)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val viewModelStore = ViewModelStore()
    private lateinit var organizer: OrganizerViewModel
    private lateinit var registry: PhotoResultRegistry
    private val imported = mutableListOf<String>()
    private val input = File(context.cacheDir, "gallery-test-${System.nanoTime()}.png")

    @Before
    fun openEditor() {
        runBlocking { fixture.container.access.setup("password1", "password1") }
        composeRule.runOnUiThread {
            organizer = OrganizerViewModel(fixture.container)
            viewModelStore.put("organizer", organizer)
            organizer.editGame("detective-photo-demo")
        }
        registry = PhotoResultRegistry()
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        composeRule.setContent {
            val game by organizer.editor.collectAsStateWithLifecycle()
            val action by organizer.action.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                val actions = rememberPhotoActions(game?.id, !action.busy, organizer)
                QuestTheme {
                    game?.let {
                        GameEditorScreen(
                            it,
                            organizer::photoUri,
                            action,
                            organizer::updateGame,
                            organizer::toggleQuest,
                            actions.gallery,
                            actions.camera,
                            organizer::openNfcWriter,
                            organizer::saveGame,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            organizer.editor.value != null && !organizer.action.value.busy
        }
    }

    @After
    fun cleanFiles() {
        composeRule.runOnUiThread { viewModelStore.clear() }
        imported.forEach(fixture.container.photos::discard)
        registry.output?.let(fixture.container.cameraPhotos::discard)
        input.delete()
    }

    @Test
    fun galleryRequestsLocalImagesAndCopiesTheResultIntoTheGame() {
        runBlocking {
            fixture.container.repository.prepareRun("detective-photo-demo", "Фото-команда", "4")
        }
        writeImage(Uri.fromFile(input))
        registry.galleryResult = Uri.fromFile(input)
        click("Из галереи")
        val reference = waitForImportedPhoto()
        assertTrue(registry.intent!!.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY, false))
        assertNotEquals(Intent.ACTION_OPEN_DOCUMENT, registry.intent!!.action)
        assertEquals("image/*", registry.intent!!.type)
        input.delete()
        click("Сохранить игру")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            !organizer.action.value.busy && organizer.editor.value == null
        }
        runBlocking {
            val saved = fixture.container.repository.loadGame("detective-photo-demo")
            assertEquals(reference, saved.photoConfig()!!.photoRef)
            assertTrue(fixture.container.photos.exists(reference))
            assertEquals(
                fixture.container.photos.displayUri(reference),
                fixture.container.repository.activeRun.first()!!.quests.single().photoUri,
            )
        }
    }

    @Test
    fun cameraUsesPrivateOutputAndCancelKeepsTheCurrentPhoto() {
        registry.cancel = true
        click("Снять фото")
        composeRule.waitForIdle()
        assertEquals(PhotoStore.DEMO_PHOTO, organizer.editor.value!!.photoConfig()!!.photoRef)
        assertTemporaryOutputDeleted()
        registry.cancel = false
        click("Снять фото")
        waitForImportedPhoto()
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, registry.intent!!.action)
        assertEquals("content", registry.output!!.scheme)
        assertEquals("${context.packageName}.photos", registry.output!!.authority)
        assertTrue(registry.intent!!.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
        assertEquals(registry.output, registry.intent!!.clipData!!.getItemAt(0).uri)
        assertTemporaryOutputDeleted()
    }

    @Test
    fun delayedCameraResultCannotReplaceAnotherGamesPhoto() {
        registry.delayResult = true
        click("Снять фото")
        val originalId = organizer.editor.value!!.id
        composeRule.runOnUiThread { organizer.editGame(null) }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            organizer.editor.value?.id != originalId && !organizer.action.value.busy
        }
        composeRule.runOnUiThread { registry.finishCamera() }
        composeRule.waitUntil(timeoutMillis = 10_000) { !organizer.action.value.busy }
        assertEquals(PhotoStore.DEMO_PHOTO, organizer.editor.value!!.photoConfig()!!.photoRef)
        assertTemporaryOutputDeleted()
    }

    private fun click(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().assertIsEnabled().performClick()
    }

    private fun waitForImportedPhoto(): String {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            organizer.editor.value?.photoConfig()?.photoRef != PhotoStore.DEMO_PHOTO &&
                !organizer.action.value.busy
        }
        return organizer.editor.value!!.photoConfig()!!.photoRef.also(imported::add)
    }

    private fun assertTemporaryOutputDeleted() {
        assertThrows(FileNotFoundException::class.java) {
            context.contentResolver.openInputStream(registry.output!!)!!.close()
        }
    }

    private fun writeImage(uri: Uri) {
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        try {
            context.contentResolver.openOutputStream(uri)!!.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally {
            bitmap.recycle()
        }
    }

    // Exercise AndroidX contracts and our actual FileProvider without taking a user's photo.
    private inner class PhotoResultRegistry : ActivityResultRegistry() {
        var intent: Intent? = null
        var output: Uri? = null
        var galleryResult: Uri? = null
        var cancel = false
        var delayResult = false
        var pendingCode: Int? = null

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            intent = contract.createIntent(context, input)
            if (
                intent!!.action ==
                    ActivityResultContracts.RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS
            ) {
                dispatchResult(
                    requestCode,
                    Activity.RESULT_OK,
                    Intent()
                        .putExtra(
                            ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS,
                            arrayOf(Manifest.permission.CAMERA),
                        )
                        .putExtra(
                            ActivityResultContracts.RequestMultiplePermissions
                                .EXTRA_PERMISSION_GRANT_RESULTS,
                            intArrayOf(PackageManager.PERMISSION_GRANTED),
                        ),
                )
                return
            }
            if (intent!!.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                @Suppress("DEPRECATION")
                val uri = intent!!.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
                output = uri
                pendingCode = requestCode
                if (!delayResult) finishCamera()
            } else {
                dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(galleryResult))
            }
        }

        fun finishCamera() {
            if (!cancel) writeImage(output!!)
            dispatchResult(
                pendingCode!!,
                if (cancel) Activity.RESULT_CANCELED else Activity.RESULT_OK,
                null,
            )
            pendingCode = null
        }
    }
}
