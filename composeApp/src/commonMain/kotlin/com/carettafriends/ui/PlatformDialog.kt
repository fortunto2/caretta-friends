package com.carettafriends.ui

import androidx.compose.runtime.Composable

/** How an action reads: plain, the recommended one, a destructive one, or "never mind". */
enum class DialogStyle { DEFAULT, PRIMARY, DESTRUCTIVE, CANCEL }

/** One button in a platform dialog. */
data class DialogAction(
    val title: String,
    val style: DialogStyle = DialogStyle.DEFAULT,
    val onSelect: () -> Unit = {},
)

/**
 * A question asked in the platform's OWN dialog — a real `UIAlertController` on iOS, a Material
 * dialog on Android.
 *
 * The app draws its own cards and buttons on purpose, but dialogs are the one place where a
 * hand-drawn imitation always reads as foreign: on iPhone a Material dialog has the wrong corner
 * radius, the wrong button row, the wrong dismiss gesture and the wrong keyboard behaviour. These
 * are the moments to hand over to the OS.
 *
 * [onDismiss] fires for every outcome, including a plain dismissal, and always BEFORE the chosen
 * action runs — so the caller's state is cleared before an action navigates away.
 */
@Composable
expect fun PlatformChoiceDialog(
    title: String,
    message: String? = null,
    actions: List<DialogAction>,
    onDismiss: () -> Unit,
)

/**
 * A single-field prompt (your name, an email…), again in the platform's own dialog.
 * [onResult] gets the entered text, or null if the volunteer backed out.
 */
@Composable
expect fun PlatformTextPrompt(
    title: String,
    message: String? = null,
    initial: String = "",
    placeholder: String = "",
    confirmLabel: String,
    cancelLabel: String,
    secure: Boolean = false,
    onResult: (String?) -> Unit,
)

/**
 * One option in a "pick one of these" dialog: ticked and highlighted when it's the current value.
 * The three pickers built on [PlatformChoiceDialog] all need exactly this.
 */
fun choiceAction(label: String, selected: Boolean, onSelect: () -> Unit): DialogAction = DialogAction(
    title = if (selected) "✓ $label" else label,
    style = if (selected) DialogStyle.PRIMARY else DialogStyle.DEFAULT,
    onSelect = onSelect,
)
