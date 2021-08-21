import dev.atsushieno.augene.App
import androidx.compose.ui.window.singleWindowApplication
import dev.atsushieno.augene.model

fun main() = singleWindowApplication {
    model.dialogs = SwingDialogs(this.window)
    model.loadConfiguration()
    App()
}