import os, re

base_dirs = [
    r"app\src\main\java\com\emanuelef\remote_capture",
    r"app\src\main\java\com\obsex",
    r"app\src\main\java\com\pcapdroid"
]

r_pattern = re.compile(r'\bR\.(string|color|drawable|style|id|layout|xml|menu)\.([a-zA-Z0-9_]+)')

resources = set()

for d in base_dirs:
    for root, dirs, files in os.walk(d):
        for f in files:
            if f.endswith(".java"):
                path = os.path.join(root, f)
                with open(path, "r", encoding="utf-8") as file:
                    content = file.read()
                
                # Check if it has R. references
                matches = r_pattern.findall(content)
                if matches:
                    for match in matches:
                        resources.add(f"{match[0]}_{match[1]}")
                    
                    # Remove old imports
                    content = re.sub(r'import com\.emanuelef\.remote_capture\.R;\n?', '', content)
                    content = re.sub(r'import com\.secureguard\.mdm\.R;\n?', '', content)
                    
                    # Add new import right after package declaration
                    content = re.sub(r'^(package [^;]+;)', r'\1\nimport com.secureguard.mdm.R;', content, count=1, flags=re.MULTILINE)
                    
                    with open(path, "w", encoding="utf-8") as file:
                        file.write(content)
                        
with open(r"app\src\main\res\values\vpn_stubs.xml", "w", encoding="utf-8") as xml_file:
    xml_file.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n')
    for res in sorted(resources):
        res_type, res_name = res.split('_', 1)
        if res_type == "string":
            xml_file.write(f'    <string name="{res_name}">Stub {res_name}</string>\n')
        elif res_type == "color":
            xml_file.write(f'    <color name="{res_name}">#FF0000</color>\n')
    xml_file.write('</resources>\n')

print(f"Fixed files and generated vpn_stubs.xml with {len(resources)} resources.")
