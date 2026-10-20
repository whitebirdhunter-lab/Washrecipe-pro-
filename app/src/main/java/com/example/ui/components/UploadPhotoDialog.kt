package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.CategoryEntity
import com.example.util.PhotoFileUtils

@Composable
fun UploadPhotoDialog(
    initialUris: List<Uri>,
    initialCategory: Int,
    categories: List<CategoryEntity>,
    isUploading: Boolean,
    uploadProgress: Float,
    uploadStatusText: String?,
    onDismiss: () -> Unit,
    onConfirmUpload: (uris: List<Uri>, name: String, date: String, notes: String, categoryId: Int) -> Unit
) {
    var selectedUris by remember(initialUris) { mutableStateOf(initialUris) }
    var photoName by remember { mutableStateOf("") }
    var photoDate by remember { mutableStateOf(PhotoFileUtils.getTodayDateString()) }
    var photoNotes by remember { mutableStateOf("") }
    var selectedCategory by remember(initialCategory) { mutableIntStateOf(initialCategory) }

    // Launcher for selecting multiple images from gallery / file manager
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
        }
    }

    val cat1 = categories.find { it.id == 1 } ?: CategoryEntity(1, "Denim Recipes")
    val cat2 = categories.find { it.id == 2 } ?: CategoryEntity(2, "Dyeing Recipes")

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedUris.size > 1) "Upload ${selectedUris.size} Recipe Photos" else "Add Wash Recipe Photo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Previews or Pick Images button
                if (selectedUris.isEmpty()) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("pick_images_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Pictures from Gallery / Files")
                    }
                } else {
                    Text(
                        text = "Selected (${selectedUris.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedUris.take(6).forEach { uri ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(72.dp),
                                tonalElevation = 2.dp
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                        if (selectedUris.size > 6) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "+${selectedUris.size - 6}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Change Selection")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vault / Category selector chips
                Text(
                    text = "Archive Vault / Category:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == 1,
                        onClick = { selectedCategory = 1 },
                        label = { Text(cat1.name) },
                        leadingIcon = {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("upload_choose_cat_1")
                    )

                    FilterChip(
                        selected = selectedCategory == 2,
                        onClick = { selectedCategory = 2 },
                        label = { Text(cat2.name) },
                        leadingIcon = {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("upload_choose_cat_2")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Photo Name
                OutlinedTextField(
                    value = photoName,
                    onValueChange = { photoName = it },
                    label = { Text("Recipe Title / Name") },
                    placeholder = {
                        Text(if (selectedUris.size > 1) "e.g. Vintage Bleach Stone #104 (auto numbered)" else "e.g. Enzyme Sky Fade Wash")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Field (with Today Quick Button)
                OutlinedTextField(
                    value = photoDate,
                    onValueChange = { photoDate = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { photoDate = PhotoFileUtils.getTodayDateString() },
                            modifier = Modifier.testTag("set_today_date_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "Set today's date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Field
                OutlinedTextField(
                    value = photoNotes,
                    onValueChange = { photoNotes = it },
                    label = { Text("Chemical Formula & Wash Steps") },
                    placeholder = { Text("e.g. Cellulase 1.5 g/L, NaOCl 2.0 g/L, 45 min at 50°C, liquor ratio 1:8...") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Upload Progress Indicator
                AnimatedVisibility(visible = isUploading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uploadStatusText ?: "Saving original quality recipe photo...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmUpload(
                        selectedUris,
                        photoName,
                        photoDate,
                        photoNotes,
                        selectedCategory
                    )
                },
                enabled = selectedUris.isNotEmpty() && !isUploading,
                modifier = Modifier.testTag("confirm_upload_button")
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save to Vault")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading,
                modifier = Modifier.testTag("cancel_upload_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
