/**
 * Copyright (C) 2020 Dominik Glaser
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dogla.filesystem.watcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.*

/**
 * A file system watcher that internally starts another thread for the event handling.
 * Inspired by the XNIO WatchServiceFileSystemWatcher.
 *
 * @author Dominik Glaser
 * @since 1.0
 */
class FileSystemWatcher(name: String) : Runnable {
    private var watchService: WatchService? = null
    private val dataByFile = Collections.synchronizedMap(HashMap<File, WatchKeyData>())
    private val dataByKey = Collections.synchronizedMap(IdentityHashMap<WatchKey, WatchKeyData>())

    @Volatile
    private var stopped = false
    private val watchThread: Thread
    override fun run() {
        while (!stopped) {
            try {
                //System.err.println("take() run");
                val key = watchService!!.take()
                if (key != null) {
                    // Prevent receiving two separate ENTRY_MODIFY events: file modified
                    // and timestamp updated. Instead, receive one ENTRY_MODIFY event
                    // with two counts.
                    Thread.sleep(100)
                    val watchablePath = key.watchable() as Path
                    try {
                        //System.err.println("take() returned");
                        val pathData = dataByKey[key]
                        if (pathData != null) {
                            handleEvents(watchablePath, pathData, key.pollEvents())
                        }
                    } finally {
                        //if the key is no longer valid remove it from the files list
                        if (!key.reset()) {
                            dataByFile.remove(watchablePath.toFile())
                        }
                    }
                }
            } catch (e: InterruptedException) {
                //ignore
                //System.err.println("InterruptedException");
            } catch (cwse: ClosedWatchServiceException) {
                // the watcher service is closed, so no more waiting on events
                //System.err.println("ClosedWatchServiceException");
                break
            }
        }
    }

    /**
     * Handles the occured watch events.
     *
     * @param watchablePath the watchable path
     * @param pathData the path data
     * @param events the events
     */
    protected fun handleEvents(watchablePath: Path, pathData: WatchKeyData, events: List<WatchEvent<*>>) {
        //System.err.println("FileSystemWatcher.handleEvents()");
        val results: MutableList<FileSystemEvent> = ArrayList()
        val addedFiles: MutableSet<File?> = HashSet()
        val deletedFiles: MutableSet<File?> = HashSet()
        val config = pathData.config
        for (event in events) {
            val eventPath = event.context() as Path
            val targetPath = watchablePath.resolve(eventPath)
            val targetFile = targetPath.toFile()
            //System.err.println(" - " + targetFile.getAbsolutePath());
            // check if changed file starts with the same path (needed for single file watching)
            if (!targetFile.absolutePath.contains(pathData.path.toFile().absolutePath)) {
                continue
            }
            // check config
            val currentDepth = targetPath.nameCount - pathData.path.nameCount
            if (currentDepth > config.maxDepth) {
                continue
            }
            var type: FileSystemEventType? = null
            if (event.kind() === StandardWatchEventKinds.ENTRY_CREATE) {
                type = FileSystemEventType.ADDED
                if (!config.isEventAllowed(type)) {
                    continue
                }
                addedFiles.add(targetFile)
            } else if (event.kind() === StandardWatchEventKinds.ENTRY_MODIFY) {
                type = FileSystemEventType.MODIFIED
                if (!config.isEventAllowed(type)) {
                    continue
                }
            } else if (event.kind() === StandardWatchEventKinds.ENTRY_DELETE) {
                type = FileSystemEventType.REMOVED
                if (!config.isEventAllowed(type)) {
                    continue
                }
                deletedFiles.add(targetFile)
            } else if (event.kind() === StandardWatchEventKinds.OVERFLOW) {
                logger.warn("Overflow detected: {}", targetFile) //$NON-NLS-1$
                continue
            }
            if (targetFile.isDirectory && FileSystemEventType.ADDED == type) {
                val depth = config.maxDepth - currentDepth
                if (depth > 0) {
                    try {
                        // watch all valid sub directories
                        Files.walk(targetFile.toPath(), depth).forEach { p: Path ->
                            val file = p.toFile()
                            if (file.isDirectory) {
                                if (p.nameCount - pathData.path.nameCount <= config.maxDepth) {
                                    try {
                                        addWatchedDirectory(pathData, file)
                                    } catch (e: IOException) {
                                        logger.warn("Could not watch directory: {}", file, e) //$NON-NLS-1$
                                    }
                                }
                            }
                            results.add(FileSystemEvent(file, FileSystemEventType.ADDED))
                        }
                    } catch (e: IOException) {
                        logger.warn("Could not traverse directory: {}", targetFile, e) //$NON-NLS-1$
                    }
                } else {
                    // directory content should not be watched because it reached the max-depth setting
                    results.add(FileSystemEvent(targetFile, FileSystemEventType.ADDED))
                }
            } else {
                results.add(FileSystemEvent(targetFile, type))
            }
        }

        // filter duplicate events
        // e.g. if the file is modified after creation we only want to show the create event
        run {
            val it = results.iterator()
            while (it.hasNext()) {
                val event = it.next()
                if (event.type == FileSystemEventType.MODIFIED) {
                    if (addedFiles.contains(event.file) &&
                        deletedFiles.contains(event.file)
                    ) {
                        // XNIO-344
                        // All file change events (ADDED, REMOVED and MODIFIED) occurred here.
                        // This happens when an updated file is moved from the different
                        // filesystems or the directory having different project quota on Linux.
                        // ADDED and REMOVED events will be removed in the latter conditional branching.
                        // So, this MODIFIED event needs to be kept for the file change notification.
                        continue
                    }
                    if (addedFiles.contains(event.file) ||
                        deletedFiles.contains(event.file)
                    ) {
                        it.remove()
                    }
                } else if (event.type == FileSystemEventType.ADDED) {
                    if (deletedFiles.contains(event.file)) {
                        it.remove()
                    }
                } else if (event.type == FileSystemEventType.REMOVED) {
                    if (addedFiles.contains(event.file)) {
                        it.remove()
                    }
                }
            }
        }

        // consider custom filter
        val filter = config.filter
        if (filter != null) {
            val it = results.iterator()
            while (it.hasNext()) {
                val event = it.next()
                val file = event.file
                if (!filter.test(file!!.toPath())) {
                    it.remove()
                }
            }
        }
        if (!results.isEmpty()) {
            try {
                // handle the events in another thread 
                val t = Thread {
                    for (event in results) {
                        val listeners = pathData.listeners.toTypedArray()
                        for (listener in listeners) {
                            try {
                                listener.fileChanged(event)
                            } catch (e: Exception) {
                                logger.error(e.message, e)
                            }
                        }
                    }
                }
                t.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread: Thread?, e: Throwable ->
                    logger.error(
                        "Uncaught exception detected: {}",
                        e.message,
                        e
                    )
                } //$NON-NLS-1$
                t.start()
            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        }
        //System.err.println("FileSystemWatcher.handleEvents() - end");
    }

