import os

drawables = ['ic_app_crash', 'ic_check_solid', 'ic_lock', 'ic_lock_open', 'ic_logo', 'ic_menu_help', 'ic_shopping_cart', 'ic_skull']

layouts = ['active_filter_chip']

ids = ['blacklisted', 'capture_interface', 'decryption_status', 'firewall', 'message', 'not_hidden', 'only_cleartext', 'status_ind']

styles = ['AppThemeAmber', 'AppThemeAmberDark', 'AppThemeBlue', 'AppThemeBlueDark', 'AppThemeClassic', 'AppThemeClassicDark', 'AppThemeCo', 'AppThemeCoDark', 'AppThemeGrd', 'AppThemeGrdDark', 'AppThemeGreen', 'AppThemeGreenDark', 'AppThemeMo', 'AppThemeMoDark', 'AppThemeOrange', 'AppThemeOrangeDark', 'AppThemePu', 'AppThemePuDark', 'AppThemePurple', 'AppThemePurpleDark', 'AppThemePurpleR', 'AppThemePurpleRDark', 'AppThemePurpleT', 'AppThemePurpleTDark', 'AppThemeTeal', 'AppThemeTealDark']

os.makedirs('app/src/main/res/drawable', exist_ok=True)
os.makedirs('app/src/main/res/layout', exist_ok=True)

for d in drawables:
    with open(f'app/src/main/res/drawable/{d}.xml', 'w') as f:
        f.write('<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#FF0000"/></shape>')

for l in layouts:
    with open(f'app/src/main/res/layout/{l}.xml', 'w') as f:
        f.write('<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"/>')

with open('app/src/main/res/values/vpn_stubs.xml', 'a') as f:
    for s in styles:
        f.write(f'    <style name="{s}"/>\n')
    for i in ids:
        f.write(f'    <item type="id" name="{i}"/>\n')

print("Created all UI stubs.")
