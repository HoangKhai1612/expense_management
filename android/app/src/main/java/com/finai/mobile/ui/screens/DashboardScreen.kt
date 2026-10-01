package com.finai.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finai.mobile.ui.components.CategoryDot
import com.finai.mobile.ui.components.EmptyState
import com.finai.mobile.ui.components.LoadingBox
import com.finai.mobile.ui.formatMoney
import com.finai.mobile.ui.formatMoneySigned
import com.finai.mobile.ui.viewmodel.FinanceViewModel

@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    val state by viewModel.state.collectAsState()

    if (state.loading) {
        LoadingBox()
        return
    }

    val dashboard = state.dashboard
    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Text("This month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Income", dashboard?.monthIncome, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                SummaryCard("Expense", dashboard?.monthExpense, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Balance", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatMoney(dashboard?.monthBalance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    val summary = dashboard?.budgetSummary
                    if (summary != null && (summary.totalBudgets ?: 0) > 0) {
                        Text(
                            "${summary.totalBudgets} budgets · ${summary.warningBudgets ?: 0} warning · " +
                                "${summary.exceededBudgets ?: 0} exceeded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        val top = dashboard?.topSpendingCategories.orEmpty()
        if (top.isNotEmpty()) {
            item {
                Text("Top spending categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            items(top, key = { it.categoryId ?: it.hashCode().toLong() }) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryDot(entry.categoryColor)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        entry.categoryName.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${entry.percentage?.let { String.format(java.util.Locale.US, "%.0f", it) } ?: 0}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(formatMoney(entry.total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        val recent = dashboard?.recentTransactions.orEmpty()
        item {
            Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        if (recent.isEmpty()) {
            item {
                EmptyState(
                    "Nothing recorded yet",
                    "Add a transaction and your summary will fill in automatically.",
                )
            }
        } else {
            items(recent, key = { it.id ?: it.hashCode().toLong() }) { transaction ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryDot(transaction.category?.color)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            transaction.category?.name ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            transaction.note?.takeIf { it.isNotBlank() }
                                ?: transaction.transactionDate.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val isIncome = transaction.type == "INCOME"
                    Text(
                        formatMoneySigned(
                            (transaction.amount ?: 0.0) * if (isIncome) 1 else -1,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "All-time: ${formatMoneySigned(dashboard?.allTimeBalance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: Double?,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
            Text(formatMoney(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}