package dev.atsushieno.augene.gui

abstract class DialogAbstraction
{
	class DialogOptions
	{
		var initialDirectory : String? = null
		var MultipleFiles : Boolean = false
	}

	abstract fun ShowWarning (message: String, onCompleted: () -> Unit)

	abstract fun ShowOpenFileDialog (dialogTitle: String, onSelectionConfirmed: (Array<String>) -> Unit)
	abstract fun ShowOpenFileDialog (dialogTitle: String, options: DialogOptions, onSelectionConfirmed: (Array<String>) -> Unit)

	abstract fun ShowSaveFileDialog (dialogTitle: String, onSelectionConfirmed: (Array<String>) -> Unit)
	abstract fun ShowSaveFileDialog (dialogTitle: String, options: DialogOptions, onSelectionConfirmed: (Array<String>) -> Unit)
}