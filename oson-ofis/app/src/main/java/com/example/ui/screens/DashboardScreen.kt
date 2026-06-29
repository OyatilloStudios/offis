package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFile
import com.example.util.UriMetadataHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    recentFiles: List<RecentFile>,
    favoriteFiles: List<RecentFile>,
    onOpenFile: (Uri) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Yaqinda ochilganlar, 1: Saralanganlar
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Barchasi") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                // Persist Uri permission so we can read it later in Recent Files if needed!
                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    // Ignore if cannot persist (e.g. not a document provider Uri)
                }
                onOpenFile(it)
            }
        }
    )

    val activeList = if (selectedTab == 0) recentFiles else favoriteFiles
    val filteredList = remember(activeList, searchQuery, selectedCategory) {
        activeList.filter { file ->
            val matchesSearch = file.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "Barchasi" -> true
                "PDF" -> file.extension == "pdf"
                "Word" -> file.extension == "docx"
                "Excel" -> file.extension == "xlsx" || file.extension == "csv"
                "Matn" -> file.extension != "pdf" && file.extension != "docx" && file.extension != "xlsx" && file.extension != "csv"
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        containerColor = Color(0xFFF7F9FC),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Oson Ofis",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF041E49)
                        )
                        Text(
                            text = "Tezkor va yengil hujjat ko'rish",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5F6368)
                        )
                    }
                },
                actions = {
                    if (recentFiles.isNotEmpty() && selectedTab == 0) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Tarixni tozalash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color(0xFFF7F9FC)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    filePicker.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "text/plain",
                            "text/csv",
                            "text/html"
                        )
                    )
                },
                icon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color(0xFF041E49)) },
                text = { Text("Faylni Ochish", color = Color(0xFF041E49), fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFFD3E3FD),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("open_file_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar matching the HTML template layout and colors
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(1.dp, CircleShape),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE9EEF6)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF5F6368),
                        modifier = Modifier.padding(start = 6.dp).size(20.dp)
                    )
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Fayllarni qidirish...", color = Color(0xFF70757A)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF1F1F1F),
                            unfocusedTextColor = Color(0xFF1F1F1F)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input"),
                        singleLine = true
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Tozalash",
                                tint = Color(0xFF5F6368)
                            )
                        }
                    }

                    // Colored user avatar bubble with "O" for "oyatillostudios01"
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(32.dp)
                            .background(Color(0xFF0A58CA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "O",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Category Selector Chips styled precisely after the HTML mockup
            val categories = listOf("Barchasi", "PDF", "Word", "Excel", "Matn")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = category },
                        color = if (isSelected) Color(0xFFD3E3FD) else Color.White,
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFD0D7DE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color(0xFF041E49) else Color(0xFF424242),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Tab Selector styled beautifully using Sleek styling
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF0A58CA),
                        height = 3.dp
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            text = "Yaqinda ochilganlar",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color(0xFF041E49) else Color(0xFF5F6368)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            text = "Saralanganlar",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color(0xFF041E49) else Color(0xFF5F6368)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File List / Empty State
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        isSearching = searchQuery.isNotEmpty() || selectedCategory != "Barchasi",
                        tabIndex = selectedTab
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.uriString }) { file ->
                        FileItemRow(
                            file = file,
                            onOpen = { onOpenFile(Uri.parse(file.uriString)) },
                            onToggleFav = { onToggleFavorite(file.uriString, !file.isFavorite) },
                            onDelete = { onDeleteRecent(file.uriString) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isSearching: Boolean, tabIndex: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        // Aesthetic vector document representation using Compose shapes
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val title = if (isSearching) {
            "Hujjatlar topilmadi"
        } else if (tabIndex == 0) {
            "Yaqinda ochilgan fayllar yo'q"
        } else {
            "Saralangan fayllar yo'q"
        }

        val subtitle = if (isSearching) {
            "Qidiruv so'rovingizga yoki tanlangan kategoriyaga mos hujjatlar mavjud emas."
        } else if (tabIndex == 0) {
            "Pastdagi 'Faylni Ochish' tugmasini bosib telefoningizdan istalgan ofis yoki matn faylini yengilgina oching."
        } else {
            "Tez-tez ishlatadigan hujjatlaringizni yulduzcha belgisi bilan saralanganlarga qo'shing."
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: RecentFile,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val iconColorAndSymbol = remember(file.extension) {
        when (file.extension.lowercase()) {
            "pdf" -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), Icons.Default.PictureAsPdf)
            "docx" -> Triple(Color(0xFFDBEAFE), Color(0xFF3B82F6), Icons.Default.Description)
            "xlsx", "csv" -> Triple(Color(0xFFD1FAE5), Color(0xFF10B981), Icons.Default.TableChart)
            else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), Icons.Default.Article)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { showMenu = true }
            )
            .testTag("file_item_${file.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Icon with background matching the "Sleek Interface" HTML design specs
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconColorAndSymbol.first,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconColorAndSymbol.third,
                    contentDescription = null,
                    tint = iconColorAndSymbol.second,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // File Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.extension.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColorAndSymbol.first
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = UriMetadataHelper.formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatTime(file.lastOpened),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row {
                IconButton(onClick = onToggleFav) {
                    Icon(
                        imageVector = if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Saralash",
                        tint = if (file.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Batafsil"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hujjatni ko'rish") },
                            onClick = {
                                showMenu = false
                                onOpen()
                            },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Saralash") },
                            onClick = {
                                showMenu = false
                                onToggleFav()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null
                                )
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Tarixdan o'chirish", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}
