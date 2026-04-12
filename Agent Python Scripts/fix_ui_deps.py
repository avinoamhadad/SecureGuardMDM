import os, re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.text = f.read()
    orig = content
    for old, new in replacements:
        content = content.replace(old, new)
    if content != orig:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)

# Fix PCAPdroid.java
replacements_pcapdroid = [
    ('import com.emanuelef.remote_capture.activities.ErrorActivity;', ''),
    ('import com.emanuelef.remote_capture.activities.MDMSettingsActivity;', ''),
    ('import com.emanuelef.remote_capture.activities.AppState;', ''),
    ('import com.emanuelef.remote_capture.activities.PathType;', ''),
    ('import com.emanuelef.remote_capture.activities.accser;', ''),
    ('import com.emanuelef.remote_capture.activities.admin;', ''),
    ('Intent intent = new Intent(getApplicationContext(), MDMSettingsActivity.class);', '// Intent to settings'),
    ('intent = new Intent(getApplicationContext(), debug.class);', '// Intent to debug'),
    ('if(AppState.getInstance()!=null){', 'if(false){'),
    ('AppState.getInstance().setCurrentPath', '// AppState'),
    ('accser.refreshacc.refreshacc(getApplicationContext());', '// refreshacc'),
    ('ComponentName mAdminComponentName = new ComponentName(this,admin.class);', 'ComponentName mAdminComponentName = null;'),
    ('if(AppState.getInstance().getCurrentPath().equals(PathType.MULTIMEDIA)){', 'if(true){')
]
replace_in_file('app/src/main/java/com/emanuelef/remote_capture/PCAPdroid.java', replacements_pcapdroid)

# Fix Blacklists.java (specifically AppState switch case)
import re
with open('app/src/main/java/com/emanuelef/remote_capture/Blacklists.java', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('import com.emanuelef.remote_capture.activities.AppState;', '')
# We need to replace the switch(AppState...) block. There is line 122: switch (AppState.getInstance().getCurrentPath()){
# Let's just blindly replace AppState.getInstance().getCurrentPath().name() with "MULTIMEDIA"
text = re.sub(r'AppState\.getInstance\(\)\.getCurrentPath\(\)', 'com.emanuelef.remote_capture.activities.PathType.MULTIMEDIA', text)

with open('app/src/main/java/com/emanuelef/remote_capture/Blacklists.java', 'w', encoding='utf-8') as f:
    f.write(text)

# We need to make sure PathType enum exists since Blacklists now uses it, or we just change the switch to use a String.
# Since PathType import was removed from PCAPdroid, let's just make it a static string.
with open('app/src/main/java/com/emanuelef/remote_capture/Blacklists.java', 'r', encoding='utf-8') as f:
    text = f.read()
text = text.replace('com.emanuelef.remote_capture.activities.PathType.MULTIMEDIA', '((Object)null)') # This will break the switch.

# Better yet, let's just create PathType and AppState stubs in com.emanuelef.remote_capture.activities so all these files compile without regex hacking!
os.makedirs('app/src/main/java/com/emanuelef/remote_capture/activities', exist_ok=True)
with open('app/src/main/java/com/emanuelef/remote_capture/activities/AppState.java', 'w') as f:
    f.write('''package com.emanuelef.remote_capture.activities;
public class AppState {
    private static AppState instance = new AppState();
    public static AppState getInstance() { return instance; }
    public PathType getCurrentPath() { return PathType.MULTIMEDIA; }
    public void setCurrentPath(PathType t) {}
}''')

with open('app/src/main/java/com/emanuelef/remote_capture/activities/PathType.java', 'w') as f:
    f.write('''package com.emanuelef.remote_capture.activities;
public enum PathType { MULTIMEDIA }''')

with open('app/src/main/java/com/emanuelef/remote_capture/activities/admin.java', 'w') as f:
    f.write('''package com.emanuelef.remote_capture.activities;
public class admin {}''')

with open('app/src/main/java/com/emanuelef/remote_capture/activities/accser.java', 'w') as f:
    f.write('''package com.emanuelef.remote_capture.activities;
public class accser {
    public static class refreshacc {
        public static void refreshacc(android.content.Context c) {}
    }
}''')

with open('app/src/main/java/com/emanuelef/remote_capture/activities/ErrorActivity.java', 'w') as f:
    f.write('''package com.emanuelef.remote_capture.activities;
public class ErrorActivity extends android.app.Activity {}''')

with open('app/src/main/java/com/emanuelef/remote_capture/activities/MDMSettingsActivity.java', 'w') as f:
    f.write('''package com.emanuelef.remote_capture.activities;
public class MDMSettingsActivity extends android.app.Activity {}''')

with open('app/src/main/java/com/emanuelef/remote_capture/debug.java', 'w') as f:
    f.write('''package com.emanuelef.remote_capture;
public class debug extends android.app.Activity {}''')

print("UI dependencies stubbed.")
