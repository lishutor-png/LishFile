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

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _recentFolders = MutableStateFlow(loadRecentFolders())
    val recentFolders: StateFlow<List<String>> = _recentFolders.asStateFlow()

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
