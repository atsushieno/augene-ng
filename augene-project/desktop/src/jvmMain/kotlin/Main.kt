import dev.atsushieno.augene.App
import androidx.compose.desktop.Window
import androidx.compose.ui.window.singleWindowApplication
import dev.atsushieno.augene.model
import javax.swing.JFrame

fun main() = singleWindowApplication {
    model.Dialogs = SwingDialogs(this.window)
    model.LoadConfiguration()
    App()
}