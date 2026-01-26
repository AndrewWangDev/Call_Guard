package com.example.phoneblocker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.phoneblocker.data.local.entity.RegexRule
import com.example.phoneblocker.data.local.entity.RuleType

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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Master Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBlockingEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                            color = if (isBlockingEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBlockingEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isBlockingEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isBlockingEnabled,
                        onCheckedChange = onBlockingEnabledChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rules Lists
            Text("Blacklist Rules", style = MaterialTheme.typography.titleMedium)
            RuleList(
                rules = regexRules.filter { it.type == RuleType.BLACKLIST },
                onDeleteRule = onDeleteRule
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Whitelist Rules", style = MaterialTheme.typography.titleMedium)
            RuleList(
                rules = regexRules.filter { it.type == RuleType.WHITELIST },
                onDeleteRule = onDeleteRule
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleList(
    rules: List<RegexRule>,
    onDeleteRule: (RegexRule) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 200.dp) // Limit height or use weight in a flexible layout
    ) {
        items(
            items = rules,
            key = { it.id }
        ) { rule ->
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
                        if (dismissState.targetValue == DismissValue.DismissedToStart) Color.Red else Color.Transparent,
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
                            tint = Color.White
                        )
                    }
                },
                dismissContent = {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Text(text = rule.pattern, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                },
                directions = setOf(DismissDirection.EndToStart)
            )
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, RuleType) -> Unit,
    initialType: RuleType
) {
    var pattern by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rule") },
        text = {
            Column {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex Pattern") }
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
                    if (pattern.isNotBlank()) {
                        onConfirm(pattern, selectedType)
                    }
                }
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
