package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopControls(
    onReview: () -> Unit,
    onSaveDraft: () -> Unit,
    onResumeDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onReview) {
            Text("Review")
        }
        Button(onClick = onSaveDraft) {
            Text("Save Draft")
        }
        Button(onClick = onResumeDraft) {
            Text("Resume Draft")
        }
    }
}
