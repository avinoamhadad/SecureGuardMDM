/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture;

import com.secureguard.mdm.utils.AppLogger;
import com.secureguard.mdm.R;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import android.preference.PreferenceManager;


import com.emanuelef.remote_capture.model.Blocklist;
import com.emanuelef.remote_capture.model.CtrlPermissions;
import com.emanuelef.remote_capture.model.MatchList;
import com.emanuelef.remote_capture.model.Prefs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import android.os.Process;
import com.emanuelef.remote_capture.activities.LogUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.widget.Toast;


import java.io.File;

import android.net.VpnService;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

//import cat.ereza.customactivityoncrash.config.CaocConfig;

/* The PCAPdroid app class.
 * This class is instantiated before anything else, and its reference is stored in the mInstance.
 * Global state is stored into this class via singletons. Contrary to static singletons, this does
 * not require passing the localized Context to the singletons getters methods.
 *
 * IMPORTANT: do not override getResources() with mLocalizedContext, otherwise the Webview used for ads will crash!
 * https://stackoverflow.com/questions/56496714/android-webview-causing-runtimeexception-at-webviewdelegate-getpackageid
 */
public class PCAPdroid extends Application {
    private static final String TAG = "PCAPdroid";
    private MatchList mVisMask;
    private MatchList mMalwareWhitelist;
    private MatchList mFirewallWhitelist;
    private MatchList mDecryptionList;
    private Blocklist mBlocklist;
    private Blacklists mBlacklists;
    private CtrlPermissions mCtrlPermissions;
    private Context mLocalizedContext;
    private boolean mIsDecryptingPcap = false;
    private boolean mIsUsharkAvailable = false;
    private static WeakReference<PCAPdroid> mInstance;
    protected static boolean isUnderTest = false;
    SharedPreferences sp;
    SharedPreferences.Editor spe;
    public static final String modesp="mode";
    
    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    //startActivity(new Intent(getApplicationContext(),debug.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    // Intent to settings
                    // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    // intent.putExtra("err","");
                    //// intent.putExtra("error", android.util.Log.getStackTraceString(throwable)+throwable.getStackTrace()[0].getClassName()+throwable.getStackTrace()[0].getMethodName()+throwable.getStackTrace()[0].getLineNumber()+throwable.getStackTrace()[0].getFileName());
                    // startActivity(intent);
                    // Intent to debug
                    // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // intent.putExtra("error", android.util.Log.getStackTraceString(throwable)+throwable.getStackTrace()[0].getClassName()+throwable.getStackTrace()[0].getMethodName()+throwable.getStackTrace()[0].getLineNumber()+throwable.getStackTrace()[0].getFileName());
                    // startActivity(intent);
                    try {
                        String LOG_PATH = "/storage/emulated/0/log.txt";
                        FileWriter writer = new FileWriter(LOG_PATH, true);
                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        writer.write("[" + time + "] " + throwable.toString() + "\n");
                        writer.close();
                    } catch (IOException ee) {
                        // silent
                    }
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            });
        super.onCreate();
        //Utils.setTheme(this);
        sp=PreferenceManager.getDefaultSharedPreferences(this);
        spe=sp.edit();
        try{
            if(sp.getString(modesp,"").equals("")){
                if(false){
                    // AppState(PathType.MULTIMEDIA);
                    // spe.putString(modesp,com.emanuelef.remote_capture.activities.AppState.getInstance().getCurrentPath().name());
                    spe.commit();
                    
                    // Toast.makeText(getApplicationContext(),com.emanuelef.remote_capture.activities.AppState.getInstance().getCurrentPath().name()+" is default",1).show();
                }
            }else{
                try{
                    // AppState(PathType.valueOf(sp.getString(modesp,"")));
                    //Toast.makeText(this, com.emanuelef.remote_capture.activities.AppState.getInstance().getCurrentPath().name()+ " is now",1).show();
                }catch(Exception e){
                    AppLogger.e("PCAPdroid", "Mode init error: " + e);
                    //importnt if it isnt found like old version
                    // AppState(PathType.MULTIMEDIA);
                    // spe.putString(modesp,com.emanuelef.remote_capture.activities.AppState.getInstance().getCurrentPath().name());
                    spe.commit();
                    
                    // Toast.makeText(getApplicationContext(),com.emanuelef.remote_capture.activities.AppState.getInstance().getCurrentPath().name()+" is default",1).show();
                }
            }
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("accessEnabled",false)){
                // refreshacc
            }
            cpdef(this);
        }catch(Exception e){}
