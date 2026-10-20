package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CategoryEntity
import com.example.data.PhotoEntity
import com.example.data.PhotoRepository
import com.example.util.PhotoFileUtils
import com.example.util.PrintHelperUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class PhotoSortOrder {
    NEWEST_DATE,
    OLDEST_DATE,
    NAME_ASC,
    NAME_DESC
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

data class ArchiveUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val totalUnfilteredCount: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryFilter: Int? = null, // null for All, 1 for Cat 1, 2 for Cat 2
    val searchQuery: String = "",
    val sortOrder: PhotoSortOrder = PhotoSortOrder.NEWEST_DATE,
    val activeViewerPhotoIndex: Int? = null, // Index in filtered photos list for full-screen viewer
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadStatusText: String? = null,
    val editingPhoto: PhotoEntity? = null,
    val editingCategory: CategoryEntity? = null,
    val photoToDelete: PhotoEntity? = null,
    val showUploadDialog: Boolean = false,
    val uploadTargetCategory: Int = 1,
    val pendingUploadUris: List<Uri> = emptyList(),
    val showStorageInfoDialog: Boolean = false,
    val notification: UiNotification? = null
)

class PhotoArchiveViewModel(
    application: Application,
    private val repository: PhotoRepository
) : AndroidViewModel(application) {

    private val _selectedCategoryFilter = MutableStateFlow<Int?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(PhotoSortOrder.NEWEST_DATE)
    private val _activeViewerPhotoIndex = MutableStateFlow<Int?>(null)
    private val _isUploading = MutableStateFlow(false)
    private val _uploadProgress = MutableStateFlow(0f)
    private val _uploadStatusText = MutableStateFlow<String?>(null)
    private val _editingPhoto = MutableStateFlow<PhotoEntity?>(null)
    private val _editingCategory = MutableStateFlow<CategoryEntity?>(null)
    private val _photoToDelete = MutableStateFlow<PhotoEntity?>(null)
    private val _showUploadDialog = MutableStateFlow(false)
    private val _uploadTargetCategory = MutableStateFlow(1)
    private val _pendingUploadUris = MutableStateFlow<List<Uri>>(emptyList())
    private val _showStorageInfoDialog = MutableStateFlow(false)
    private val _notification = MutableStateFlow<UiNotification?>(null)

    val uiState: StateFlow<ArchiveUiState> = combine(
        repository.allPhotos,
        repository.allCategories,
        _selectedCategoryFilter,
        _searchQuery,
        _sortOrder
    ) { allPhotos, categories, catFilter, query, sort ->
        // Ensure default categories exist if database was empty
        val finalCategories = if (categories.isEmpty()) {
            listOf(
                CategoryEntity(1, "Denim Recipes", "Denim wash recipes, enzyme treatments & stone wash formulas", "#2563EB"),
                CategoryEntity(2, "Dyeing Recipes", "Color dyeing, tinting, ozone & pigment dyeing recipes", "#7C3AED")
            )
        } else categories

        // Filter by category
        var filtered = if (catFilter != null) {
            allPhotos.filter { it.categoryId == catFilter }
        } else {
            allPhotos
        }

        // Filter by search query (name, full date YYYY-MM-DD, year YYYY, notes)
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { photo ->
                photo.name.lowercase().contains(q) ||
                        photo.dateString.lowercase().contains(q) ||
                        photo.notes.lowercase().contains(q) ||
                        (photo.dateString.take(4).contains(q)) // match year
            }
        }

        // Apply sort order
        filtered = when (sort) {
            PhotoSortOrder.NEWEST_DATE -> filtered.sortedWith(
                compareByDescending<PhotoEntity> { it.dateString }
                    .thenByDescending { it.createdAt }
            )
            PhotoSortOrder.OLDEST_DATE -> filtered.sortedWith(
                compareBy<PhotoEntity> { it.dateString }
                    .thenBy { it.createdAt }
            )
            PhotoSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            PhotoSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        }

        ArchiveUiState(
            photos = filtered,
            totalUnfilteredCount = allPhotos.size,
            categories = finalCategories,
            selectedCategoryFilter = catFilter,
            searchQuery = query,
            sortOrder = sort,
            activeViewerPhotoIndex = _activeViewerPhotoIndex.value,
            isUploading = _isUploading.value,
            uploadProgress = _uploadProgress.value,
            uploadStatusText = _uploadStatusText.value,
            editingPhoto = _editingPhoto.value,
            editingCategory = _editingCategory.value,
            photoToDelete = _photoToDelete.value,
            showUploadDialog = _showUploadDialog.value,
            uploadTargetCategory = _uploadTargetCategory.value,
            pendingUploadUris = _pendingUploadUris.value,
            showStorageInfoDialog = _showStorageInfoDialog.value,
            notification = _notification.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArchiveUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedSampleArchiveIfEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun selectCategoryFilter(categoryId: Int?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setSortOrder(order: PhotoSortOrder) {
        _sortOrder.value = order
    }

    fun openUploadDialog(targetCategory: Int = 1, uris: List<Uri> = emptyList()) {
        _uploadTargetCategory.value = targetCategory
        _pendingUploadUris.value = uris
        _showUploadDialog.value = true
    }

    fun dismissUploadDialog() {
        _showUploadDialog.value = false
        _pendingUploadUris.value = emptyList()
    }

    fun onPhotosSelected(uris: List<Uri>, defaultCategory: Int? = null) {
        if (uris.isEmpty()) return
        val cat = defaultCategory ?: _selectedCategoryFilter.value ?: 1
        _uploadTargetCategory.value = cat
        _pendingUploadUris.value = uris
        _showUploadDialog.value = true
    }

    fun confirmUpload(
        uris: List<Uri>,
        photoName: String,
        dateString: String,
        notes: String,
        categoryId: Int
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f
            _uploadStatusText.value = "Preparing ${uris.size} photo(s)..."

            val total = uris.size
            val result = repository.importMultiplePhotos(
                uris = uris,
                baseName = photoName,
                dateString = dateString,
                notes = notes,
                categoryId = categoryId,
                onProgress = { current, totalCount ->
                    _uploadProgress.value = current.toFloat() / totalCount.toFloat()
                    _uploadStatusText.value = "Saved photo $current of $totalCount..."
                }
            )

            _isUploading.value = false
            _uploadProgress.value = 1f
            _uploadStatusText.value = null
            _showUploadDialog.value = false
            _pendingUploadUris.value = emptyList()

            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                showNotification("Successfully imported $count photo(s) into Category $categoryId")
            } else {
                showNotification(
                    "Error importing photos: ${result.exceptionOrNull()?.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun openPhotoViewer(photo: PhotoEntity) {
        val currentList = uiState.value.photos
        val index = currentList.indexOfFirst { it.id == photo.id }
        if (index != -1) {
            _activeViewerPhotoIndex.value = index
        }
    }

    fun closePhotoViewer() {
        _activeViewerPhotoIndex.value = null
    }

    fun navigateViewerNext() {
        val currentList = uiState.value.photos
        val currentIndex = _activeViewerPhotoIndex.value ?: return
        if (currentIndex < currentList.lastIndex) {
            _activeViewerPhotoIndex.value = currentIndex + 1
        }
    }

    fun navigateViewerPrevious() {
        val currentIndex = _activeViewerPhotoIndex.value ?: return
        if (currentIndex > 0) {
            _activeViewerPhotoIndex.value = currentIndex - 1
        }
    }

    fun startEditingPhoto(photo: PhotoEntity) {
        _editingPhoto.value = photo
    }

    fun dismissEditingPhoto() {
        _editingPhoto.value = null
    }

    fun savePhotoEdit(
        photo: PhotoEntity,
        newName: String,
        newDate: String,
        newNotes: String,
        newCategoryId: Int
    ) {
        viewModelScope.launch {
            val updated = photo.copy(
                name = newName.trim(),
                dateString = newDate.trim(),
                notes = newNotes.trim(),
                categoryId = newCategoryId
            )
            repository.updatePhoto(updated)
            _editingPhoto.value = null
            showNotification("Photo \"${updated.name}\" updated successfully")
        }
    }

    fun promptDeletePhoto(photo: PhotoEntity) {
        _photoToDelete.value = photo
    }

    fun dismissDeletePrompt() {
        _photoToDelete.value = null
    }

    fun confirmDeletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            _photoToDelete.value = null
            // If viewer was open on this photo, adjust index or close
            val viewerIndex = _activeViewerPhotoIndex.value
            if (viewerIndex != null) {
                val remainingCount = uiState.value.photos.size - 1
                if (remainingCount <= 0) {
                    _activeViewerPhotoIndex.value = null
                } else if (viewerIndex >= remainingCount) {
                    _activeViewerPhotoIndex.value = remainingCount - 1
                }
            }
            showNotification("Photo \"${photo.name}\" deleted")
        }
    }

    fun startEditingCategory(category: CategoryEntity) {
        _editingCategory.value = category
    }

    fun dismissEditingCategory() {
        _editingCategory.value = null
    }

    fun saveCategoryEdit(category: CategoryEntity, newName: String, description: String, colorHex: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateCategoryName(category.id, newName.trim(), description.trim(), colorHex)
            _editingCategory.value = null
            showNotification("Category renamed to \"$newName\"")
        }
    }

    fun downloadPhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            val result = PhotoFileUtils.exportPhotoToDeviceGallery(
                context = getApplication(),
                sourceFilePath = photo.filePath,
                targetDisplayName = photo.name,
                mimeType = photo.mimeType
            )
            if (result.isSuccess) {
                showNotification("Photo downloaded: ${result.getOrNull()}")
            } else {
                showNotification(
                    "Download failed: ${result.exceptionOrNull()?.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun printPhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            val result = PrintHelperUtil.printPhoto(
                context = getApplication(),
                filePath = photo.filePath,
                photoName = photo.name
            )
            if (result.isFailure) {
                showNotification(
                    "Print failed: ${result.exceptionOrNull()?.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun openStorageInfo() {
        _showStorageInfoDialog.value = true
    }

    fun dismissStorageInfo() {
        _showStorageInfoDialog.value = false
    }

    fun addDemoPhotos() {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadStatusText.value = "Creating demo archive samples..."

            // Sample 1: Category 1 (Denim Recipes - Stone Wash)
            repository.createSampleDemoPhoto(
                title = "Vintage Stone Bleach Wash #104",
                date = "2026-07-14",
                categoryId = 1,
                notes = "Pumice stone 1:2 ratio, cellulase enzyme 1.2 g/L, NaOCl 3.0 g/L at 45°C. Neutralized with bisulfite.",
                primaryHex = android.graphics.Color.rgb(30, 64, 175), // Denim blue
                accentHex = android.graphics.Color.rgb(245, 158, 11)  // Amber contrast
            )

            // Sample 2: Category 1 (Denim Recipes - Enzyme Polish)
            repository.createSampleDemoPhoto(
                title = "Enzyme Bio-Polish Sky Fade",
                date = "2026-08-20",
                categoryId = 1,
                notes = "Neutral cellulase enzyme wash at pH 6.5, 55°C for 45 min. Clean salt-and-pepper grain effect.",
                primaryHex = android.graphics.Color.rgb(14, 116, 144), // Indigo cyan
                accentHex = android.graphics.Color.rgb(234, 88, 12)   // Orange stitch accent
            )

            // Sample 3: Category 2 (Dyeing Recipes - Sulfur Tint)
            repository.createSampleDemoPhoto(
                title = "Sulfur Indigo Top Tinting 2.5%",
                date = "2026-09-05",
                categoryId = 2,
                notes = "Sulfur Brown liquid tint 2.5 g/L with soda ash 1.0 g/L. Steamed at 102°C for authentic dirty vintage tone.",
                primaryHex = android.graphics.Color.rgb(88, 28, 135), // Violet sulfur
                accentHex = android.graphics.Color.rgb(56, 189, 248)  // Light tint blue
            )

            // Sample 4: Category 2 (Dyeing Recipes - Reactive Overdye)
            repository.createSampleDemoPhoto(
                title = "Reactive Overdye Deep Ocean",
                date = "2026-10-01",
                categoryId = 2,
                notes = "Reactive Blue 19 overdye 1.8% owf, Glauber salt 40 g/L, alkali fixation at 60°C. Excellent wash fastness.",
                primaryHex = android.graphics.Color.rgb(19, 78, 74),  // Deep ocean teal
                accentHex = android.graphics.Color.rgb(250, 204, 21)  // Yellow fixation
            )

            _isUploading.value = false
            _uploadStatusText.value = null
            showNotification("Added 4 demo wash & dyeing chemical recipes")
        }
    }

    fun calculateTotalStorageSize(): Long {
        val photosDir = File(getApplication<Application>().filesDir, "photos")
        if (!photosDir.exists()) return 0L
        return photosDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearNotification() {
        _notification.value = null
    }

    private fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message, isError)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    val repository = PhotoRepository(application, db.photoDao())
                    return PhotoArchiveViewModel(application, repository) as T
                }
            }
    }
}
