package com.oddjobs.app.data

import android.content.Context
import com.oddjobs.app.domain.AppData
import com.oddjobs.app.domain.Out
import com.oddjobs.app.domain.Seed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Local-first store. Keeps the whole app state in memory (StateFlow) and persists it to a file.
 * Local-first store. App data is kept on this device.
 */
class AppRepository(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "oddjobs_state_v2.bin")
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()
    private var saveJob: kotlinx.coroutines.Job? = null

    private fun seeded(): AppData {
        val s = Seed.initial(System.currentTimeMillis())
        return AppData(users = s.users, jobs = s.jobs, reviews = s.reviews)
    }

    private fun load(): AppData {
        try {
            if (file.exists()) {
                ObjectInputStream(BufferedInputStream(FileInputStream(file))).use { stream ->
                    val d = stream.readObject() as AppData
                    if (d.schema == AppData.SCHEMA) return d
                }
            }
        } catch (_: Throwable) {
            // Corrupt or incompatible file: start fresh with demo data.
        }
        return seeded()
    }

    fun update(transform: (AppData) -> AppData) {
        var changed = false
        _data.update { old ->
            val n = transform(old)
            changed = n != old
            n
        }
        if (changed) scheduleSave()
    }

    /** Applies a rule from the Engine and returns its result (including any error key). */
    fun act(f: (AppData) -> Out): Out {
        var result = Out(_data.value)
        update { old ->
            val o = f(old)
            result = o
            o.data
        }
        return result
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(500)
            write(_data.value)
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        write(_data.value)
    }

    @Synchronized
    private fun write(d: AppData) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            ObjectOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { it.writeObject(d) }
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (_: Throwable) {
            // Disk full or similar: keep running from memory.
        }
    }
}