try{
        if(!isUnderTest())
            Log.init(getFilesDir().getAbsolutePath());

        Utils.BuildType buildtp = Utils.getVerifiedBuild(this);
        //AppLogger.i(TAG, "Build type: " + buildtp);

        /*CaocConfig.Builder builder = CaocConfig.Builder.create();
        if((buildtp == Utils.BuildType.PLAYSTORE) || (buildtp == Utils.BuildType.UNKNOWN)) {
            // Disabled to get reports via the Android system reporting facility and for unsupported builds
            builder.enabled(false);
        } else {
            builder.errorDrawable(R.drawable.ic_app_crash)
                    .errorActivity(ErrorActivity.class);
        }
        builder.apply();*/
        CaptureService.setdebug(Prefs.isdebug(PreferenceManager.getDefaultSharedPreferences(this)));
        CaptureService.setisNetfree(Prefs.isNetfree(PreferenceManager.getDefaultSharedPreferences(this)));
        mInstance = new WeakReference<>(this);
        mLocalizedContext = createConfigurationContext(Utils.getLocalizedConfig(this));
        mIsUsharkAvailable = CaptureService.isUsharkAvailable(this);

        // Listen to package events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
                    boolean newInstall = !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                    String packageName = intent.getData().getSchemeSpecificPart();
                    AppLogger.d(TAG, "ACTION_PACKAGE_ADDED [new=" + newInstall + "]: " + packageName);

                    if(newInstall)
                        checkUidMapping(packageName);
                } else if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
                    boolean isUpdate = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                    String packageName = intent.getData().getSchemeSpecificPart();
                    AppLogger.d(TAG, "ACTION_PACKAGE_REMOVED [update=" + isUpdate + "]: " + packageName);

                    if(!isUpdate) {
                        checkUidMapping(packageName);
                        removeUninstalledAppsFromAppFilter();
                    }
                }
            }
        }, filter);

        removeUninstalledAppsFromAppFilter();
        

    DevicePolicyManager mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName mAdminComponentName = null;

    if(mDpm.isDeviceOwnerApp(getPackageName())){
        boolean vpnenabled=false;
        String strpkgvpn= mDpm.getAlwaysOnVpnPackage(mAdminComponentName);
        if(strpkgvpn!=null){
            vpnenabled=strpkgvpn.equals(getPackageName());
        }

        if(vpnenabled){
            try {
                VpnService.prepare(this);
                mDpm.setAlwaysOnVpnPackage(mAdminComponentName, this.getPackageName(), true);
            } catch (Exception e) {}
        }
    }
        
        }catch(Throwable e){
            //LogUtil.logToFile(e.toString());
            // Intent to settings
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // intent.putExtra("err","");
            //// intent.putExtra("error", ""+android.util.Log.getStackTraceString(e));
            // startActivity(intent);
            // Intent to debug
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // intent.putExtra("error", ""+android.util.Log.getStackTraceString(e));
            // startActivity(intent);
            try {
                String LOG_PATH = "/storage/emulated/0/log.txt";
                FileWriter writer = new FileWriter(LOG_PATH, true);
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                writer.write("[" + time + "] " + e.toString() + "\n");
                writer.close();
            } catch (IOException ee) {
                // silent
            }
            //Process.killProcess(Process.myPid());
            //System.exit(1);
        }
    }
    
    private static void cpdef(Context mcontext){
        
        try {
            if(true){
                if(!(new File(mcontext.getFilesDir().getPath()+"/malware_bl/").isDirectory()&&new File(mcontext.getFilesDir().getPath()+"/malware_bl/").list().length==2)){
            new File(mcontext.getFilesDir().getPath()+"/malware_bl/").mkdirs();
            String fname="domainswhite.txt";
            Utils.copy(mcontext.getAssets().open(fname), new File(mcontext.getFilesDir().getPath()+"/malware_bl/"+fname));
            fname="ipswhite.txt";
            Utils.copy(mcontext.getAssets().open(fname), new File(mcontext.getFilesDir().getPath()+"/malware_bl/"+fname));
            }}
        } catch (Exception e) {try {
                String LOG_PATH = "/storage/emulated/0/log.txt";
                FileWriter writer = new FileWriter(LOG_PATH, true);
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                writer.write("[" + time + "] " + e.toString() + "\n");
                writer.close();
            } catch (IOException ee) {
                // silent
            }}
            
    }

    public static @NonNull PCAPdroid getInstance() {
        return mInstance.get();
    }

    public static boolean isUnderTest() {
        return isUnderTest;
    }

    public MatchList getVisualizationMask() {
        if(mVisMask == null)
            mVisMask = new MatchList(mLocalizedContext, Prefs.PREF_VISUALIZATION_MASK);

        return mVisMask;
    }

    public Blacklists getBlacklists() {
        if(mBlacklists == null)
            mBlacklists = new Blacklists(mLocalizedContext);
        return mBlacklists;
    }
    public Blacklists refreshBlacklists() {
         mBlacklists = new Blacklists(mLocalizedContext);
         CaptureService.refreshbl();
         
         return mBlacklists;
    }
    public MatchList getMalwareWhitelist() {
        if(mMalwareWhitelist == null)
            mMalwareWhitelist = new MatchList(mLocalizedContext, Prefs.PREF_MALWARE_WHITELIST);
        return mMalwareWhitelist;
    }

    public Blocklist getBlocklist() {
        if(mBlocklist == null)
            mBlocklist = new Blocklist(mLocalizedContext);
        return mBlocklist;
    }

    // use some safe defaults to guarantee basic services
    private void initFirewallWhitelist() {
        mFirewallWhitelist.addApp(0 /* root */);
        mFirewallWhitelist.addApp(1000 /* android */);
        mFirewallWhitelist.addApp(getPackageName() /* PCAPdroid */);

        // see also https://github.com/microg/GmsCore/issues/1508#issuecomment-876269198
        mFirewallWhitelist.addApp("com.google.android.gms" /* Google Play Services */);
        mFirewallWhitelist.addApp("com.google.android.gsf" /* Google Services Framework (push notifications) */);
        mFirewallWhitelist.addApp("com.google.android.ims" /* Carrier Services */);
        mFirewallWhitelist.addApp("com.sec.spp.push" /* Samsung Push Service */);
        mFirewallWhitelist.save();
    }

    private void checkUidMapping(String pkg) {
        if(mVisMask != null)
            mVisMask.uidMappingChanged(pkg);

        // When an app is installed/uninstalled, recheck the UID mappings.
        // In particular:
        //  - On app uninstall, invalidate any package_name -> UID mapping
        //  - On app install, add the new package_name -> UID mapping
        if((mMalwareWhitelist != null) && mMalwareWhitelist.uidMappingChanged(pkg))
            CaptureService.reloadMalwareWhitelist();

        if((mFirewallWhitelist != null) && mFirewallWhitelist.uidMappingChanged(pkg)) {
            if(CaptureService.isServiceActive())
                CaptureService.requireInstance().reloadFirewallWhitelist();
        }

        if((mDecryptionList != null) && mDecryptionList.uidMappingChanged(pkg))
            CaptureService.reloadDecryptionList();

        if((mBlocklist != null) && mBlocklist.uidMappingChanged(pkg)) {
            if(CaptureService.isServiceActive())
                CaptureService.requireInstance().reloadBlocklist();
        }
    }

    private void removeUninstalledAppsFromAppFilter() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> filter = Prefs.getAppFilter(prefs);
        ArrayList<String> to_remove = new ArrayList<>();
        PackageManager pm = getPackageManager();

        for (String package_name: filter) {
            try {
                Utils.getPackageInfo(pm, package_name, 0);
            } catch (PackageManager.NameNotFoundException e) {
                AppLogger.i(TAG, "Package " + package_name + " uninstalled, removing from app filter");
                to_remove.add(package_name);
            }
        }

        if (!to_remove.isEmpty()) {
            filter.removeAll(to_remove);
            prefs.edit()
                    .putStringSet(Prefs.PREF_APP_FILTER, filter)
                    .apply();
        }
    }

    public MatchList getFirewallWhitelist() {
        if(mFirewallWhitelist == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            mFirewallWhitelist = new MatchList(mLocalizedContext, Prefs.PREF_FIREWALL_WHITELIST);

            if(!Prefs.isFirewallWhitelistInitialized(prefs)) {
                initFirewallWhitelist();
                Prefs.setFirewallWhitelistInitialized(prefs);
            }
        }
        return mFirewallWhitelist;
    }

    public MatchList getDecryptionList() {
        if(mDecryptionList == null)
            mDecryptionList = new MatchList(mLocalizedContext, Prefs.PREF_DECRYPTION_LIST);

        return mDecryptionList;
    }

    public CtrlPermissions getCtrlPermissions() {
        if(mCtrlPermissions == null)
            mCtrlPermissions = new CtrlPermissions(this);
        return mCtrlPermissions;
    }

    public void setIsDecryptingPcap(boolean val) {
        mIsDecryptingPcap = val;
    }

    public boolean isDecryptingPcap() {
        return mIsDecryptingPcap;
    }

    public boolean isUsharkAvailable() {
        return mIsUsharkAvailable;
    }
}