    /**
     * Registers the given listener for the given file.
     *
     * @param file the file to watch
     * @param listener the lisener
     */
    @Synchronized
    fun watchPath(file: File, listener: FileSystemListener) {
        watchPath(file, listener, FileSystemConfig())
    }

    /**
     * Registers the given listener for the given file.
     *
     * @param file the file to watch
     * @param listener the lisener
     * @param config the config
     */
    @Synchronized
    fun watchPath(file: File, listener: FileSystemListener, config: FileSystemConfig?) {
        //System.err.println("FileSystemWatcher.watchPath()");
        requireNotNull(config) {
            "The variable config may not be null" //$NON-NLS-1$
        }
        try {
            var data = dataByFile[file]
            if (data == null) {
                data = WatchKeyData(Paths.get(file.toURI()), config)
                if (file.isDirectory) {
                    try {
                        val d: WatchKeyData = data
                        Files.walk(file.toPath(), config.maxDepth - 1).forEach { p: Path ->
                            val f = p.toFile()
                            if (f.isDirectory) {
                                try {
                                    addWatchedDirectory(d, f)
                                } catch (e: IOException) {
                                    logger.warn("Could not watch directory: {}", f, e) //$NON-NLS-1$
                                }
                            }
                        }
                    } catch (e: IOException) {
                        logger.warn("Could not traverse directory: {}", file, e) //$NON-NLS-1$
                    }
                } else {
                    // be sure the parent exists
                    if (!file.parentFile.exists()) {
                        file.parentFile.mkdirs()
                    }
                    addWatchedDirectory(data, file.parentFile)
                }
                dataByFile[file] = data
            }
            data.listeners.add(listener)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        //System.err.println("FileSystemWatcher.watchPath() - end");
    }

    @Throws(IOException::class)
    private fun addWatchedDirectory(data: WatchKeyData, dir: File) {
        val path = Paths.get(dir.toURI())
        val key = path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
        dataByKey[key] = data
        data.keys.add(key)
    }

    /**
     * Unwatches the given file.
     *
     * @param file the file to unwatch
     * @param listener the listener to remove
     */
    @Synchronized
    fun unwatchPath(file: File, listener: FileSystemListener) {
        val data = dataByFile[file]
        if (data != null) {
            data.listeners.remove(listener)
            if (data.listeners.isEmpty()) {
                dataByFile.remove(file)
                for (key in data.keys) {
                    key.reset()
                    key.cancel()
                    dataByKey.remove(key)
                }
            }
        }
    }

    /**
     * Unwatches all registered paths.
     */
    @Synchronized
    fun unwatchPaths() {
        //System.err.println("FileSystemWatcher.unwatchPaths()");
        dataByFile.clear()
        for (value in dataByKey.values) {
            for (key in value.keys) {
                key.cancel()
            }
        }
        dataByKey.clear()
        //System.err.println("FileSystemWatcher.unwatchPaths() - end");
    }

    /**
     * Closes the underlying watch service.
     */
    fun close() {
        stopped = true
        watchThread.interrupt()
        try {
            if (watchService != null) {
                watchService!!.close()
            }
        } catch (ioe: IOException) {
            // ignore
        }
    }

    inner class WatchKeyData(val path: Path, val config: FileSystemConfig) {
        val listeners: MutableList<FileSystemListener> = ArrayList()
        val keys: MutableList<WatchKey> = ArrayList()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FileSystemWatcher::class.java)
    }

    /**
     * Constructor.
     *
     * @param name the name of the file system watcher (used as part of the watch service thread name)
     */
    init {
        watchService = try {
            FileSystems.getDefault().newWatchService()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        watchThread = Thread(this, "file-system-watcher[$name]") //$NON-NLS-1$ //$NON-NLS-2$
        watchThread.start()
    }
}