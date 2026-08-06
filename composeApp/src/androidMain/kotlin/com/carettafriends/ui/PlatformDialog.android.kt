package com.carettafriends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.carettafriends.ui.theme.caretta

/** Android already gets its own look from Material — this is the platform dialog here. */
@Composable
actual fun PlatformChoiceDialog(
    title: String,
    message: String?,
    actions: List<DialogAction>,
    onDismiss: () -> Unit,
) {
    val c = caretta
    val cancel = actions.firstOrNull { it.style == DialogStyle.CANCEL }
    val rest = actions.filter { it.style != DialogStyle.CANCEL }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                message?.let { Text(it, color = c.muted) }
                // Material stacks at most two buttons in its row, so the choices are rows here —
                // a list of options reads better than a cramped button strip anyway.
                rest.forEach { action ->
                    TextButton(
                        onClick = { onDismiss(); action.onSelect() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Left-aligned: these are list choices, not a centred button strip.
                        Text(
                            action.title,
                            color = if (action.style == DialogStyle.DESTRUCTIVE) c.risk else c.sea,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); cancel?.onSelect?.invoke() }) {
                Text(cancel?.title ?: "OK")
            }
        },
    )
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
    val c = caretta
    var draft by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { onResult(null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message?.let { Text(it, color = c.muted) }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(60) },
                    singleLine = true,
                    placeholder = { Text(placeholder) },
                    visualTransformation = if (secure) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onResult(draft) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } },
    )
}
