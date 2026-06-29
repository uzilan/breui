package breui.model

sealed class Overlay {
    data class Confirm(val message: String, val onConfirm: () -> Unit) : Overlay()
    object TapManager : Overlay()
    data class Progress(val title: String, val lines: List<String>) : Overlay()
}
