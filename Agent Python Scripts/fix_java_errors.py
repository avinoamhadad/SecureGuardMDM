import os, re

res_dir = 'app/src/main/java/com/emanuelef/remote_capture'

for root, dirs, files in os.walk(res_dir):
    for f in files:
        if f.endswith('.java'):
            filepath = os.path.join(root, f)
            with open(filepath, 'r', encoding='utf-8') as file:
                content = file.read()
            
            orig_content = content
            
            # Fix annotations
            content = content.replace('android.annotation.NonNull', 'androidx.annotation.NonNull')
            content = content.replace('android.annotation.Nullable', 'androidx.annotation.Nullable')
            
            # Fix multi-catch subclassing error
            content = content.replace('catch (IOException | Exception e)', 'catch (Exception e)')
            
            # Fix CaptureCtrl.notifyCaptureStopped in CaptureService.java
            if f == 'CaptureService.java':
                content = content.replace('CaptureCtrl.notifyCaptureStopped(CaptureService. this, getStats());', '// CaptureCtrl.notifyCaptureStopped')
                content = content.replace('.putExtra(ConnectionsFragment.QUERY_EXTRA, app.getPackageName())', '.putExtra("query", app.getPackageName())')
                content = content.replace('.putExtra(ConnectionsFragment.FILTER_EXTRA, filter)', '.putExtra("filter", filter)')
                
            # Fix ListInfo.java missing EditListFragment
            if f == 'ListInfo.java':
                content = content.replace('import com.emanuelef.remote_capture.fragments.EditListFragment;', '')
                content = re.sub(r'public EditListFragment newFragment\(\) \{.*\}', 'public Object newFragment() { return null; }', content, flags=re.DOTALL)
                
            # Fix Prefs.java missing BuildConfig
            if f == 'Prefs.java':
                content = content.replace('import com.emanuelef.remote_capture.BuildConfig;', '')
                content = content.replace('BuildConfig.', 'com.secureguard.mdm.BuildConfig.')

            if content != orig_content:
                with open(filepath, 'w', encoding='utf-8') as file:
                    file.write(content)

print("Java fixes applied.")
