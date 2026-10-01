package com.finai.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finai.mobile.data.CategoryView
import com.finai.mobile.ui.components.EmptyState
import com.finai.mobile.ui.components.StatusPill
import com.finai.mobile.ui.components.parseColor
import com.finai.mobile.ui.formatMoney
import com.finai.mobile.ui.formatPercent
import com.finai.mobile.ui.viewmodel.FinanceViewModel

@Composable
fun BudgetsScreen(viewModel: FinanceViewModel) {
    val state by viewModel.state.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (state.budgets.isEmpty()) {
        EmptyState(
            "No budgets yet",
            "Set a monthly limit and the app will warn you before you overspend.",
        )
    } else {
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            items(state.budgets, key = { it.id ?: it.hashCode().toLong() }) { budget ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                budget.categoryName.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            BudgetStatusPill(budget.status.orEmpty())
                            IconButton(onClick = { budget.id?.let(viewModel::deleteBudget) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete budget",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ((budget.usagePercentage ?: 0.0) / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = statusColor(budget.status.orEmpty()),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${formatMoney(budget.usedAmount)} of ${formatMoney(budget.amount)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                formatPercent(budget.usagePercentage),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        val remaining = budget.remainingAmount
                        if (remaining != null) {
                            Text(
                                if (remaining >= 0) {
                                    "${formatMoney(remaining)} left"
                                } else {
                                    "Over by ${formatMoney(-remaining)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (remaining >= 0) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    Button(
        onClick = { showEditor = true },
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.height(4.dp))
        Text("  Set a budget")
    }

    if (showEditor) {
        val expenseCategories = state.categories.filter { it.type == "EXPENSE" }
        var categoryId by remember { mutableStateOf(expenseCategories.firstOrNull()?.id) }
        var amount by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("Monthly budget") },
            text = {
                Column {
                    if (expenseCategories.isEmpty()) {
                        Text("No expense categories are available.")
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(expenseCategories, key = { it.id ?: it.hashCode().toLong() }) { category ->
                                androidx.compose.material3.AssistChip(
                                    onClick = { categoryId = category.id },
                                    label = { Text(category.name.orEmpty()) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Limit") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (localError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            localError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = amount.toDoubleOrNull()
                    when {
                        parsed == null || parsed <= 0.0 -> localError = "Enter an amount greater than zero."
                        categoryId == null -> localError = "Choose a category."
                        else -> {
                            localError = null
                            viewModel.saveBudget(null, categoryId!!, parsed) { message ->
                                if (message == null) showEditor = false else localError = message
                            }
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
        )
    }

    if (error != null) {
        Box(Modifier.padding(16.dp)) { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun BudgetStatusPill(status: String) {
    when (status) {
        "EXCEEDED" -> StatusPill("EXCEEDED", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        "WARNING" -> StatusPill("WARNING", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> StatusPill("SAFE", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun statusColor(status: String): Color = when (status) {
    "EXCEEDED" -> Color(0xFFDC2626)
    "WARNING" -> Color(0xFFF59E0B)
    else -> Color(0xFF22C55E)
}