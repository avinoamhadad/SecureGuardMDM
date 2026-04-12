import re

filepath = 'app/src/main/java/com/emanuelef/remote_capture/CaptureService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove the block: if (isNativeLibraryLoaded()) { ... }
# Matches: if (isNativeLibraryLoaded()) { \n setdebug(...); \n setisNetfree(...); \n } 
# and also: if (isNativeLibraryLoaded()) \n setPrivateDnsBlocked(false);
content = re.sub(r'if \(isNativeLibraryLoaded\(\)\) \{\n\s+(.*?)\n\s+(.*?)\n\s+\}', r'\1\n\2', content)
content = re.sub(r'if \(isNativeLibraryLoaded\(\)\)\n\s+(.*?);', r'\1;', content)

# Remove any isNativeLibraryLoaded from Log.java
log_filepath = 'app/src/main/java/com/emanuelef/remote_capture/Log.java'
with open(log_filepath, 'r', encoding='utf-8') as f:
    log_content = f.read()

log_content = log_content.replace('if(!PCAPdroid.isUnderTest() && CaptureService.isNativeLibraryLoaded())', 'if(!PCAPdroid.isUnderTest())')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
with open(log_filepath, 'w', encoding='utf-8') as f:
    f.write(log_content)

print("Reverted all native guards.")
