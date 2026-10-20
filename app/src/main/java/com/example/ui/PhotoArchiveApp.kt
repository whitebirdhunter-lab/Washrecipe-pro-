package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CategoryEntity
import com.example.data.PhotoEntity
import com.example.ui.components.CategoryCardsSection
import com.example.ui.components.EditCategoryDialog
import com.example.ui.components.EditPhotoDialog
import com.example.ui.components.FullScreenPhotoViewer
import com.example.ui.components.PhotoGrid
import com.example.ui.components.SearchBarWithFilters
import com.example.ui.components.StorageInfoDialog
import com.example.ui.components.UploadPhotoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoArchiveApp(
    viewModel: PhotoArchiveViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Multi-photo picker for main FAB
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onPhotosSelected(uris)
        }
    }

    // Category-specific photo picker
    val categoryPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onPhotosSelected(uris, defaultCategory = uiState.uploadTargetCategory)
        }
    }

    // Handle notifications / feedback
    LaunchedEffect(uiState.notification) {
        uiState.notification?.let { notif ->
            snackbarHostState.showSnackbar(notif.message)
            viewModel.clearNotification()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "WashRecipe Pro",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.photos.size} recipes in vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Demo Photos generator for quick emulator testing
                        IconButton(
                            onClick = { viewModel.addDemoPhotos() },
                            modifier = Modifier.testTag("add_sample_demo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Add demo photos",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Info button
                        IconButton(
                            onClick = { viewModel.openStorageInfo() },
                            modifier = Modifier.testTag("top_app_bar_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Storage information"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        // Open file picker or upload dialog
                        viewModel.openUploadDialog(
                            targetCategory = uiState.selectedCategoryFilter ?: 1
                        )
                    },
                    icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                    text = { Text("Upload Recipe") },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier.testTag("main_upload_fab")
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Search Bar & Filter Chips
                SearchBarWithFilters(
                    searchQuery = uiState.searchQuery,
                    onQueryChanged = viewModel::onSearchQueryChanged,
                    onClearQuery = viewModel::clearSearchQuery,
                    categories = uiState.categories,
                    selectedCategoryFilter = uiState.selectedCategoryFilter,
                    onCategoryFilterSelected = viewModel::selectCategoryFilter,
                    sortOrder = uiState.sortOrder,
                    onSortOrderSelected = viewModel::setSortOrder,
                    onOpenStorageInfo = viewModel::openStorageInfo
                )

                // 2. Large Category Cards (Shown unless user is deep in a search query or prefers collapsed)
                CategoryCardsSection(
                    categories = uiState.categories,
                    allPhotos = uiState.photos,
                    selectedCategoryFilter = uiState.selectedCategoryFilter,
                    onSelectCategory = { catId ->
                        // Toggle filter
                        if (uiState.selectedCategoryFilter == catId) {
                            viewModel.selectCategoryFilter(null)
                        } else {
                            viewModel.selectCategoryFilter(catId)
                        }
                    },
                    onEditCategory = viewModel::startEditingCategory,
                    onAddPhotosToCategory = { catId ->
                        viewModel.openUploadDialog(targetCategory = catId)
                    }
                )

                // 3. Grid of photos
                PhotoGrid(
                    photos = uiState.photos,
                    categories = uiState.categories,
                    searchQuery = uiState.searchQuery,
                    selectedCategoryFilter = uiState.selectedCategoryFilter,
                    onPhotoClick = viewModel::openPhotoViewer,
                    onDownloadPhoto = viewModel::downloadPhoto,
                    onPrintPhoto = viewModel::printPhoto,
                    onEditPhoto = viewModel::startEditingPhoto,
                    onDeletePhoto = viewModel::promptDeletePhoto,
                    onUploadClick = { viewModel.openUploadDialog(targetCategory = uiState.selectedCategoryFilter ?: 1) },
                    onAddDemoClick = viewModel::addDemoPhotos,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
                    modifier = Modifier.weight(1f)
                )

                // Elegant Developer Credit at bottom of home screen
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, top = 2.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        text = "Developed by Naim",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("developer_credit_footer")
                    )
                }
            }
        }

        // Full Screen Lightbox Viewer
        uiState.activeViewerPhotoIndex?.let { viewerIndex ->
            FullScreenPhotoViewer(
                photos = uiState.photos,
                currentIndex = viewerIndex,
                categories = uiState.categories,
                onClose = viewModel::closePhotoViewer,
                onNavigateNext = viewModel::navigateViewerNext,
                onNavigatePrevious = viewModel::navigateViewerPrevious,
                onDownload = viewModel::downloadPhoto,
                onPrint = viewModel::printPhoto,
                onEdit = viewModel::startEditingPhoto,
                onDelete = viewModel::promptDeletePhoto
            )
        }

        // Upload Photos Dialog
        if (uiState.showUploadDialog) {
            UploadPhotoDialog(
                initialUris = uiState.pendingUploadUris,
                initialCategory = uiState.uploadTargetCategory,
                categories = uiState.categories,
                isUploading = uiState.isUploading,
                uploadProgress = uiState.uploadProgress,
                uploadStatusText = uiState.uploadStatusText,
                onDismiss = viewModel::dismissUploadDialog,
                onConfirmUpload = { uris, name, date, notes, categoryId ->
                    viewModel.confirmUpload(uris, name, date, notes, categoryId)
                }
            )
        }

        // Edit Photo Dialog
        uiState.editingPhoto?.let { photo ->
            EditPhotoDialog(
                photo = photo,
                categories = uiState.categories,
                onDismiss = viewModel::dismissEditingPhoto,
                onSave = { newName, newDate, newNotes, newCategoryId ->
                    viewModel.savePhotoEdit(photo, newName, newDate, newNotes, newCategoryId)
                }
            )
        }

        // Edit Category Dialog
        uiState.editingCategory?.let { category ->
            EditCategoryDialog(
                category = category,
                onDismiss = viewModel::dismissEditingCategory,
                onSave = { newName, newDescription ->
                    viewModel.saveCategoryEdit(category, newName, newDescription, category.colorHex)
                }
            )
        }

        // Delete Photo Confirmation Dialog
        uiState.photoToDelete?.let { photo ->
            AlertDialog(
                onDismissRequest = viewModel::dismissDeletePrompt,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("Delete Photo?") },
                text = {
                    Text(
                        "Are you sure you want to permanently delete \"${photo.name}\"? This will remove the file from your local archive storage."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeletePhoto(photo) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Delete Permanently")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::dismissDeletePrompt,
                        modifier = Modifier.testTag("cancel_delete_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Storage Info Dialog
        if (uiState.showStorageInfoDialog) {
            StorageInfoDialog(
                totalPhotos = uiState.totalUnfilteredCount,
                storageSizeBytes = viewModel.calculateTotalStorageSize(),
                categories = uiState.categories,
                onEditCategory = viewModel::startEditingCategory,
                onDismiss = viewModel::dismissStorageInfo
            )
        }
    }
}
