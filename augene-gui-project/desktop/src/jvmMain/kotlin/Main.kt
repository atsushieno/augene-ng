import dev.atsushieno.augene.gui.App
import androidx.compose.ui.window.singleWindowApplication
import dev.atsushieno.augene.gui.model

fun main() = singleWindowApplication {
    model.dialogs = SwingDialogs(this.window)
    model.loadConfiguration()
    App()
}