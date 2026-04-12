import re

filepath = 'app/src/main/java/com/emanuelef/remote_capture/CaptureService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove the 'if (isNativeLibraryLoaded())\n    ' pattern
# Note: we need to handle the indentation correctly.
new_content = re.sub(r'if \(isNativeLibraryLoaded\(\)\)\n\s+', '', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Reverted native guards in CaptureService.java")
