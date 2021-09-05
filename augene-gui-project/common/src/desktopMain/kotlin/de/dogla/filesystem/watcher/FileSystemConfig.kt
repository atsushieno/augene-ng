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

import de.dogla.filesystem.watcher.FileSystemEventType
import de.dogla.filesystem.watcher.FileSystemConfig
import de.dogla.filesystem.watcher.FileSystemWatcher.WatchKeyData
import de.dogla.filesystem.watcher.FileSystemEvent
import de.dogla.filesystem.watcher.FileSystemListener
import java.nio.file.Path
import java.util.HashSet
import java.util.function.Predicate

/**
 * The configuration for a file system path.
 *
 * @author Dominik Glaser
 * @since 1.0
 */
class FileSystemConfig {
    /**
     * Returns the maxDepth.
     *
     * @return the maxDepth
     */
    var maxDepth = 1
        private set

    /**
     * Returns the filter.
     *
     * @return the filter
     */
    var filter: Predicate<Path>? = null
        private set
    private var allowedEventTypes: MutableSet<FileSystemEventType?>? = null

    /**
     * Sets the maxDepth.
     *
     * @param maxDepth the maxDepth to set
     *
     * @return the instance itself
     */
    fun withMaxDepth(maxDepth: Int): FileSystemConfig {
        check(maxDepth > 0) {
            "Only values greater than 0 are allowed." //$NON-NLS-1$
        }
        this.maxDepth = maxDepth
        return this
    }

    /**
     * Sets the filter.
     *
     * @param filter the filter to set
     *
     * @return the instance itself
     */
    fun withFilter(filter: Predicate<Path>?): FileSystemConfig {
        this.filter = filter
        return this
    }

    /**
     * Returns the allowedEventTypes.
     *
     * @return the allowedEventTypes
     */
    fun getAllowedEventTypes(): Set<FileSystemEventType?>? {
        return allowedEventTypes
    }

    /**
     * Sets the allowedEventTypes.
     *
     * @param allowedEventTypes the allowedEventTypes to set
     *
     * @return the instance itself
     */
    fun withAllowedEventTypes(vararg allowedEventTypes: FileSystemEventType?): FileSystemConfig {
        this.allowedEventTypes = null
        if (allowedEventTypes != null) {
            this.allowedEventTypes = HashSet()
            for (eventType in allowedEventTypes) {
                if (eventType != null) {
                    this.allowedEventTypes!!.add(eventType)
                }
            }
        }
        return this
    }

    /**
     * Sets the allowedEventTypes.
     *
     * @param allowedEventTypes the allowedEventTypes to set
     *
     * @return the instance itself
     */
    fun withAllowedEventTypes(allowedEventTypes: MutableSet<FileSystemEventType?>?): FileSystemConfig {
        this.allowedEventTypes = allowedEventTypes
        return this
    }

    /**
     * This method checks the available event types ([.withAllowedEventTypes]) if the given event type is part of the set.
     * If the event types set is not determined all event types are valid.
     *
     * @param eventType the event type to check
     *
     * @return `true` if the given event type is allowed.
     */
    fun isEventAllowed(eventType: FileSystemEventType?): Boolean {
        return if (allowedEventTypes != null) {
            allowedEventTypes!!.contains(eventType)
        } else eventType != null
    }
}