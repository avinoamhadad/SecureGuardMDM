package com.secureguard.mdm.settingsfeatures.impl

import android.os.Build
import com.secureguard.mdm.R
import com.secureguard.mdm.settingsfeatures.api.NavigationalSetting
import com.secureguard.mdm.settingsfeatures.api.SettingCategory
import com.secureguard.mdm.ui.navigation.Routes

object NavigateToLogsSetting : NavigationalSetting {
    override val id: String = "navigate_to_logs"
    override val titleRes: Int = R.string.settings_navigate_logs
    override val iconRes: Int = R.drawable.ic_info
    override val category: SettingCategory = SettingCategory.ADVANCED_ACTIONS
    override val route: String = Routes.LOGS
}
