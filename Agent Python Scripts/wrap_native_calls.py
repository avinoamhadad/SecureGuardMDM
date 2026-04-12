import re

filepath = 'app/src/main/java/com/emanuelef/remote_capture/CaptureService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    lines = f.readlines()

native_methods = [
    'setPrivateDnsBlocked', 'setDnsServer', 'addPortMapping', 'reloadBlacklists',
    'reloadBlocklist', 'reloadFirewallWhitelist', 'reloadMalwareWhitelist',
    'reloadDecryptionList', 'askStatsDump', 'nativeSetFirewallEnabled',
    'setPayloadMode', 'setdebug', 'setisNetfree'
]

new_lines = []
for line in lines:
    # Skip the actual native method declarations (at the end of the file)
    if ' native ' in line and ';' in line and ('public' in line or 'private' in line):
        new_lines.append(line)
        continue
    
    modified = line
    # Match calls like: setDnsServer(dns_server); or setdebug(true);
    # But NOT when they are already wrapped with if (isNativeLibraryLoaded())
    # This regex is simplified but should work for the common cases in this file.
    for method in native_methods:
        # Check if the line contains the method call and is NOT already wrapped
        if method + '(' in modified and 'if (isNativeLibraryLoaded())' not in modified and 'isNativeLibraryLoaded() && ' not in modified:
            # Avoid wrapping method definitions or declarations
            if 'void ' in modified or 'int ' in modified or 'boolean ' in modified:
                continue
            
            # Simple wrapper
            indent = re.match(r'^\s*', modified).group(0)
            modified = f"{indent}if (isNativeLibraryLoaded())\n    {modified}"
    
    new_lines.append(modified)

with open(filepath, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("Wrapped native calls in CaptureService.java")
