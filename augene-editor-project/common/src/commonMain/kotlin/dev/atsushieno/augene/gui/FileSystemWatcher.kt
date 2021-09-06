package dev.atsushieno.augene.gui

interface FileSystemEventListener {
    fun onEvent(filePath: String, eventType: FileWatcher.EventType)
}

// FIXME: implement
class FileWatcher {
    enum class EventType {
        Created,
        Modified,
        Deleted
    }

    lateinit var platformContext: Any
    private val listeners = mutableListOf<FileSystemEventListener>()

    fun addChangeListener(listener: FileSystemEventListener) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: FileSystemEventListener) {
        listeners.remove(listener) // does this even work?
    }

    fun onEvent(filePath: String, eventType: EventType) = listeners.forEach { it.onEvent(filePath, eventType) }

    fun addTargetPath(filePath: String) = addFileWatcherTargetPath(this, filePath)

    fun removeTargetPath(filePath: String) = removeFileWatcherTargetPath(this, filePath)

    private var enabled = false
    var enableRaisingEvents : Boolean
        get() = enabled
        set(value) {
            enabled = value
            updateFileChangeListenerStatus(this)
        }

    fun terminate() {
        releaseFileWatcherContext(this)
    }

    init {
        platformContext = createFileWatcherContext(this)
    }
}

class FileSystemWatcherEventArgs(val fullPath: String) // not sure if we should support change type as it may not be available on all platforms.

internal expect fun createFileWatcherContext(watcher: FileWatcher): Any
internal expect fun releaseFileWatcherContext(watcher: FileWatcher)
internal expect fun updateFileChangeListenerStatus(watcher: FileWatcher)
internal expect fun addFileWatcherTargetPath(watcher: FileWatcher, pathToBeWatched: String?)
internal expect fun removeFileWatcherTargetPath(watcher: FileWatcher, pathToBeWatched: String?)
