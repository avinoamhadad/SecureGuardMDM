package com.secureguard.mdm.settingsfeatures.impl

import com.secureguard.mdm.R
import com.secureguard.mdm.settingsfeatures.api.SettingCategory
import com.secureguard.mdm.settingsfeatures.api.ToggleSetting

object ToggleUiPositionSetting : ToggleSetting {
    override val id: String = "toggle_ui_position"
    override val titleRes: Int = R.string.settings_item_toggle_position
    override val iconRes: Int = 0 // No specific icon for this toggle
    override val category: SettingCategory = SettingCategory.UI_AND_BEHAVIOR
}

object ToggleUiControlTypeSetting : ToggleSetting {
    override val id: String = "toggle_ui_control_type"
    override val titleRes: Int = R.string.settings_item_use_checkbox
    override val iconRes: Int = 0 // No specific icon for this toggle
    override val category: SettingCategory = SettingCategory.UI_AND_BEHAVIOR
}

object ToggleContactEmailSetting : ToggleSetting {
    override val id: String = "toggle_contact_email"
    override val titleRes: Int = R.string.settings_item_show_contact_email
    override val iconRes: Int = 0 // No specific icon for this toggle
    override val category: SettingCategory = SettingCategory.UI_AND_BEHAVIOR
}

// New setting item for the boot toast
object ShowBootToastSetting : ToggleSetting {
    override val id: String = "toggle_show_boot_toast"
    override val titleRes: Int = R.string.settings_item_show_boot_toast
    override val iconRes: Int = 0 // No specific icon
    override val category: SettingCategory = SettingCategory.UI_AND_BEHAVIOR
}

// Watermark overlay (small image in the top-right of the device home screen / app drawer).
object WatermarkEnabledSetting : ToggleSetting {
    override val id: String = "toggle_watermark_enabled"
    override val titleRes: Int = R.string.settings_item_watermark_enabled
    override val iconRes: Int = R.drawable.ic_watermark_filtered
    override val category: SettingCategory = SettingCategory.UI_AND_BEHAVIOR
}

object WatermarkAlphaAction : com.secureguard.mdm.settingsfeatures.api.ActionSetting {
    override val id: String = "action_watermark_alpha"
    override val titleRes: Int = R.string.settings_item_watermark_alpha
    override val iconRes: Int = 0
    override val category: SettingCategory = SettingCategory.UI_AND_BEHAVIOR
}

object WatermarkVariantAction : com.secureguard.mdm.settingsfeatures.api.ActionSetting {
    override val id: String = "action_watermark_variant"
    override val titleRes: Int = R.string.settings_item_watermark_variant
    override val iconRes: Int = 0
    override val category: SettingCategory = SettingCategory.UI_AND_BEHAVIOR
}