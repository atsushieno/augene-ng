package dev.atsushieno.augene

import kotlin.test.Test
import kotlin.test.assertTrue

class FileSupportTest {
    @Test
    fun resolvePath() {
        val fs = FileSupport("dummy")
        val relPath = fs.resolvePathRelativeToProject("../samples/automation/opnplug.augene")
        val absPath = FileSupport.canonicalizePath(relPath)
        val content = fs.readString(absPath)
        assertTrue(content.isNotEmpty(), "content")
    }
}
