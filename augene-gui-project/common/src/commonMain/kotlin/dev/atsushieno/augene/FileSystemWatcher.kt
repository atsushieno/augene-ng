package dev.atsushieno.augene

// FIXME: implement
class FileSystemWatcher {
    private val listeners = mutableListOf<(Any, FileSystemWatcherEventArgs) -> Unit>()

    fun addChangeListener(listener: (Any, FileSystemWatcherEventArgs) -> Unit) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: (Any, FileSystemWatcherEventArgs) -> Unit) {
        listeners.remove(listener) // does this even work?
    }

    var path: String? = null
    var enableRaisingEvents : Boolean = false
}

class FileSystemWatcherEventArgs(val fullPath: String)
