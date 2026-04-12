import os, glob

vector_content = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path
        android:fillColor="#FF0000"
        android:pathData="M12,2A10,10 0 1,0 22,12A10,10 0 0,0 12,2Z" />
</vector>'''

count = 0
drawable_dir = 'app/src/main/res/drawable'
for filepath in glob.glob(os.path.join(drawable_dir, '*.xml')):
    with open(filepath, 'r') as f:
        content = f.read()
    if '<shape' in content:
        with open(filepath, 'w') as f:
            f.write(vector_content)
        count += 1

print(f"Fixed {count} drawable stubs by converting them to vectors.")
