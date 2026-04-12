import os

filepath = 'app/src/main/java/com/secureguard/mdm/settingsfeatures/registry/SettingsRegistry.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

if 'NavigateToVpnSetting' not in content:
    content = content.replace('ExportSettingsAction\n    )', 'ExportSettingsAction,\n        NavigateToVpnSetting\n    )')
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Added NavigateToVpnSetting to SettingsRegistry!")
else:
    print("Already exists.")
