package com.idestudio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.ui.theme.*
import java.io.File

data class EditorTab(
    val file: File,
    val isDirty: Boolean = false
) {
    val name: String
        get() = file.name
}

@Composable
fun FileTabs(
    tabs: List<EditorTab>,
    activeFile: File?,
    onSelectTab: (File) -> Unit,
    onCloseTab: (File) -> Unit
) {
    if (tabs.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(DarkSurface),
        contentPadding = PaddingValues(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs, key = { it.file.absolutePath }) { tab ->
            val isActive = tab.file.absolutePath == activeFile?.absolutePath

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(if (isActive) DarkBackground else DarkSurfaceElevated.copy(alpha = 0.5f))
                    .clickable { onSelectTab(tab.file) }
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dirty indicator (unsaved changes dot)
                if (tab.isDirty) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = tab.name,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive) TextPrimary else TextSecondary
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onCloseTab(tab.file) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close tab",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
