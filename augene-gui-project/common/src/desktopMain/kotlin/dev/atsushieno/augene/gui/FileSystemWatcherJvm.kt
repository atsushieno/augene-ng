package dev.atsushieno.augene.gui

import de.dogla.filesystem.watcher.FileSystemEvent
import de.dogla.filesystem.watcher.FileSystemEventType
import de.dogla.filesystem.watcher.FileSystemListener
import de.dogla.filesystem.watcher.FileSystemWatcher
import java.io.File

class FileWatcherJvmContext(val watcher: FileWatcher) : FileSystemListener {
    val impl = FileSystemWatcher("augene-ng")
    var enabled = false

    fun updateStatus(enabled: Boolean) {
        this.enabled = enabled
    }

    fun addWatchTarget(path: String?) {
        if (path == null)
            return
        impl.watchPath(File(path), this)
    }

    fun removeWatchTarget(path: String?) {
        if (path == null)
            return
        impl.unwatchPath(File(path), this)
    }

    override fun fileChanged(event: FileSystemEvent?) {
        if (event == null || !enabled)
            return
        val eventType = when (event.type) {
            FileSystemEventType.ADDED -> FileWatcher.EventType.Created
            FileSystemEventType.MODIFIED -> FileWatcher.EventType.Modified
            FileSystemEventType.REMOVED -> FileWatcher.EventType.Deleted
            else -> throw IllegalArgumentException()
        }
        watcher.onEvent(event.file.path, eventType)
    }
}

internal actual fun createFileWatcherContext(watcher: FileWatcher): Any = FileWatcherJvmContext(watcher)

internal actual fun updateFileChangeListenerStatus(watcher: FileWatcher) {
    val ctx = watcher.platformContext as FileWatcherJvmContext
    ctx.updateStatus(watcher.enableRaisingEvents)
}

internal actual fun addFileWatcherTargetPath(watcher: FileWatcher, pathToBeWatched: String?) {
    val ctx = watcher.platformContext as FileWatcherJvmContext
    ctx.addWatchTarget(pathToBeWatched)
}

internal actual fun removeFileWatcherTargetPath(watcher: FileWatcher, pathToBeWatched: String?) {
    val ctx = watcher.platformContext as FileWatcherJvmContext
    ctx.removeWatchTarget(pathToBeWatched)
}
