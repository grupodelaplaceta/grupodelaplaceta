package org.laplaceta.placetaid.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.laplaceta.placetaid.ui.theme.*

/**
 * Main screen with bottom navigation tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentTab: String,
    onTabChange: (String) -> Unit,
    onAddPlacetaID: () -> Unit,
    placetaidsContent: @Composable () -> Unit,
    requestsContent: @Composable () -> Unit,
    historyContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    documentsContent: @Composable () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PlacetaID Móvil",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Autenticación segura",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PlacetaidPrimary
                ),
                actions = {
                    if (currentTab == "placetaids") {
                        IconButton(onClick = onAddPlacetaID) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Añadir PlacetaID",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "placetaids",
                    onClick = { onTabChange("placetaids") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Identidades", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PlacetaidPrimary,
                        selectedTextColor = PlacetaidPrimary,
                        indicatorColor = PlacetaidInfoSoft
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "requests",
                    onClick = { onTabChange("requests") },
                    icon = {
                        BadgedBox(badge = {
                            // Badge count would be dynamic
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text("Solicitudes", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PlacetaidPrimary,
                        selectedTextColor = PlacetaidPrimary,
                        indicatorColor = PlacetaidInfoSoft
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "documents",
                    onClick = { onTabChange("documents") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Documentos", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PlacetaidPrimary,
                        selectedTextColor = PlacetaidPrimary,
                        indicatorColor = PlacetaidInfoSoft
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "history",
                    onClick = { onTabChange("history") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Historial", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PlacetaidPrimary,
                        selectedTextColor = PlacetaidPrimary,
                        indicatorColor = PlacetaidInfoSoft
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { onTabChange("settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PlacetaidPrimary,
                        selectedTextColor = PlacetaidPrimary,
                        indicatorColor = PlacetaidInfoSoft
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PlacetaidBackground)
        ) {
            when (currentTab) {
                "placetaids" -> placetaidsContent()
                "requests" -> requestsContent()
                "history" -> historyContent()
                "settings" -> settingsContent()
            }
        }
    }
}
