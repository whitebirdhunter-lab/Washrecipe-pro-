package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.ui.PhotoSortOrder

@Composable
fun SearchBarWithFilters(
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    categories: List<CategoryEntity>,
    selectedCategoryFilter: Int?,
    onCategoryFilterSelected: (Int?) -> Unit,
    sortOrder: PhotoSortOrder,
    onSortOrderSelected: (PhotoSortOrder) -> Unit,
    onOpenStorageInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Main Search Field Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_text_input"),
                    placeholder = {
                        Text(
                            "Search recipes, formulas, date (2026-10-20), year...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = onClearQuery,
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort photos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest Date First") },
                            onClick = {
                                onSortOrderSelected(PhotoSortOrder.NEWEST_DATE)
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOrder == PhotoSortOrder.NEWEST_DATE) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest Date First") },
                            onClick = {
                                onSortOrderSelected(PhotoSortOrder.OLDEST_DATE)
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOrder == PhotoSortOrder.OLDEST_DATE) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (A to Z)") },
                            onClick = {
                                onSortOrderSelected(PhotoSortOrder.NAME_ASC)
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOrder == PhotoSortOrder.NAME_ASC) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (Z to A)") },
                            onClick = {
                                onSortOrderSelected(PhotoSortOrder.NAME_DESC)
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOrder == PhotoSortOrder.NAME_DESC) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }

                // Storage Info Button
                IconButton(
                    onClick = onOpenStorageInfo,
                    modifier = Modifier.testTag("storage_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Storage and archive info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { onCategoryFilterSelected(null) },
                    label = { Text("All Recipes") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_all_chip")
                )

                val cat1 = categories.find { it.id == 1 } ?: CategoryEntity(1, "Denim Recipes")
                val cat2 = categories.find { it.id == 2 } ?: CategoryEntity(2, "Dyeing Recipes")

                FilterChip(
                    selected = selectedCategoryFilter == 1,
                    onClick = { onCategoryFilterSelected(if (selectedCategoryFilter == 1) null else 1) },
                    label = { Text(cat1.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_cat1_chip")
                )

                FilterChip(
                    selected = selectedCategoryFilter == 2,
                    onClick = { onCategoryFilterSelected(if (selectedCategoryFilter == 2) null else 2) },
                    label = { Text(cat2.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("filter_cat2_chip")
                )
            }
        }
    }
}
