package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lishfile_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean(KEY_SHOW_HIDDEN, false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _showSystemFiles = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SYSTEM, false))
    val showSystemFiles: StateFlow<Boolean> = _showSystemFiles.asStateFlow()

    private val _isGridView = MutableStateFlow(prefs.getBoolean(KEY_GRID_VIEW, false))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _hasMasterPin = MutableStateFlow(prefs.getString(KEY_MASTER_PIN, null) != null)
    val hasMasterPin: StateFlow<Boolean> = _hasMasterPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC, true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _recentFolders = MutableStateFlow(loadRecentFolders())
    val recentFolders: StateFlow<List<String>> = _recentFolders.asStateFlow()

    fun saveCategoryStats(stats: com.example.model.CategoryOverviewStats) {
        prefs.edit()
            .putInt("cat_img_count", stats.imagesCount)
            .putLong("cat_img_size", stats.imagesSize)
            .putInt("cat_vid_count", stats.videosCount)
            .putLong("cat_vid_size", stats.videosSize)
            .putInt("cat_aud_count", stats.audioCount)
            .putLong("cat_aud_size", stats.audioSize)
            .putInt("cat_doc_count", stats.docsCount)
            .putLong("cat_doc_size", stats.docsSize)
            .putInt("cat_arc_count", stats.archivesCount)
            .putLong("cat_arc_size", stats.archivesSize)
            .putInt("cat_apk_count", stats.apksCount)
            .putLong("cat_apk_size", stats.apksSize)
            .putInt("cat_dl_count", stats.downloadsCount)
            .putLong("cat_dl_size", stats.downloadsSize)
            .apply()
    }

    fun loadCategoryStats(): com.example.model.CategoryOverviewStats {
        return com.example.model.CategoryOverviewStats(
            imagesCount = prefs.getInt("cat_img_count", 0),
            imagesSize = prefs.getLong("cat_img_size", 0L),
            videosCount = prefs.getInt("cat_vid_count", 0),
            videosSize = prefs.getLong("cat_vid_size", 0L),
            audioCount = prefs.getInt("cat_aud_count", 0),
            audioSize = prefs.getLong("cat_aud_size", 0L),
            docsCount = prefs.getInt("cat_doc_count", 0),
            docsSize = prefs.getLong("cat_doc_size", 0L),
            archivesCount = prefs.getInt("cat_arc_count", 0),
            archivesSize = prefs.getLong("cat_arc_size", 0L),
            apksCount = prefs.getInt("cat_apk_count", 0),
            apksSize = prefs.getLong("cat_apk_size", 0L),
            downloadsCount = prefs.getInt("cat_dl_count", 0),
            downloadsSize = prefs.getLong("cat_dl_size", 0L)
        )
    }

    fun addRecentFolder(path: String) {
        val current = _recentFolders.value.toMutableList()
        current.remove(path)
        current.add(0, path)
        val trimmed = current.take(10)
        prefs.edit().putString(KEY_RECENT_FOLDERS, trimmed.joinToString("|")).apply()
        _recentFolders.value = trimmed
    }

    private fun loadRecentFolders(): List<String> {
        val raw = prefs.getString(KEY_RECENT_FOLDERS, null) ?: return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun clearRecentFolders() {
        prefs.edit().remove(KEY_RECENT_FOLDERS).apply()
        _recentFolders.value = emptyList()
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setShowHiddenFiles(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN, show).apply()
        _showHiddenFiles.value = show
    }

    fun setShowSystemFiles(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SYSTEM, show).apply()
        _showSystemFiles.value = show
    }

    fun setGridView(grid: Boolean) {
        prefs.edit().putBoolean(KEY_GRID_VIEW, grid).apply()
        _isGridView.value = grid
    }

    fun setMasterPin(pin: String) {
        prefs.edit().putString(KEY_MASTER_PIN, pin).apply()
        _hasMasterPin.value = true
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun verifyMasterPin(pin: String): Boolean {
        val storedPin = prefs.getString(KEY_MASTER_PIN, null)
        return storedPin == pin
    }

    fun isFolderLocked(folderPath: String): Boolean {
        val lockedSet = prefs.getStringSet(KEY_LOCKED_FOLDERS, emptySet()) ?: emptySet()
        return lockedSet.contains(folderPath)
    }

    fun lockFolder(folderPath: String) {
        val lockedSet = (prefs.getStringSet(KEY_LOCKED_FOLDERS, emptySet()) ?: emptySet()).toMutableSet()
        lockedSet.add(folderPath)
        prefs.edit().putStringSet(KEY_LOCKED_FOLDERS, lockedSet).apply()
    }

    fun unlockFolder(folderPath: String) {
        val lockedSet = (prefs.getStringSet(KEY_LOCKED_FOLDERS, emptySet()) ?: emptySet()).toMutableSet()
        lockedSet.remove(folderPath)
        prefs.edit().putStringSet(KEY_LOCKED_FOLDERS, lockedSet).apply()
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_SHOW_HIDDEN = "pref_show_hidden"
        private const val KEY_SHOW_SYSTEM = "pref_show_system"
        private const val KEY_GRID_VIEW = "pref_grid_view"
        private const val KEY_MASTER_PIN = "pref_master_pin"
        private const val KEY_BIOMETRIC = "pref_biometric"
        private const val KEY_LOCKED_FOLDERS = "pref_locked_folders"
        private const val KEY_RECENT_FOLDERS = "pref_recent_folders"
    }
}
