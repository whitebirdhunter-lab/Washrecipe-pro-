package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.data.PhotoEntity

@Composable
fun CategoryCardsSection(
    categories: List<CategoryEntity>,
    allPhotos: List<PhotoEntity>,
    selectedCategoryFilter: Int?,
    onSelectCategory: (Int) -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onAddPhotosToCategory: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cat1 = categories.find { it.id == 1 } ?: CategoryEntity(1, "Denim Recipes")
    val cat2 = categories.find { it.id == 2 } ?: CategoryEntity(2, "Dyeing Recipes")

    val countCat1 = allPhotos.count { it.categoryId == 1 }
    val countCat2 = allPhotos.count { it.categoryId == 2 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recipe Vaults",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Denim Washing & Dyeing",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category 1 Card
            CategoryCard(
                category = cat1,
                photoCount = countCat1,
                isSelected = selectedCategoryFilter == 1,
                isPrimaryStyle = true,
                onCardClick = { onSelectCategory(1) },
                onEditClick = { onEditCategory(cat1) },
                onAddClick = { onAddPhotosToCategory(1) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("category_card_1")
            )

            // Category 2 Card
            CategoryCard(
                category = cat2,
                photoCount = countCat2,
                isSelected = selectedCategoryFilter == 2,
                isPrimaryStyle = false,
                onCardClick = { onSelectCategory(2) },
                onEditClick = { onEditCategory(cat2) },
                onAddClick = { onAddPhotosToCategory(2) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("category_card_2")
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryEntity,
    photoCount: Int,
    isSelected: Boolean,
    isPrimaryStyle: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        if (isPrimaryStyle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val borderColor = if (isSelected) {
        if (isPrimaryStyle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier.clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Category Icon & Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPrimaryStyle) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPrimaryStyle) Icons.Default.Folder else Icons.Default.FolderSpecial,
                            contentDescription = "Category icon",
                            tint = if (isPrimaryStyle) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_category_${category.id}_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename ${category.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Name
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Photo Count
            Text(
                text = if (photoCount == 1) "1 wash recipe" else "$photoCount wash recipes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Add Photos Button
            FilledTonalButton(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_photos_cat_${category.id}_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isPrimaryStyle) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    contentColor = if (isPrimaryStyle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ Add Recipe",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
