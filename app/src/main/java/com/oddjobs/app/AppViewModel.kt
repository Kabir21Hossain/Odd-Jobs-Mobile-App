package com.oddjobs.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oddjobs.app.data.AppRepository
import com.oddjobs.app.domain.AppData
import com.oddjobs.app.domain.Engine
import com.oddjobs.app.domain.JobDraft
import com.oddjobs.app.domain.JobFilter
import com.oddjobs.app.domain.JobStatus
import com.oddjobs.app.domain.Mode
import com.oddjobs.app.domain.Out
import com.oddjobs.app.domain.PayMethod
import com.oddjobs.app.domain.ProfileInput
import com.oddjobs.app.domain.Simulation
import com.oddjobs.app.ui.Errors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class AuthUiState(
    val loading: Boolean = false,
    val authenticated: Boolean = false,
    val error: String? = null
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app, viewModelScope)
    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState

    val state: StateFlow<AppData> = repo.data

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    /** Seeker job-feed filter (kept while the app is open). */
    val filter = MutableStateFlow(JobFilter())

    private fun now() = System.currentTimeMillis()
    private fun uid(): String? = state.value.currentUserId

    init {
        viewModelScope.launch {
            while (true) {
                delay(1500)
                repo.update { Simulation.step(it, now()) }
            }
        }
    }

    override fun onCleared() {
        repo.saveNow()
        super.onCleared()
    }

    fun flush() = repo.saveNow()

    fun toast(text: String) {
        _messages.tryEmit(text)
    }

    private fun act(f: (AppData) -> Out): Out {
        val out = repo.act(f)
        if (!out.ok) toast(Errors.text(out.error!!, state.value.lang))
        return out
    }

    // ---- settings / auth
    fun chooseLang(lang: String) = repo.update { Engine.setLang(it, lang) }
    fun setDemo(on: Boolean) = repo.update { Engine.setDemo(it, on) }
    fun requestOtp(phone: String): Out = act { Engine.requestOtp(it, phone, now()) }
    fun verifyOtp(code: String): Out = act { Engine.verifyOtp(it, code, now()) }
    fun logout() {
        _authState.value = AuthUiState()
        repo.update { Engine.logout(it) }
        flush()
    }
    fun deleteAccount() {
        val id = uid() ?: return
        repo.update { Engine.deleteAccount(it, id) }
        flush()
    }

    // ---- profile
    fun saveProfile(p: ProfileInput): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.saveProfile(it, id, p) }
    }
    fun setPhoto(path: String) { val id = uid() ?: return; repo.update { Engine.setPhoto(it, id, path) } }
    fun setAvailable(on: Boolean) { val id = uid() ?: return; repo.update { Engine.setAvailable(it, id, on) } }
    fun switchMode(mode: Mode) = repo.update { Engine.switchMode(it, mode) }
    fun submitVerification(photos: List<String>): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.submitVerification(it, id, photos, now()) }
    }

    // ---- jobs
    fun postJob(draft: JobDraft): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.postJob(it, id, draft, now()) }
    }
    fun apply(jobId: String, price: Int, message: String, eta: Int): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.applyToJob(it, id, jobId, price, message, eta, now()) }
    }
    fun withdraw(jobId: String): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.withdraw(it, id, jobId) }
    }
    fun hire(jobId: String, seekerId: String): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.hire(it, id, jobId, seekerId, now()) }
    }
    fun advance(jobId: String, to: JobStatus, code: String? = null): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.advance(it, id, jobId, to, now(), code) }
    }
    fun confirm(jobId: String, price: Int, pay: PayMethod): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.confirmCompletion(it, id, jobId, price, pay, now()) }
    }
    fun cancel(jobId: String, reason: String): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.cancel(it, id, jobId, reason, now()) }
    }
    fun toggleSaved(jobId: String) { val id = uid() ?: return; repo.update { Engine.toggleSaved(it, id, jobId) } }
    fun hideJob(jobId: String) { val id = uid() ?: return; repo.update { Engine.hideJob(it, id, jobId) } }
    fun toggleFavorite(seekerId: String) { val id = uid() ?: return; repo.update { Engine.toggleFavorite(it, id, seekerId) } }
    fun block(otherId: String) { val id = uid() ?: return; repo.update { Engine.block(it, id, otherId) } }
    fun unblock(otherId: String) { val id = uid() ?: return; repo.update { Engine.unblock(it, id, otherId) } }
    fun report(type: String, targetId: String, reason: String) {
        val id = uid() ?: return
        repo.update { Engine.report(it, id, type, targetId, reason, now()) }
    }
    fun review(jobId: String, rating: Int, tags: List<String>, comment: String): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.submitReview(it, id, jobId, rating, tags, comment, now()) }
    }

    // ---- chat / notices
    fun send(chatId: String, text: String): Out {
        val id = uid() ?: return Out(state.value, "not_allowed")
        return act { Engine.sendMessage(it, chatId, id, text, now()) }
    }
    fun markChatRead(chatId: String) {
        val id = uid() ?: return
        repo.update { Engine.markChatRead(it, id, chatId, now()) }
    }
    fun markNoticesRead() { val id = uid() ?: return; repo.update { Engine.markNoticesRead(it, id) } }

    // ---- photos: copy a picked image into private app storage, scaled down and rotated upright
    fun importPhoto(uri: Uri, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { copyScaled(uri) }
            onDone(path)
        }
    }

    private fun copyScaled(uri: Uri): String? {
        return try {
            val cr = getApplication<Application>().contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp: Bitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val orientation = try {
                cr.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Throwable) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) {
                val m = Matrix().apply { postRotate(degrees) }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            val dir = File(getApplication<Application>().filesDir, "photos").apply { mkdirs() }
            val f = File(dir, UUID.randomUUID().toString() + ".jpg")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            f.absolutePath
        } catch (_: Throwable) {
            null
        }
    }
}




