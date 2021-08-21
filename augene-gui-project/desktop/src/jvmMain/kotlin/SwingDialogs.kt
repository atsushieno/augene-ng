import dev.atsushieno.augene.DialogAbstraction
import dev.atsushieno.augene.model
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane

class SwingDialogs(private val frame: JFrame) : DialogAbstraction() {
    override fun ShowWarning(message: String, onCompleted: () -> Unit) {
        JOptionPane.showMessageDialog(frame, message)
    }

    private fun runFileDialog(
        dialogType: Int,
        dialogTitle: String,
        options: DialogOptions,
        onSelectionConfirmed: (Array<String>) -> Unit
    ) {
        val dialog = JFileChooser()
        dialog.dialogType = dialogType
        dialog.isMultiSelectionEnabled = options.MultipleFiles
        dialog.dialogTitle = dialogTitle
        if (model.projectFileName != null)
            dialog.currentDirectory = File(model.projectDirectory!!)
        if (dialog.showDialog(JFrame(), "OK") == JFileChooser.APPROVE_OPTION)
            if (dialog.isMultiSelectionEnabled)
                onSelectionConfirmed(dialog.selectedFiles.map { it.absolutePath }.toTypedArray())
        else
                onSelectionConfirmed(arrayOf(dialog.selectedFile.absolutePath))
    }

    override fun ShowOpenFileDialog(dialogTitle: String, onSelectionConfirmed: (Array<String>) -> Unit) {
        runFileDialog(JFileChooser.OPEN_DIALOG, dialogTitle, DialogOptions(), onSelectionConfirmed)
    }

    override fun ShowOpenFileDialog(dialogTitle: String, options: DialogOptions, onSelectionConfirmed: (Array<String>) -> Unit) {
        runFileDialog(JFileChooser.OPEN_DIALOG, dialogTitle, options, onSelectionConfirmed)
    }

    override fun ShowSaveFileDialog(dialogTitle: String, onSelectionConfirmed: (Array<String>) -> Unit) {
        runFileDialog(JFileChooser.SAVE_DIALOG, dialogTitle, DialogOptions(), onSelectionConfirmed)
    }

    override fun ShowSaveFileDialog(dialogTitle: String, options: DialogOptions, onSelectionConfirmed: (Array<String>) -> Unit) {
        runFileDialog(JFileChooser.SAVE_DIALOG, dialogTitle, options, onSelectionConfirmed)
    }
}