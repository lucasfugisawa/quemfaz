package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme

@Composable
fun BlockedUserScreen(
    onContactSupport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(Strings.BlockedUser.TITLE, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            Strings.BlockedUser.MESSAGE,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onContactSupport) {
            Text(Strings.BlockedUser.CONTACT_SUPPORT)
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun BlockedUserPreview() {
    AppTheme { BlockedUserScreen(onContactSupport = {}) }
}
