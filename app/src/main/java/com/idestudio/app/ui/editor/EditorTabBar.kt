package com.idestudio.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.app.data.model.EditorTab
import com.idestudio.app.ui.theme.CardStroke
import com.idestudio.app.ui.theme.PurplePrimary
import com.idestudio.app.ui.theme.TextPrimary
import com.idestudio.app.ui.theme.TextSecondary

@Composable
fun EditorTabBar(
    tabs: List<EditorTab>,
    activeTabId: String?,
    onSelectTab: (EditorTab) -> Unit,
    onCloseTab: (EditorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF1F3F4),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardStroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val bgColor = if (isActive) Color.White else Color(0xFFE8EAED)

                Box(
                    modifier = Modifier
                        .background(bgColor)
                        .clickable { onSelectTab(tab) }
                        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.title + if (tab.isModified) " •" else "",
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) PurplePrimary else TextPrimary
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onCloseTab(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Active underline
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(PurplePrimary)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }

                // Vertical separator between tabs
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(CardStroke)
                )
            }
        }
    }
}
