package com.carettafriends.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UITextField

@Composable
actual fun PlatformChoiceDialog(
    title: String,
    message: String?,
    actions: List<DialogAction>,
    onDismiss: () -> Unit,
) {
    // The list and the callbacks may be rebuilt on recomposition; the presented alert must keep
    // calling the CURRENT ones, not the ones captured when it was first shown.
    val latest = rememberUpdatedState(actions to onDismiss)
    DisposableEffect(Unit) {
        var handled = false
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert,
        )
        actions.forEachIndexed { index, action ->
            alert.addAction(
                UIAlertAction.actionWithTitle(
                    title = action.title,
                    style = action.style.toUiKit(),
                    handler = { _ ->
                        handled = true
                        // Clear the caller's state first: the action itself may navigate away.
                        latest.value.second()
                        latest.value.first.getOrNull(index)?.onSelect?.invoke()
                    },
                ),
            )
            // iOS bolds one action as the suggested answer, the way its own dialogs do.
            if (action.style == DialogStyle.PRIMARY) alert.preferredAction = alert.actions.last() as UIAlertAction
        }
        topmostViewController()?.presentViewController(alert, animated = true, completion = null)
        onDispose {
            if (!handled) alert.dismissViewControllerAnimated(flag = true, completion = null)
        }
    }
}

@Composable
actual fun PlatformTextPrompt(
    title: String,
    message: String?,
    initial: String,
    placeholder: String,
    confirmLabel: String,
    cancelLabel: String,
    secure: Boolean,
    onResult: (String?) -> Unit,
) {
    val latest = rememberUpdatedState(onResult)
    DisposableEffect(Unit) {
        var handled = false
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert,
        )
        alert.addTextFieldWithConfigurationHandler { field ->
            field?.text = initial
            field?.placeholder = placeholder
            field?.secureTextEntry = secure
        }
        alert.addAction(
            UIAlertAction.actionWithTitle(cancelLabel, UIAlertActionStyleCancel) { _ ->
                handled = true
                latest.value(null)
            },
        )
        val confirm = UIAlertAction.actionWithTitle(confirmLabel, UIAlertActionStyleDefault) { _ ->
            handled = true
            latest.value((alert.textFields?.firstOrNull() as? UITextField)?.text.orEmpty())
        }
        alert.addAction(confirm)
        alert.preferredAction = confirm
        topmostViewController()?.presentViewController(alert, animated = true, completion = null)
        onDispose {
            if (!handled) alert.dismissViewControllerAnimated(flag = true, completion = null)
        }
    }
}

private fun DialogStyle.toUiKit(): Long = when (this) {
    DialogStyle.DESTRUCTIVE -> UIAlertActionStyleDestructive
    DialogStyle.CANCEL -> UIAlertActionStyleCancel
    else -> UIAlertActionStyleDefault
}
