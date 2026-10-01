package com.finai.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finai.mobile.data.CategoryView
import com.finai.mobile.data.TransactionView
import com.finai.mobile.ui.components.CategoryDot
import com.finai.mobile.ui.components.EmptyState
import com.finai.mobile.ui.components.LoadingBox
import com.finai.mobile.ui.formatMoney
import com.finai.mobile.ui.viewmodel.FinanceViewModel
import java.time.LocalDate

@Composable
fun TransactionsScreen(viewModel: FinanceViewModel) {
    val state by viewModel.state.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TransactionView?>(null) }
    var pendingDelete by remember { mutableStateOf<TransactionView?>(null) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.typeFilter == null,
                onClick = { viewModel.setTypeFilter(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = state.typeFilter == "EXPENSE",
                onClick = { viewModel.setTypeFilter("EXPENSE") },
                label = { Text("Expense") },
            )
            FilterChip(
                selected = state.typeFilter == "INCOME",
                onClick = { viewModel.setTypeFilter("INCOME") },
                label = { Text("Income") },
            )
        }

        if (state.loading) {
            LoadingBox()
            return@Column
        }

        if (state.transactions.isEmpty()) {
            EmptyState(
                "No transactions",
                when (state.typeFilter) {
                    null -> "Record your first income or expense to see it here."
                    else -> "No ${state.typeFilter?.lowercase()} entries match this filter."
                },
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.transactions, key = { it.id ?: it.hashCode().toLong() }) { transaction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editing = transaction
                                showEditor = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryDot(transaction.category?.color)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                transaction.category?.name ?: "Unknown category",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                listOfNotNull(
                                    transaction.note?.takeIf { it.isNotBlank() },
                                    transaction.transactionDate,
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            formatMoney(transaction.amount),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (transaction.type == "INCOME") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        IconButton(onClick = { pendingDelete = transaction }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            if (state.transactionPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { viewModel.loadTransactions(state.transactionPage - 1) },
                        enabled = state.transactionPage > 0,
                    ) { Text("Previous") }
                    Text(
                        "Page ${state.transactionPage + 1} of ${state.transactionPages}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = { viewModel.loadTransactions(state.transactionPage + 1) },
                        enabled = state.transactionPage < state.transactionPages - 1,
                    ) { Text("Next") }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                editing = null
                showEditor = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add") },
            modifier = Modifier.padding(16.dp),
        )
    }

    if (showEditor) {
        TransactionEditorDialog(
            existing = editing,
            categories = state.categories,
            onDismiss = { showEditor = false },
            onSave = { categoryId, type, amount, note, date ->
                viewModel.saveTransaction(editing?.id, categoryId, type, amount, note, date) { error ->
                    if (error == null) showEditor = false
                }
            },
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this transaction?") },
            text = {
                Text(
                    "${target.category?.name ?: "This entry"} for " +
                        "${formatMoney(target.amount)} will be removed permanently.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    target.id?.let(viewModel::deleteTransaction)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TransactionEditorDialog(
    existing: TransactionView?,
    categories: List<CategoryView>,
    onDismiss: () -> Unit,
    onSave: (Long, String, Double, String?, String) -> Unit,
) {
    var type by remember { mutableStateOf(existing?.type ?: "EXPENSE") }
    val initialCategory = existing?.category?.id
    var categoryId by remember {
        mutableStateOf(
            initialCategory
                ?: categories.firstOrNull { it.type == (existing?.type ?: "EXPENSE") }?.id
                ?: categories.firstOrNull()?.id,
        )
    }
    var amount by remember { mutableStateOf(existing?.amount?.let { trimNumber(it) } ?: "") }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var date by remember { mutableStateOf(existing?.transactionDate ?: LocalDate.now().toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val matching = categories.filter { it.type == type }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add transaction" else "Edit transaction") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = {
                            type = "EXPENSE"
                            categoryId = matching.firstOrNull { it.type == "EXPENSE" }?.id ?: categoryId
                        },
                        label = { Text("Expense") },
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                            categoryId = categories.firstOrNull { it.type == "INCOME" }?.id ?: categoryId
                        },
                        label = { Text("Income") },
                    )
                }
                Spacer(Modifier.height(10.dp))

                if (matching.isEmpty()) {
                    Text(
                        "No $type categories are available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matching, key = { it.id ?: it.hashCode().toLong() }) { category ->
                            AssistChip(
                                onClick = { categoryId = category.id },
                                label = { Text(category.name.orEmpty()) },
                                leadingIcon = { CategoryDot(category.color) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (parsed == null || parsed <= 0.0) {
                    error = "Enter an amount greater than zero."
                } else if (categoryId == null) {
                    error = "Choose a category."
                } else {
                    error = null
                    onSave(categoryId!!, type, parsed, note.takeIf { it.isNotBlank() }, date)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Renders a stored amount without a locale decimal separator or trailing zeros. */
private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()