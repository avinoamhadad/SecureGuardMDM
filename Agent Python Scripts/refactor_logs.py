import os
import re

APP_DIR = r"c:\Users\Yossi\Documents\TheFix\ABloq\app\src\main\java"

def process_file(filepath):
    # Ignore AppLogger.kt itself
    if "AppLogger.kt" in filepath:
        return
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    
    # Replace import
    content = content.replace("import android.util.Log", "import com.secureguard.mdm.utils.AppLogger")
    
    # Replace method calls
    # Match Log.d(, Log.e(, Log.v(, Log.w(, Log.i(
    content = re.sub(r'\bLog\.(v|d|i|w|e)\(', r'AppLogger.\1(', content)
    
    # Replace FileLogger.log -> AppLogger.i  (since we removed FileLogger possibly later, lets cover bases)
    # The application class was done manually.
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, _, files in os.walk(APP_DIR):
    for filename in files:
        if filename.endswith(".kt") or filename.endswith(".java"):
            process_file(os.path.join(root, filename))

print("Done refactoring logs.")
