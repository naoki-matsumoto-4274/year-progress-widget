package com.tamakitech.yearprogress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

/** 「◯◯ 年の目標」：一覧・達成チェック・追加／編集／削除。 */
@Composable
fun GoalsSection(year: Int, goals: List<Goal>, onGoalsChange: (List<Goal>) -> Unit) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.goals_title, year),
                color = AppColors.TextPrimary,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                modifier = Modifier.weight(1f),
            )
            if (goals.isNotEmpty()) {
                Text(
                    stringResource(R.string.goals_done_count, Goals.doneCount(goals), goals.size),
                    color = AppColors.TextTick,
                    style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (goals.isEmpty()) {
            Text(
                stringResource(R.string.goals_empty),
                color = AppColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = (14 * 1.7).sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        goals.forEach { goal ->
            GoalRow(
                goal = goal,
                onToggle = { onGoalsChange(Goals.toggle(goals, goal.id)) },
                onEdit = {
                    editingId = goal.id
                    dialogOpen = true
                },
            )
            HorizontalDivider(color = AppColors.Track, thickness = 1.dp)
        }

        TextButton(
            onClick = {
                editingId = null
                dialogOpen = true
            },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("＋ " + stringResource(R.string.goals_add), color = AppColors.AccentText, fontSize = 15.sp)
        }
    }

    if (dialogOpen) {
        val editing = goals.firstOrNull { it.id == editingId }
        GoalDialog(
            initialText = editing?.text.orEmpty(),
            isEdit = editing != null,
            onSave = { text ->
                onGoalsChange(
                    if (editing != null) Goals.edit(goals, editing.id, text)
                    else Goals.add(goals, UUID.randomUUID().toString(), text),
                )
                dialogOpen = false
            },
            onDelete = {
                if (editing != null) onGoalsChange(Goals.remove(goals, editing.id))
                dialogOpen = false
            },
            onDismiss = { dialogOpen = false },
        )
    }
}

@Composable
private fun GoalRow(goal: Goal, onToggle: () -> Unit, onEdit: () -> Unit) {
    val toggleLabel = stringResource(if (goal.done) R.string.goals_mark_undone else R.string.goals_mark_done)
    val editLabel = stringResource(R.string.goals_dialog_edit_title)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClickLabel = editLabel, role = Role.Button, onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = goal.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = AppColors.Accent,
                uncheckedColor = AppColors.TextTick,
                checkmarkColor = AppColors.Background,
            ),
            modifier = Modifier.semantics { contentDescription = toggleLabel },
        )
        Spacer(Modifier.width(4.dp))
        Text(
            goal.text,
            color = if (goal.done) AppColors.TextTick else AppColors.TextPrimary,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textDecoration = if (goal.done) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun GoalDialog(
    initialText: String,
    isEdit: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val canSave = Goals.normalize(text) != null
    val focus = remember { FocusRequester() }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            text = { Text(stringResource(R.string.goals_delete_confirm, initialText)) },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.goals_delete), color = AppColors.BarRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.goals_cancel))
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isEdit) R.string.goals_dialog_edit_title else R.string.goals_dialog_add_title))
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(Goals.MAX_LENGTH) },
                label = { Text(stringResource(R.string.goals_input_label)) },
                supportingText = { Text("${text.length} / ${Goals.MAX_LENGTH}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = canSave) {
                Text(stringResource(R.string.goals_save))
            }
        },
        dismissButton = {
            Row {
                if (isEdit) {
                    TextButton(onClick = { confirmingDelete = true }) {
                        Text(stringResource(R.string.goals_delete), color = AppColors.BarRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.goals_cancel))
                }
            }
        },
    )
}
