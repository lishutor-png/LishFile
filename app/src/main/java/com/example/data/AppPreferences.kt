package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.AesCryptoEngine
import com.example.model.SortBy
import com.example.model.SortOrder
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

    private val _sortBy = MutableStateFlow(loadSortBy())
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _sortOrder = MutableStateFlow(loadSortOrder())
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _hasMasterPin = MutableStateFlow(prefs.getString(KEY_MASTER_PIN_HASH, null) != null)
    val hasMasterPin: StateFlow<Boolean> = _hasMasterPin.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setShowHiddenFiles(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN, show).apply()
        _showHiddenFiles.value = show
    }

    fun toggleShowHiddenFiles() {
        setShowHiddenFiles(!_showHiddenFiles.value)
    }

    fun setShowSystemFiles(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SYSTEM, show).apply()
        _showSystemFiles.value = show
    }

    fun toggleShowSystemFiles() {
        setShowSystemFiles(!_showSystemFiles.value)
    }

    fun setGridView(grid: Boolean) {
        prefs.edit().putBoolean(KEY_GRID_VIEW, grid).apply()
        _isGridView.value = grid
    }

    fun toggleGridView() {
        setGridView(!_isGridView.value)
    }

    fun setSorting(sortBy: SortBy, sortOrder: SortOrder) {
        prefs.edit()
            .putString(KEY_SORT_BY, sortBy.name)
            .putString(KEY_SORT_ORDER, sortOrder.name)
            .apply()
        _sortBy.value = sortBy
        _sortOrder.value = sortOrder
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setMasterPin(pin: String) {
        val salt = AesCryptoEngine.generateSalt()
        val hash = AesCryptoEngine.hashPassword(pin, salt)
        prefs.edit()
            .putString(KEY_MASTER_PIN_HASH, hash)
            .putString(KEY_MASTER_PIN_SALT, salt)
            .apply()
        _hasMasterPin.value = true
    }

    fun verifyMasterPin(pin: String): Boolean {
        val hash = prefs.getString(KEY_MASTER_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_MASTER_PIN_SALT, null) ?: return false
        val computed = AesCryptoEngine.hashPassword(pin, salt)
        return hash == computed
    }

    fun getDeviceName(): String {
        return prefs.getString(KEY_DEVICE_NAME, "FileManagerPlus-" + android.os.Build.MODEL) ?: "FileManagerPlus Device"
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    private fun loadSortBy(): SortBy {
        val name = prefs.getString(KEY_SORT_BY, SortBy.NAME.name)
        return try {
            SortBy.valueOf(name ?: SortBy.NAME.name)
        } catch (_: Exception) {
            SortBy.NAME
        }
    }

    private fun loadSortOrder(): SortOrder {
        val name = prefs.getString(KEY_SORT_ORDER, SortOrder.ASCENDING.name)
        return try {
            SortOrder.valueOf(name ?: SortOrder.ASCENDING.name)
        } catch (_: Exception) {
            SortOrder.ASCENDING
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SHOW_HIDDEN = "show_hidden_files"
        private const val KEY_SHOW_SYSTEM = "show_system_files"
        private const val KEY_GRID_VIEW = "is_grid_view"
        private const val KEY_SORT_BY = "sort_by"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_MASTER_PIN_HASH = "master_pin_hash"
        private const val KEY_MASTER_PIN_SALT = "master_pin_salt"
        private const val KEY_DEVICE_NAME = "device_name"
    }
}
