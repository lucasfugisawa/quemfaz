package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun BlockedUserScreen(
    onContactSupport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screenEdge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            modifier = Modifier.size(Spacing.emptyStateIconSize),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            Strings.BlockedUser.TITLE,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            Strings.BlockedUser.MESSAGE,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.sectionGap))
        Button(
            onClick = onContactSupport,
            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(Strings.BlockedUser.CONTACT_SUPPORT, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun BlockedUserPreview() {
    AppTheme { BlockedUserScreen(onContactSupport = {}) }
}
