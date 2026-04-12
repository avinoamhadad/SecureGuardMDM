package com.secureguard.mdm.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.secureguard.mdm.R

/**
 * דיאלוג גנרי להצגת מידע או בקשת אישור מהמשתמש.
 * @param title כותרת הדיאלוג.
 * @param message גוף ההודעה.
 * @param onDismiss הפעולה שתתבצע כאשר הדיאלוג נסגר.
 * @param onConfirm (אופציונלי) הפעולה שתתבצע בלחיצה על כפתור האישור. אם הוא null, יוצג רק כפתור סגירה.
 * @param isDestructive (אופציונלי) האם צבע כפתור האישור צריך להיות אדום (לפעולות מסוכנות).
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        dismissButton = {
            if (onConfirm != null) { // מציג כפתור "ביטול" רק אם יש גם כפתור "אישור"
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.dialog_button_cancel))
                }
            }
        },
        confirmButton = {
            val confirmButtonText = if (onConfirm != null) {
                stringResource(id = R.string.dialog_button_confirm)
            } else {
                stringResource(id = R.string.dialog_button_confirm) // אותו טקסט במקרה הזה
            }

            TextButton(
                onClick = {
                    onConfirm?.invoke() ?: onDismiss() // אם onConfirm קיים, הפעל אותו. אחרת, פשוט סגור.
                }
            ) {
                Text(
                    text = confirmButtonText,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}