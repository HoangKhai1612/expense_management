package com.finai.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finai.mobile.ui.viewmodel.FinanceViewModel

private val suggestions = listOf(
    "How much did I spend this month?",
    "Where did I spend the most?",
    "Am I over budget?",
    "How does this month compare to last month?",
)

/**
 * The assistant conversation.
 *
 * The rendered answer preserves the backend's FACT / SUGGESTION split rather than
 * flattening it, because that separation is what lets a user tell a measured
 * figure from advice.
 */
@Composable
fun AiChatScreen(viewModel: FinanceViewModel) {
    val state by viewModel.state.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Financial assistant", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (state.aiThinking) "Thinking…" else "Answers only from your own records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = viewModel::startNewConversation) { Text("New chat") }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Text(
                        "Ask about your income, spending or budgets. The assistant reports only " +
                            "figures it can verify from your transactions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    suggestions.forEach { suggestion ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { viewModel.askAi(suggestion) {} },
                        ) {
                            Text(
                                suggestion,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            items(state.messages, key = { it.id ?: it.hashCode().toLong() }) { message ->
                MessageBubble(
                    isUser = message.role == "USER",
                    content = message.content.orEmpty(),
                    hasFacts = message.hasGroundingFacts == true,
                )
            }

            if (state.aiThinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(18.dp).widthIn(min = 18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Checking your records…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Ask about your spending…") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    val question = draft
                    draft = ""
                    viewModel.askAi(question) {}
                },
                enabled = draft.isNotBlank() && !state.aiThinking,
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, content: String, hasFacts: Boolean) {
    val background = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(Modifier.fillMaxWidth()) {
        Surface(
            color = background,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(content, style = MaterialTheme.typography.bodyMedium)
                if (!isUser && hasFacts) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Based on your stored records",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}