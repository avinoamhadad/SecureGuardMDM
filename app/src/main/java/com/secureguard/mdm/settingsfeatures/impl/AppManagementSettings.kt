package com.secureguard.mdm.settingsfeatures.impl

import com.secureguard.mdm.R
import com.secureguard.mdm.settingsfeatures.api.NavigationalSetting
import com.secureguard.mdm.settingsfeatures.api.SettingCategory
import com.secureguard.mdm.ui.navigation.Routes

object NavigateToAppSelectionSetting : NavigationalSetting {
    override val id: String = "navigate_app_selection"
    override val titleRes: Int = R.string.settings_item_select_apps_to_block
    override val iconRes: Int = R.drawable.ic_manage_apps
    override val category: SettingCategory = SettingCategory.APP_MANAGEMENT
    override val route: String = Routes.APP_SELECTION
}

object NavigateToBlockedAppsSetting : NavigationalSetting {
    override val id: String = "navigate_blocked_apps"
    override val titleRes: Int = R.string.settings_item_view_blocked_apps
    override val iconRes: Int = R.drawable.ic_apps_blocked
    override val category: SettingCategory = SettingCategory.APP_MANAGEMENT
    override val route: String = Routes.BLOCKED_APPS_DISPLAY
}

object AstoreToggleSetting : com.secureguard.mdm.settingsfeatures.api.ToggleSetting {
    override val id: String = "toggle_astore_enabled"
    override val titleRes: Int = R.string.settings_item_enable_astore
    override val iconRes: Int = R.drawable.ic_astore
    override val category: com.secureguard.mdm.settingsfeatures.api.SettingCategory = com.secureguard.mdm.settingsfeatures.api.SettingCategory.APP_MANAGEMENT
}

object NavigateToAstoreSettingsSetting : NavigationalSetting {
    override val id: String = "navigate_astore_settings"
    override val titleRes: Int = R.string.astore_settings_title
    override val iconRes: Int = R.drawable.ic_astore
    override val category: SettingCategory = SettingCategory.APP_MANAGEMENT
    override val route: String = com.secureguard.mdm.ui.navigation.Routes.ASTORE_SETTINGS
}
