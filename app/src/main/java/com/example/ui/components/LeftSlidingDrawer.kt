package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppMode
import com.example.data.model.WorkspaceTab
import com.example.ui.theme.*

@Composable
fun LeftSlidingDrawerContent(
    currentMode: AppMode,
    currentWorkspaceTab: WorkspaceTab?,
    isDarkMode: Boolean,
    onModeSelected: (AppMode) -> Unit,
    onWorkspaceTabSelected: (WorkspaceTab) -> Unit,
    onToggleTheme: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            // App Branding Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentCyanBright.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = AccentCyanBright,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Infinity Agent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Autonomous AI IDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyanBright,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                // MODES SECTION
                item {
                    Text(
                        text = "MODES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                item {
                    DrawerNavPill(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "Chat",
                        subtitle = "Conversational coding",
                        isSelected = currentMode == AppMode.CHAT && currentWorkspaceTab == null,
                        accentColor = AccentCyanBright,
                        onClick = {
                            onModeSelected(AppMode.CHAT)
                            onCloseDrawer()
                        }
                    )
                }

                item {
                    DrawerNavPill(
                        icon = Icons.Default.Code,
                        title = "Coder",
                        subtitle = "Multi-language workbench",
                        isSelected = currentMode == AppMode.CODER && currentWorkspaceTab == null,
                        accentColor = AccentIndigo,
                        onClick = {
                            onModeSelected(AppMode.CODER)
                            onCloseDrawer()
                        }
                    )
                }

                item {
                    DrawerNavPill(
                        icon = Icons.Default.SmartToy,
                        title = "Agent",
                        subtitle = "Analyze → Plan → Code → Apply",
                        isSelected = currentMode == AppMode.AGENT && currentWorkspaceTab == null,
                        accentColor = AccentPurple,
                        onClick = {
                            onModeSelected(AppMode.AGENT)
                            onCloseDrawer()
                        }
                    )
                }

                item {
                    DrawerNavPill(
                        icon = Icons.Default.Hub,
                        title = "Multi-Agent",
                        subtitle = "5 Models collaborative swarm",
                        isSelected = currentMode == AppMode.MULTI_AGENT && currentWorkspaceTab == null,
                        accentColor = AccentPink,
                        onClick = {
                            onModeSelected(AppMode.MULTI_AGENT)
                            onCloseDrawer()
                        }
                    )
                }

                // WORKSPACE SECTION
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "WORKSPACE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                item {
                    DrawerNavPill(
                        icon = Icons.Default.FolderOpen,
                        title = "Projects",
                        subtitle = "Java, C++, XML",
                        isSelected = currentWorkspaceTab == WorkspaceTab.PROJECTS,
                        accentColor = AccentAmber,
                        onClick = {
                            onWorkspaceTabSelected(WorkspaceTab.PROJECTS)
                            onCloseDrawer()
                        }
                    )
                }

                item {
                    DrawerNavPill(
                        icon = Icons.Default.Description,
                        title = "Files",
                        subtitle = "Local & project file tree",
                        isSelected = currentWorkspaceTab == WorkspaceTab.FILES,
                        accentColor = AccentGreen,
                        onClick = {
                            onWorkspaceTabSelected(WorkspaceTab.FILES)
                            onCloseDrawer()
                        }
                    )
                }

                item {
                    DrawerNavPill(
                        icon = Icons.Default.History,
                        title = "History",
                        subtitle = "Past chats & agent logs",
                        isSelected = currentWorkspaceTab == WorkspaceTab.HISTORY,
                        accentColor = AccentCyan,
                        onClick = {
                            onWorkspaceTabSelected(WorkspaceTab.HISTORY)
                            onCloseDrawer()
                        }
                    )
                }
            }

            // Bottom Settings Section
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleTheme() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = if (isDarkMode) AccentPurple else AccentAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isDarkMode) "Dark Theme" else "Light Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleTheme() },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentCyanBright)
                )
            }
        }
    }
}

@Composable
private fun DrawerNavPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor)
                )
            }
        }
    }
}
