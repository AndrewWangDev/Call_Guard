package com.example.phoneblocker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.phoneblocker.data.local.entity.RegexRule
import com.example.phoneblocker.data.local.entity.RuleType
import java.util.regex.Pattern

@Composable
fun ConfigScreen(
    isBlockingEnabled: Boolean,
    onBlockingEnabledChange: (Boolean) -> Unit,
    regexRules: List<RegexRule>,
    onAddRule: (String, RuleType) -> Unit,
    onDeleteRule: (RegexRule) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRuleType by remember { mutableStateOf(RuleType.BLACKLIST) }

    val blacklistRules = remember(regexRules) { regexRules.filter { it.type == RuleType.BLACKLIST } }
    val whitelistRules = remember(regexRules) { regexRules.filter { it.type == RuleType.WHITELIST } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBlockingEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Call Blocking",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isBlockingEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = if (isBlockingEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isBlockingEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Switch(
                            checked = isBlockingEnabled,
                            onCheckedChange = onBlockingEnabledChange
                        )
                    }
                }
            }

            // Blacklist Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blacklist Rules (${blacklistRules.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = {
                        selectedRuleType = RuleType.BLACKLIST
                        showAddDialog = true
                    }) {
                        Text("+ Add Blacklist")
                    }
                }
            }

            if (blacklistRules.isEmpty()) {
                item {
                    EmptyRuleCard(message = "No blacklist rules configured. Calls will not be blocked.")
                }
            } else {
                items(
                    items = blacklistRules,
                    key = { "black_${it.id}" }
                ) { rule ->
                    RuleItem(rule = rule, onDeleteRule = onDeleteRule)
                }
            }

            // Whitelist Header
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Whitelist Rules (${whitelistRules.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = {
                        selectedRuleType = RuleType.WHITELIST
                        showAddDialog = true
                    }) {
                        Text("+ Add Whitelist")
                    }
                }
            }

            if (whitelistRules.isEmpty()) {
                item {
                    EmptyRuleCard(message = "No whitelist rules configured.")
                }
            } else {
                items(
                    items = whitelistRules,
                    key = { "white_${it.id}" }
                ) { rule ->
                    RuleItem(rule = rule, onDeleteRule = onDeleteRule)
                }
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { pattern, type ->
                onAddRule(pattern, type)
                showAddDialog = false
            },
            initialType = selectedRuleType
        )
    }
}

@Composable
private fun EmptyRuleCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleItem(
    rule: RegexRule,
    onDeleteRule: (RegexRule) -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = {
            if (it == DismissValue.DismissedToStart) {
                onDeleteRule(rule)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            val color by animateColorAsState(
                if (dismissState.targetValue == DismissValue.DismissedToStart) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Transparent
                },
                label = "SwipeBackground"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        dismissContent = {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rule.pattern,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteRule(rule) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete rule",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        directions = setOf(DismissDirection.EndToStart)
    )
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, RuleType) -> Unit,
    initialType: RuleType
) {
    var pattern by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validatePattern(input: String) {
        if (input.isBlank()) {
            errorMessage = "Pattern cannot be empty"
            return
        }
        try {
            if (input.startsWith("+")) {
                Pattern.compile("\\" + input)
            } else {
                Pattern.compile(input)
            }
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Invalid regex syntax"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rule") },
        text = {
            Column {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = {
                        pattern = it
                        validatePattern(it)
                    },
                    label = { Text("Regex Pattern (e.g. ^1\\d{2}.*)") },
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == RuleType.BLACKLIST,
                        onClick = { selectedType = RuleType.BLACKLIST }
                    )
                    Text("Blacklist")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = selectedType == RuleType.WHITELIST,
                        onClick = { selectedType = RuleType.WHITELIST }
                    )
                    Text("Whitelist")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pattern.isNotBlank() && errorMessage == null) {
                        onConfirm(pattern, selectedType)
                    }
                },
                enabled = pattern.isNotBlank() && errorMessage == null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
