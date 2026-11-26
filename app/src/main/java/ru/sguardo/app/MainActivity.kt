package ru.sguardo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.Coil
import kotlinx.coroutines.launch
import coil.ImageLoader
import ru.sguardo.app.data.Phase
import ru.sguardo.app.data.Repo
import ru.sguardo.app.ui.Permissions
import ru.sguardo.app.ui.SgApp

class MainActivity : ComponentActivity(), Permissions {
    private lateinit var repo: Repo
    private var permDone: ((Boolean) -> Unit)? = null
    private var pickDone: ((Uri?) -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> permDone?.invoke(r.values.all { it }); permDone = null }
    private val imageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> pickDone?.invoke(u); pickDone = null }
    private val fileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { u -> pickDone?.invoke(u); pickDone = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        repo = Repo.get(this)
        Coil.setImageLoader(ImageLoader.Builder(this).okHttpClient(repo.api.client).crossfade(true).build())
        repo.watchNetwork()
        if (repo.phase.value == Phase.Launching) repo.bootstrap()
        handle(intent)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { if (repo.phase.value == Phase.Ready && !repo.demo.value) { repo.startLive(); repo.scope.launch { repo.reload() } } }
            override fun onStop(owner: LifecycleOwner) { repo.stopLive() }
        })
        setContent { SgApp(repo, this) }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }

    private fun handle(i: Intent?) { i?.getStringExtra("url")?.let { repo.handleUrl(it) } }

    private fun ask(perms: Array<String>, done: (Boolean) -> Unit) {
        val need = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isEmpty()) { done(true); return }
        permDone = done; permLauncher.launch(need.toTypedArray())
    }

    override fun notifications(done: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= 33) ask(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { ok -> if (ok) repo.registerPushIfPossible(); done(ok) }
        else { repo.registerPushIfPossible(); done(true) }
    }
    override fun contacts(done: (Boolean) -> Unit) = ask(arrayOf(Manifest.permission.READ_CONTACTS)) { ok -> repo.book.refresh(); done(ok) }
    override fun audio(done: (Boolean) -> Unit) = ask(arrayOf(Manifest.permission.RECORD_AUDIO), done)
    override fun camera(done: (Boolean) -> Unit) = ask(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), done)
    override fun hasContacts() = granted(Manifest.permission.READ_CONTACTS)
    override fun hasNotifications() = Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)
    override fun hasAudio() = granted(Manifest.permission.RECORD_AUDIO)
    override fun hasCamera() = granted(Manifest.permission.CAMERA)
    override fun pickImage(done: (Uri?) -> Unit) { pickDone = done; imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
    override fun pickFile(done: (Uri?) -> Unit) { pickDone = done; fileLauncher.launch(arrayOf("*/*")) }
    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
}
