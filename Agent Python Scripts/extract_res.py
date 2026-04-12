import re, os
r_pattern = re.compile(r'\bR\.(drawable|id|layout|style)\.([a-zA-Z0-9_]+)')
res = set()
for d in ['app/src/main/java/com/emanuelef/remote_capture', 'app/src/main/java/com/obsex', 'app/src/main/java/com/pcapdroid']:
    for r, ds, fs in os.walk(d):
        for f in fs:
            if f.endswith('.java'):
                with open(os.path.join(r, f), encoding='utf-8') as file:
                    for m in r_pattern.findall(file.read()):
                        res.add(m)
for typ, name in sorted(res):
    print(f'{typ} {name}')
