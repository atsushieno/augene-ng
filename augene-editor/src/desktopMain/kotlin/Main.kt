import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import dev.atsushieno.augene.gui.App
import androidx.compose.ui.window.singleWindowApplication
import dev.atsushieno.augene.AugeneCompiler
import dev.atsushieno.augene.gui.SwingDialogs
import dev.atsushieno.augene.gui.model
import kotlin.system.exitProcess

fun main(args: Array<String>) = singleWindowApplication(
    icon = BitmapPainter(useResource("ic_launcher.png", ::loadImageBitmap)),
) {
    if (args.any()) {
        val model = AugeneCompiler()
        model.loadProjectFile(args[0])
        model.compile()
        println("augene compiler completed.")
        exitProcess(0)
    } else {
        model.dialogs = SwingDialogs(this.window)
        model.loadConfiguration()
        App()
        model.processExit()
    }
}