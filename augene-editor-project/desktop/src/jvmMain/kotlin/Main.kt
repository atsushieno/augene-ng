import dev.atsushieno.augene.gui.App
import androidx.compose.ui.window.singleWindowApplication
import dev.atsushieno.augene.AugeneModel
import dev.atsushieno.augene.gui.SwingDialogs
import dev.atsushieno.augene.gui.model
import kotlin.system.exitProcess

fun main(args: Array<String>) = singleWindowApplication {
    if (args.any()) {
        val model = AugeneModel()
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