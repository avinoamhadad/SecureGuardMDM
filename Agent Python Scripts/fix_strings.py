import os

filepath = 'app/src/main/res/values/vpn_stubs.xml'
with open(filepath, 'r') as f:
    content = f.read()

# Replace </resources> with the string and </resources>
if 'vpn_control_title' not in content:
    content = content.replace('</resources>', '    <string name="vpn_control_title">VPN Control Module</string>\n</resources>')

with open(filepath, 'w') as f:
    f.write(content)

print("String resource injected.")
