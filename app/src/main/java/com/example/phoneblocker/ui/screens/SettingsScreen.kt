package com.example.phoneblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    isBlockingEnabled: Boolean,
    onBlockingEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Enable Blocking", style = MaterialTheme.typography.titleMedium)
                Text(text = "Master switch for all rules", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = isBlockingEnabled,
                onCheckedChange = onBlockingEnabledChange
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(text = "Manage Rules", style = MaterialTheme.typography.titleMedium)
        Text(text = "Not implemented in this overview", style = MaterialTheme.typography.bodyMedium)
    }
}
