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
 * Copyright 2020-24 - Emanuele Faranda
 */

package com.emanuelef.remote_capture;

import com.secureguard.mdm.utils.AppLogger;
import com.secureguard.mdm.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;
/*
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
*/
import com.emanuelef.remote_capture.activities.CaptureCtrl;
import com.emanuelef.remote_capture.activities.ConnectionsActivity;
import com.emanuelef.remote_capture.activities.FirewallActivity;
import com.emanuelef.remote_capture.activities.MainActivity;
import com.emanuelef.remote_capture.fragments.ConnectionsFragment;
import com.emanuelef.remote_capture.model.AppDescriptor;
import com.emanuelef.remote_capture.model.BlacklistDescriptor;
import com.emanuelef.remote_capture.model.Blocklist;
import com.emanuelef.remote_capture.model.CaptureSettings;
import com.emanuelef.remote_capture.model.ConnectionDescriptor;
import com.emanuelef.remote_capture.model.ConnectionUpdate;
import com.emanuelef.remote_capture.model.FilterDescriptor;
import com.emanuelef.remote_capture.model.Geomodel;
import com.emanuelef.remote_capture.model.MatchList;
import com.emanuelef.remote_capture.model.PortMapping;
import com.emanuelef.remote_capture.model.Prefs;
import com.emanuelef.remote_capture.model.CaptureStats;
import com.emanuelef.remote_capture.pcap_dump.FileDumper;
import com.emanuelef.remote_capture.pcap_dump.HTTPServer;
import com.emanuelef.remote_capture.interfaces.PcapDumper;
import com.emanuelef.remote_capture.pcap_dump.TCPDumper;
import com.emanuelef.remote_capture.pcap_dump.UDPDumper;
import com.pcapdroid.mitm.MitmAPI;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.preference.PreferenceManager;
import java.util.Observer;
import java.util.Observable;
import com.emanuelef.remote_capture.fragments.StatusFragment;
import com.emanuelef.remote_capture.activities.LogUtil;
import com.obsex.obseimp;
import com.obsex.obseobj;
import android.net.NetworkInfo;
import android.icu.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.io.InputStream;
import android.os.AsyncTask;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.json.JSONObject;
import org.json.JSONException;


public class CaptureService extends VpnService implements Runnable {
    private static final String TAG = "CaptureService";
    private static final String VpnSessionName = "PCAPdroid VPN";
    private static final String NOTIFY_CHAN_VPNSERVICE = "VPNService";
    private static final String NOTIFY_CHAN_MALWARE_DETECTION = "Malware detection";
    private static final String NOTIFY_CHAN_OTHER = "Other";
    private static final int VPN_MTU = 10000;
    public static final int NOTIFY_ID_VPNSERVICE = 1;
    public static final int NOTIFY_ID_LOW_MEMORY = 2;
    public static final int NOTIFY_ID_APP_BLOCKED = 3;
    private static CaptureService INSTANCE;
    private static boolean HAS_ERROR = false;
    final ReentrantLock mLock = new ReentrantLock();
    final Condition mCaptureStopped = mLock.newCondition();
    private ParcelFileDescriptor mParcelFileDescriptor;
    private boolean mIsAlwaysOnVPN;
    private boolean mRevoked;
    private SharedPreferences mPrefs;
    private CaptureSettings mSettings;
    private Billing mBilling;
    private Handler mHandler;
    private Thread mCaptureThread;
    private Thread mBlacklistsUpdateThread;
    private Thread mConnUpdateThread;
    private Thread mDumperThread;
    private MitmReceiver mMitmReceiver;
    private final LinkedBlockingDeque<Pair<ConnectionDescriptor[], ConnectionUpdate[]>> mPendingUpdates = new LinkedBlockingDeque<>(32);
    private LinkedBlockingDeque<byte[]> mDumpQueue;
    private String vpn_ipv4;
    private String vpn_dns;
    private String dns_server;
    private long last_bytes;
    private int last_connections;
    private int[] mAppFilterUids;
    private PcapDumper mDumper;
    private ConnectionsRegister conn_reg;
    private Uri mPcapUri;
    private String mPcapFname;
    private Notification.Builder mStatusBuilder;
    private Notification.Builder mMalwareBuilder;
    private long mMonitoredNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private AppsResolver mNativeAppsResolver; // can only be accessed by native code to avoid concurrency issues
    private Geolocation mNativeGeolocation;   // only native
    private boolean mMalwareDetectionEnabled;
    private boolean mBlacklistsUpdateRequested;
    private boolean mFirewallEnabled;
    private boolean mBlockPrivateDns;
    private boolean mDnsEncrypted;
    private boolean mStrictDnsNoticeShown;
    private boolean mQueueFull;
    private boolean mStopping;
    private static Blacklists mBlacklists;
    private Blocklist mBlocklist;
    private MatchList mMalwareWhitelist;
    private MatchList mFirewallWhitelist;
    private MatchList mDecryptionList;
    private SparseArray<String> mIfIndexToName;
    private boolean mSocks5Enabled;
    private String mSocks5Address;
    private int mSocks5Port;
    private String mSocks5Auth;
//    private static final MutableLiveData<CaptureStats> lastStats = new MutableLiveData<>();
  //  private static final MutableLiveData<ServiceStatus> serviceStatus = new MutableLiveData<>();
    private static obseimp lastStats=new obseimp();
    private static obseimp serviceStatus=new obseimp();
    private boolean mLowMemory;
    private BroadcastReceiver mNewAppsInstallReceiver;
    private Utils.PrivateDnsMode mPrivateDnsMode;

    /* The maximum connections to log into the ConnectionsRegister. Older connections are dropped.
     * Max estimated memory usage: less than 4 MB (+8 MB with payload mode minimal). */
    public static final int CONNECTIONS_LOG_SIZE = 8192;

    /* The IP address of the virtual network interface */
    public static final String VPN_IP_ADDRESS = "10.215.173.1";
    public static final String VPN_IP6_ADDRESS = "fd00:2:fd00:1:fd00:1:fd00:1";

    /* The DNS server IP address to use to internally analyze the DNS requests.
     * It must be in the same subnet of the VPN network interface.
     * After the analysis, requests will be routed to the primary DNS server. */
    public static final String VPN_VIRTUAL_DNS_SERVER = "10.215.173.2";

    public enum ServiceStatus {
        STOPPED,
        STARTED
        }

    private static boolean mNativeLibraryLoaded = false;

    static {
        /* Load native library */
        try {
            System.loadLibrary("capture");
            CaptureService.initPlatformInfo(Utils.getAppVersionString(), Utils.getDeviceModel(), Utils.getOsVersion());
            mNativeLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            // This should only happen while running tests or on unsupported architectures (e.g. x86 emulator)
            //e.printStackTrace();
            //LogUtil.logToFile(e.toString());
        }
    }

    public static boolean isNativeLibraryLoaded() {
        return mNativeLibraryLoaded;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base.createConfigurationContext(Utils.getLocalizedConfig(base)));
    }

    @Deprecated
    @Override
    public void onCreate() {
        AppLogger.d(CaptureService.TAG, "onCreate");
        AppLogger.e(CaptureService.TAG, "onCreate");
        AppsResolver.clearMappedApps();
        mNativeAppsResolver = new AppsResolver(this);
        mNativeGeolocation = new Geolocation(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings = new CaptureSettings(this, mPrefs); // initialize to prevent NULL pointer exceptions in methods (e.g. isRootCapture)

        INSTANCE = this;
        super.onCreate();
        new Handler().postDelayed(new Runnable(){
                @Deprecated
                @Override
                public void run() {
                    new Handler().postDelayed(this,86400000);//24 hours
                    if(CaptureService.isServiceActive()){
                        CaptureService.requestBlacklistsUpdate();
                    }
                }
            },60000);//60 seconds
        handler = new Handler();
        
        if(!Prefs.isNetfree(mPrefs)){
            startPeriodicCheck();
        }
    }
    private static Handler handler;
    private static Runnable runnable;
    private boolean previousNetfree = false;
    private void startPeriodicCheck() {
        runnable = new Runnable() {
            @Override
            public void run() {
                new CheckNetfreeTask().execute();
                handler.postDelayed(this, 30000); // 30 ׳©׳ ׳™׳•׳×
            }
        };
        handler.post(runnable);
    }
    static boolean cunetfree=false;
    static String lastup="";
    private class CheckNetfreeTask extends AsyncTask<Void, Void, Boolean> {
        // the crt checker -
        /*
        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean netfree = false; // ׳‘׳¨׳™׳¨׳× ׳׳—׳“׳

            try {
                //LogUtil.logToFile("s");
                URL url = new URL("http://netfree.link/netfree-ca.crt");
                
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                //connection.setRequestMethod("GET");
                //connection.connect();
                //connection.setInstanceFollowRedirects(true);
                //LogUtil.logToFile("s3");
                //LogUtil.logToFile("c="+connection.getResponseCode()+connection.getResponseMessage()+connection.getDoInput()+(connection.getErrorStream()!=null));
                //  LogUtil.logToFile("i="+connection.getInputStream());

                // LogUtil.logToFile("res="+connection.getResponseCode()+connection.getDoInput()+(connection.getInputStream()!=null));
                /*FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                 InputStream inputStream=connection.getErrorStream();
                 if(inputStream!=null){
                 byte[] buffer = new byte[4096];
                 long bytesRead;
                 while ((bytesRead = inputStream.read(buffer)) != -1) {
                 fileOutputStream.write(buffer, 0,(int) bytesRead);
                 }
                 }*/
      /*          int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    netfree = true; // ׳”׳×׳•׳›׳ ׳§׳™׳™׳
                }
                /*if (responseCode == HttpURLConnection.HTTP_OK) {
                    FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                    InputStream inputStream=connection.getInputStream();
                    if(inputStream!=null){
                        byte[] buffer = new byte[4096];
                        long bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0,(int) bytesRead);
                        }
                    }
                }else{
                    FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                    InputStream inputStream=connection.getErrorStream();
                    if(inputStream!=null){
                        byte[] buffer = new byte[4096];
                        long bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0,(int) bytesRead);
                        }
                    }
                }*/
     /*       } catch (Exception e) {
                // ׳‘׳׳§׳¨׳” ׳©׳ IOException, ׳‘׳“׳•׳§ ׳׳ ׳™׳© ׳—׳™׳‘׳•׳¨ ׳׳׳™׳ ׳˜׳¨׳ ׳˜
                //LogUtil.logToFile(e.toString()+isInternetAvailablenew());
                netfree = isInternetAvailablenew() ? false : netfree;
            }
            return netfree;
        }*/
        // the api checker -
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            
            boolean netfree = false;
            if(mPrefs.getBoolean(Prefs.PREF_NETFREEb,false)){
                HttpURLConnection connection =null;
                try {
                    // the crt checker -
                    //LogUtil.logToFile("s");
                    URL url = new URL("http://netfree.link/netfree-ca.crt");
                    connection = (HttpURLConnection) url.openConnection();
                    //connection.setRequestMethod("GET");
                    //connection.connect();
                    //connection.setInstanceFollowRedirects(true);
                    //LogUtil.logToFile("s3");
                    //LogUtil.logToFile("c="+connection.getResponseCode()+connection.getResponseMessage()+connection.getDoInput()+(connection.getErrorStream()!=null));
                    //  LogUtil.logToFile("i="+connection.getInputStream());

                    // LogUtil.logToFile("res="+connection.getResponseCode()+connection.getDoInput()+(connection.getInputStream()!=null));
                    /*FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                     InputStream inputStream=connection.getErrorStream();
                     if(inputStream!=null){
                     byte[] buffer = new byte[4096];
                     long bytesRead;
                     while ((bytesRead = inputStream.read(buffer)) != -1) {
                     fileOutputStream.write(buffer, 0,(int) bytesRead);
                     }
                     }*/
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        netfree = true; // ׳”׳×׳•׳›׳ ׳§׳™׳™׳
                        //LogUtil.logToFile("netfree=ok");
                    }
                    if(connection.getResponseCode()==connection.HTTP_MOVED_TEMP){
                        if(connection.getHeaderField("Location").equals("https://netfree.link/go/install-cert-x2")){
                            netfree = true;
                            //LogUtil.logToFile("netfreeloc=netfree");
                        }
                    }
                    /*if (responseCode == HttpURLConnection.HTTP_OK) {
                     FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                     InputStream inputStream=connection.getInputStream();
                     if(inputStream!=null){
                     byte[] buffer = new byte[4096];
                     long bytesRead;
                     while ((bytesRead = inputStream.read(buffer)) != -1) {
                     fileOutputStream.write(buffer, 0,(int) bytesRead);
                     }
                     }
                     }else{
                     FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                     InputStream inputStream=connection.getErrorStream();
                     if(inputStream!=null){
                     byte[] buffer = new byte[4096];
                     long bytesRead;
                     while ((bytesRead = inputStream.read(buffer)) != -1) {
                     fileOutputStream.write(buffer, 0,(int) bytesRead);
                     }
                     }
                     }*/
                } catch (Exception e) {
                    // ׳‘׳׳§׳¨׳” ׳©׳ IOException, ׳‘׳“׳•׳§ ׳׳ ׳™׳© ׳—׳™׳‘׳•׳¨ ׳׳׳™׳ ׳˜׳¨׳ ׳˜
                    //LogUtil.logToFile(e.toString()+isInternetAvailablenew());
                    netfree = isInternetAvailablenew() ? false : netfree;
                }finally {
                    if (connection != null) {
                        try{
                            connection.disconnect();
                        }catch(Exception e){LogUtil.logToFile(e.toString());}
                    }
                }
            }else if(mPrefs.getBoolean(Prefs.PREF_NETFREE,false)){
                // the api checker -
            HttpURLConnection connection=null;
            try {
                URL url = new URL("https://api.internal.netfree.link/user/0");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(15000);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                   // netfree = true;
                    netfree= NetfreeUser.parseFromJson(response.toString()).isNetFree;
                }
                
            } catch (Exception e) {
                // ׳‘׳׳§׳¨׳” ׳©׳ IOException, ׳‘׳“׳•׳§ ׳׳ ׳™׳© ׳—׳™׳‘׳•׳¨ ׳׳׳™׳ ׳˜׳¨׳ ׳˜
                //LogUtil.logToFile(e.toString()+isInternetAvailablenew());
                netfree = isInternetAvailablenew() ? false : netfree;
            }finally {
                if (connection != null) {
                    try{
                    connection.disconnect();
                    }catch(Exception e){LogUtil.logToFile(e.toString());}
                }
            }
            }else if(mPrefs.getBoolean(Prefs.PREF_RIMON,false)){
                HttpURLConnection connection =null;
                try {
                    // the crt checker -
                    //LogUtil.logToFile("s");
                    URL url = new URL("https://horadah.click/%D7%90%D7%AA%D7%A8%D7%95%D7%92-%D7%95%D7%A8%D7%99%D7%9E%D7%95%D7%9F/%D7%90%D7%AA%D7%A8%D7%95%D7%92.php");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(15000);
                    //connection.setRequestMethod("GET");
                    //connection.connect();
                    //connection.setInstanceFollowRedirects(true);
                    //LogUtil.logToFile("s3");
                    //LogUtil.logToFile("c="+connection.getResponseCode()+connection.getResponseMessage()+connection.getDoInput()+(connection.getErrorStream()!=null));
                    //  LogUtil.logToFile("i="+connection.getInputStream());

                    // LogUtil.logToFile("res="+connection.getResponseCode()+connection.getDoInput()+(connection.getInputStream()!=null));
                    /*FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                     InputStream inputStream=connection.getErrorStream();
                     if(inputStream!=null){
                     byte[] buffer = new byte[4096];
                     long bytesRead;
                     while ((bytesRead = inputStream.read(buffer)) != -1) {
                     fileOutputStream.write(buffer, 0,(int) bytesRead);
                     }
                     }*/
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        if(connection.getHeaderField("Rimon")!=null){
                            if(connection.getHeaderField("Rimon").equals("RWC_BLOCK")){
                                netfree = true;
                                LogUtil.logToFile("detected etrog");
                            }
                        }
                    }
                    /*if (responseCode == HttpURLConnection.HTTP_OK) {
                     FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                     InputStream inputStream=connection.getInputStream();
                     if(inputStream!=null){
                     byte[] buffer = new byte[4096];
                     long bytesRead;
                     while ((bytesRead = inputStream.read(buffer)) != -1) {
                     fileOutputStream.write(buffer, 0,(int) bytesRead);
                     }
                     }
                     }else{
                     FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/emulated/0/s.txt"));
                     InputStream inputStream=connection.getErrorStream();
                     if(inputStream!=null){
                     byte[] buffer = new byte[4096];
                     long bytesRead;
                     while ((bytesRead = inputStream.read(buffer)) != -1) {
                     fileOutputStream.write(buffer, 0,(int) bytesRead);
                     }
                     }
                     }*/
                } catch (Exception e) {
                    // ׳‘׳׳§׳¨׳” ׳©׳ IOException, ׳‘׳“׳•׳§ ׳׳ ׳™׳© ׳—׳™׳‘׳•׳¨ ׳׳׳™׳ ׳˜׳¨׳ ׳˜
                    //LogUtil.logToFile(e.toString()+isInternetAvailablenew());
                    netfree = isInternetAvailablenew() ? false : netfree;
                }finally {
                    if (connection != null) {
                        try{
                            connection.disconnect();
                        }catch(Exception e){LogUtil.logToFile(e.toString());}
                    }
                }
            }
            return netfree;
        }
        @Override
        protected void onPostExecute(Boolean netfree) {
            LogUtil.logToFile("netfree="+netfree);
            cunetfree=netfree;
            lastup=new SimpleDateFormat("HH:mm:ss").format(new Date());
            try{
                //LogUtil.logToFile(lastup+(cunetfree?"׳ ׳˜׳₪׳¨׳™":"׳₪׳×׳•׳—"));
            }catch(Exception e){LogUtil.logToFile(e.toString());}
            if (netfree != previousNetfree) { // ׳׳ ׳™׳© ׳©׳™׳ ׳•׳™ ׳‘׳¢׳¨׳
                startVpn(netfree);
                if(!Prefs.isNetfree(mPrefs)){
                setisNetfree(netfree);
                LogUtil.logToFile("n="+netfree);
                }
                previousNetfree = netfree; // ׳¢׳“׳›׳ ׳׳× ׳”׳¢׳¨׳ ׳”׳§׳•׳“׳
            }
        }
    }
    static class NetfreeUser {

        private final boolean isNetFree;
        private final String userKey;
        private final String enc;
        private final int userId;
        private final String serverName;
        private final String serverInfo;
        private final int plan;
        private final int sectorId;
        private final String userIp;

        // ׳§׳•׳ ׳¡׳˜׳¨׳§׳˜׳•׳¨ ׳׳׳
        public NetfreeUser(boolean isNetFree, String userKey, String enc, int userId, 
                           String serverName, String serverInfo, int plan, int sectorId, String userIp) {
            this.isNetFree = isNetFree;
            this.userKey = userKey;
            this.enc = enc;
            this.userId = userId;
            this.serverName = serverName;
            this.serverInfo = serverInfo;
            this.plan = plan;
            this.sectorId = sectorId;
            this.userIp = userIp;
        }

        /**
         * ׳₪׳•׳ ׳§׳¦׳™׳” ׳”׳׳׳™׳¨׳” ׳׳—׳¨׳•׳–׳× JSON ׳׳׳•׳‘׳™׳™׳§׳˜ NetfreeUser.
         * ׳©׳™׳˜׳” ׳–׳• ׳׳©׳×׳׳©׳× ׳‘-JSONObject ׳”׳¡׳˜׳ ׳“׳¨׳˜׳™ ׳©׳ Android.
         */
        public static NetfreeUser parseFromJson(String jsonString) throws JSONException {
            JSONObject json = new JSONObject(jsonString);

            return new NetfreeUser(
                json.optBoolean("isNetFree", false),
                json.optString("userKey", ""),
                json.optString("enc", ""),
                json.optInt("userId", 0),
                json.optString("servername", ""),
                json.optString("serverInfo", ""),
                json.optInt("plan", 0),
                json.optInt("sectorId", 0),
                json.optString("userIp", "")
            );
        }

        // Getters
        public boolean isNetFree() { return isNetFree; }
        public String getUserKey() { return userKey; }
        public String getEnc() { return enc; }
        public int getUserId() { return userId; }
        public String getServerName() { return serverName; }
        public String getServerInfo() { return serverInfo; }
        public int getPlan() { return plan; }
        public int getSectorId() { return sectorId; }
        public String getUserIp() { return userIp; }
    }
/*
    private boolean isInternetAvailable() {
        try{
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                    return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                } else {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
                }
            }
        }catch(Exception e){}
        return false;
    }*/
    boolean isInternetAvailablenew(){
        final ConnectivityManager cm=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean connected=false;
        for(Network ne:cm.getAllNetworks()){
            NetworkInfo ni=cm.getNetworkInfo(ne);
            if(ni.isConnected()&&ni.getType()!=cm.TYPE_VPN){
                connected=true;
                break;
            }

        }
        if(connected){
            NetworkInfo ni=null;
            if(Build.VERSION.SDK_INT>=23){
                ni=cm.getNetworkInfo(cm.getActiveNetwork());
                //LogUtil.logToFile("def==ln="+cm.getAllNetworks().length+"ei="+ni.getExtraInfo()+"re="+ni.getReason()+"st="+ni.getSubtypeName()+"t="+ni.getTypeName()+"ds="+ni.getDetailedState().name()+"s="+ni.getState().name());
            }
            for(Network ne:cm.getAllNetworks()){
                ni=cm.getNetworkInfo(ne);
                //LogUtil.logToFile("ln="+cm.getAllNetworks().length+"ei="+ni.getExtraInfo()+"re="+ni.getReason()+"st="+ni.getSubtypeName()+"t="+ni.getTypeName()+"ds="+ni.getDetailedState().name()+"s="+ni.getState().name());
                NetworkCapabilities nc=cm.getNetworkCapabilities(ne);
                //LogUtil.logToFile("cvpn="+nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)+"tvpn="+nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)+"up="+nc.getLinkUpstreamBandwidthKbps()+"do="+nc.getLinkDownstreamBandwidthKbps()+(Build.VERSION.SDK_INT>=29?("stren="+nc.getSignalStrength()):""));
            }
        }
        return connected;
    }
    private void startVpn(boolean netfree) {
        LogUtil.logToFile("c netfree="+netfree);
        /*try{
         if(MainActivity.islive)
         MainActivity.tvnetfree.setText(lastup+(checkService.cunetfree?"׳ ׳˜׳₪׳¨׳™":"׳₪׳×׳•׳—"));
         }catch(Exception e){LogUtil.logToFile(e.toString());}*/
    }
/*
    private void stopVpn() {
        LogUtil.logToFile("stopped");
    }*/
    public static void refreshbl(){
        mBlacklists = PCAPdroid.getInstance().getBlacklists();
    }
    private int abortStart() {
        stopService();
        updateServiceStatus(ServiceStatus.STOPPED);
        return START_NOT_STICKY;
    }

    private static boolean alwaysOnVpnErrorLogged = false;

    // Android does not provide a reliable API to track the always-on VPN state
    // This function tries to detect but may fail to do so
    private boolean isAlwaysOnVpnDetected() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return isAlwaysOn();

        try {
            String always_on_vpn_app = Settings.Secure.getString(getContentResolver(), "always_on_vpn_app");
            return always_on_vpn_app.equals(getPackageName());
        } catch (Exception e) {
            LogUtil.logToFile(e.toString());
            if (!alwaysOnVpnErrorLogged) {
                AppLogger.w(TAG, "Querying the always-on VPN state failed: " + e);
                alwaysOnVpnErrorLogged = true;
            }
            return false;
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        mStopping = false;

        // startForeground must always be called since the Service is being started with
        // ContextCompat.startForegroundService.
        // NOTE: since Android 12, startForeground cannot be called when the app is in background
        // (unless invoked via an Intent).
        setupNotifications();
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(NOTIFY_ID_VPNSERVICE, getStatusNotification(), 1073741824);
        else
            startForeground(NOTIFY_ID_VPNSERVICE, getStatusNotification());

        // NOTE: onStartCommand may be called when the capture is already running, e.g. if the user
        // turns on the always-on VPN while the capture is running in root mode
        if(mCaptureThread != null) {
            // Restarting the capture requires calling stopAndJoinThreads, which is blocking.
            // Choosing not to support this right now.
            AppLogger.e(TAG, "Restarting the capture is not supported");
            return abortStart();
        }

        if (VpnReconnectService.isAvailable())
            VpnReconnectService.stopService();

        mHandler = new Handler(Looper.getMainLooper());
        mBilling = Billing.newInstance(this);

        AppLogger.d(CaptureService.TAG, "onStartCommand");

        // NOTE: a null intent may be delivered due to START_STICKY
        // It can be simulated by starting the capture, putting PCAPdroid in the background and then running:
        //  adb shell ps | grep remote_capture | awk '{print $2}' | xargs adb shell run-as com.emanuelef.remote_capture.debug kill
        CaptureSettings settings = ((intent == null) ? null : Utils.getSerializableExtra(intent, "settings", CaptureSettings.class));
        if(settings == null) {
            // Use the settings from mPrefs

            // An Intent without extras is delivered in case of always on VPN
            // https://developer.android.com/guide/topics/connectivity/vpn#always-on
            mIsAlwaysOnVPN = (intent != null);
            AppLogger.i(CaptureService.TAG, "Missing capture settings, using SharedPrefs");
        } else {
            // Use the provided settings
            mSettings = settings;
            mIsAlwaysOnVPN = false;
        }
        
        setdebug(Prefs.isdebug(mPrefs));
setisNetfree(Prefs.isNetfree(mPrefs));
        
        mIsAlwaysOnVPN |= isAlwaysOnVpnDetected();

        AppLogger.d(TAG, "alwaysOn? " + mIsAlwaysOnVPN);
        if(mIsAlwaysOnVPN) {
            mSettings.root_capture = false;
            mSettings.input_pcap_path = null;
        }

        if(mSettings.readFromPcap()) {
            // Disable incompatible settings
            mSettings.dump_mode = Prefs.DumpMode.NONE;
            mSettings.app_filter.clear();
            mSettings.socks5_enabled = false;
            mSettings.tls_decryption = false;
            mSettings.root_capture = false;
            mSettings.auto_block_private_dns = false;
            mSettings.capture_interface = mSettings.input_pcap_path;
        }

        // Retrieve DNS server
        String fallbackDnsV4 = Prefs.getDnsServerV4(mPrefs);
        dns_server = fallbackDnsV4;
        mBlockPrivateDns = false;
        mStrictDnsNoticeShown = false;
        mDnsEncrypted = false;
        setPrivateDnsBlocked(false);

        // Map network interfaces
        mIfIndexToName = new SparseArray<>();

        Enumeration<NetworkInterface> ifaces = Utils.getNetworkInterfaces();
        while(ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();

            AppLogger.d(TAG, "ifidx " + iface.getIndex() + " -> " + iface.getName());
            mIfIndexToName.put(iface.getIndex(), iface.getName());
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);
            Network net = cm.getActiveNetwork();

            if(net != null) {
                handleLinkProperties(cm.getLinkProperties(net));

                if(Prefs.useSystemDns(mPrefs) || mSettings.root_capture) {
                    dns_server = Utils.getDnsServer(cm, net);
                    if (dns_server == null)
                        dns_server = fallbackDnsV4;
                    else {
                        mMonitoredNetwork = net.getNetworkHandle();
                        //old
                        //registerNetworkCallbacks();
                        //
                    }
                } else
                    dns_server = fallbackDnsV4;
            }
            //new anyway register
            registerNetworkCallbacks();
            //
        }
        
        LogUtil.logToFile("dns="+dns_server);
        
        vpn_dns = VPN_VIRTUAL_DNS_SERVER;
        vpn_ipv4 = VPN_IP_ADDRESS;
        last_bytes = 0;
        last_connections = 0;
        mLowMemory = false;
        conn_reg = new ConnectionsRegister(this, CONNECTIONS_LOG_SIZE);
        mDumper = null;
        mDumpQueue = null;
        mPendingUpdates.clear();
        mPcapFname = null;
        HAS_ERROR = false;

        // Possibly allocate the dumper
        if(mSettings.dump_mode == Prefs.DumpMode.HTTP_SERVER)
            mDumper = new HTTPServer(this, mSettings.http_server_port, mSettings.pcapng_format);
        else if(mSettings.dump_mode == Prefs.DumpMode.PCAP_FILE) {
            mPcapFname = !mSettings.pcap_name.isEmpty() ? mSettings.pcap_name : Utils.getUniquePcapFileName(this, mSettings.pcapng_format);

            if(!mSettings.pcap_uri.isEmpty())
                mPcapUri = Uri.parse(mSettings.pcap_uri);
            else
                mPcapUri = Utils.getDownloadsUri(this, mPcapFname);

            if(mPcapUri == null)
                return abortStart();

            mDumper = new FileDumper(this, mPcapUri);
        } else if(mSettings.dump_mode == Prefs.DumpMode.UDP_EXPORTER) {
            InetAddress addr;

            try {
                addr = InetAddress.getByName(mSettings.collector_address);
            } catch (UnknownHostException e) {
                reportError(e.getLocalizedMessage());
                e.printStackTrace();
                LogUtil.logToFile(e.toString());
                return abortStart();
            }

            mDumper = new UDPDumper(new InetSocketAddress(addr, mSettings.collector_port), mSettings.pcapng_format);
        } else if(mSettings.dump_mode == Prefs.DumpMode.TCP_EXPORTER) {
            InetAddress addr;

            try {
                addr = InetAddress.getByName(mSettings.collector_address);
            } catch (UnknownHostException e) {
                reportError(e.getLocalizedMessage());
                e.printStackTrace();
                LogUtil.logToFile(e.toString());
                return abortStart();
            }

            mDumper = new TCPDumper(new InetSocketAddress(addr, mSettings.collector_port), mSettings.pcapng_format);
        }

        if(mDumper != null)
        // Max memory usage = (JAVA_PCAP_BUFFER_SIZE * 64) = 32 MB
            mDumpQueue = new LinkedBlockingDeque<>(64);

        mSocks5Address = "";
        mSocks5Enabled = mSettings.socks5_enabled || mSettings.tls_decryption;
        if(mSocks5Enabled) {
            if(mSettings.tls_decryption) {
                // Built-in decryption
                mSocks5Address = "127.0.0.1";
                mSocks5Port = MitmReceiver.TLS_DECRYPTION_PROXY_PORT;
                mSocks5Auth = Utils.genRandomString(8) + ":" + Utils.genRandomString(8);

                mMitmReceiver = new MitmReceiver(this, mSettings, mSocks5Auth);
                try {
                    if(!mMitmReceiver.start())
                        return abortStart();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.logToFile(e.toString());
                    return abortStart();
                }
            } else {
                // SOCKS5 proxy
                mSocks5Address = mSettings.socks5_proxy_address;
                mSocks5Port = mSettings.socks5_proxy_port;

                if(!mSettings.socks5_username.isEmpty() && !mSettings.socks5_password.isEmpty())
                    mSocks5Auth = mSettings.socks5_username + ":" + mSettings.socks5_password;
                else
                    mSocks5Auth = null;
            }
        }

        if(mSettings.tls_decryption && !mSettings.root_capture && !mSettings.readFromPcap())
            mDecryptionList = PCAPdroid.getInstance().getDecryptionList();
        else
            mDecryptionList = null;

        if ((mSettings.app_filter != null) && (!mSettings.app_filter.isEmpty())) {
            ArrayList<Integer> uids = new ArrayList<>();

            for (String package_name: mSettings.app_filter) {
                int uid;

                try {
                    uid = Utils.getPackageUid(getPackageManager(), package_name, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    LogUtil.logToFile(e.toString());
                    continue;
                }

                uids.add(uid);
            }

            // populate the array only with resolved UIDs
            mAppFilterUids = new int[uids.size()];

            int i = 0;
            for (Integer uid: uids)
                mAppFilterUids[i++] = uid;
        } else
            mAppFilterUids = new int[0];

        mMalwareDetectionEnabled = !mSettings.readFromPcap() && Prefs.isMalwareDetectionEnabled(this, mPrefs);
        mFirewallEnabled = !mSettings.readFromPcap() && Prefs.isFirewallEnabled(this, mPrefs);

        if(!mSettings.root_capture && !mSettings.readFromPcap()) {
            AppLogger.i(TAG, "Using DNS server " + dns_server);

            // VPN
            /* In order to see the DNS packets into the VPN we must set an internal address as the DNS
             * server. */
            Builder builder = new Builder()
                .setMtu(VPN_MTU);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                builder.setMetered(false);

            if (getIPv4Enabled() == 1) {
                builder.addAddress(vpn_ipv4, 30)
                    .addRoute("0.0.0.0", 1)
                    .addRoute("128.0.0.0", 1)
                    .addDnsServer(vpn_dns);
            }

            if (getIPv6Enabled() == 1) {
                builder.addAddress(VPN_IP6_ADDRESS, 128);

                // Route unicast IPv6 addresses
                builder.addRoute("2000::", 3);
                builder.addRoute("fc00::", 7);

                try {
                    builder.addDnsServer(InetAddress.getByName(Prefs.getDnsServerV6(mPrefs)));
                } catch (UnknownHostException | IllegalArgumentException e) {
                    AppLogger.w(TAG, "Could not set IPv6 DNS server");
                    LogUtil.logToFile(e.toString());
                }
            }

            if ((mSettings.app_filter != null) && (!mSettings.app_filter.isEmpty())) {
                AppLogger.d(TAG, "Setting app filter: " + mSettings.app_filter);

                try {
                    // NOTE: the API requires a package name, however it is converted to a UID
                    // (see Vpn.java addUserToRanges). This means that vpn routing happens on a UID basis,
                    // not on a package-name basis!
                    for (String package_name: mSettings.app_filter)
                        builder.addAllowedApplication(package_name);
                } catch (PackageManager.NameNotFoundException e) {
                    String msg = String.format(getResources().getString(R.string.app_not_found), mSettings.app_filter);
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    LogUtil.logToFile(e.toString());
                    return abortStart();
                }
            } else {
                // VPN exceptions
                Set<String> exceptions = mPrefs.getStringSet(Prefs.PREF_VPN_EXCEPTIONS, new HashSet<>());
                for(String packageName: exceptions) {
                    try {
                        builder.addDisallowedApplication(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        LogUtil.logToFile(e.toString());
                    }
                }

                if(mSettings.tls_decryption) {
                    // Exclude the mitm addon traffic in case system-wide decryption is performed
                    // Important: cannot call addDisallowedApplication with addAllowedApplication
                    /*try {
                        builder.addDisallowedApplication(MitmAPI.PACKAGE_NAME);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }*/
                }
            }

            if(Prefs.isPortMappingEnabled(mPrefs)) {
                PortMapping portMap = new PortMapping(this);
                Iterator<PortMapping.PortMap> it = portMap.iter();
                while (it.hasNext()) {
                    PortMapping.PortMap mapping = it.next();
                    addPortMapping(mapping.ipproto, mapping.orig_port, mapping.redirect_port, mapping.redirect_ip);
                }
            }

            try {
                mParcelFileDescriptor = builder.setSession(CaptureService.VpnSessionName).establish();
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                e.printStackTrace();
                LogUtil.logToFile(e.toString());
                Utils.showToast(this, R.string.vpn_setup_failed);
                return abortStart();
            }
        }

        mMalwareWhitelist = PCAPdroid.getInstance().getMalwareWhitelist();
        mBlacklists = PCAPdroid.getInstance().getBlacklists();
        if(mMalwareDetectionEnabled && !mBlacklists.needsUpdate(true))
            reloadBlacklists();
        checkBlacklistsUpdates(true);

        mBlocklist = PCAPdroid.getInstance().getBlocklist();
        mFirewallWhitelist = PCAPdroid.getInstance().getFirewallWhitelist();

        mConnUpdateThread = new Thread("UpdateListener"){public void run(){
            connUpdateWork();
        }};
        mConnUpdateThread.start();

        if(mDumper != null) {
            mDumperThread = new Thread( "DumperThread"){
                public void run(){
                    dumpWork();
                }
            };
            mDumperThread.start();
        }

        if(mFirewallEnabled) {
            mNewAppsInstallReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // executed on the main thread
                    if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
                        boolean newInstall = !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                        String packageName = intent.getData().getSchemeSpecificPart();

                        if(newInstall && Prefs.blockNewApps(mPrefs)) {
                            if(!mBlocklist.addApp(packageName))
                                return;

                            mBlocklist.save();
                            reloadBlocklist();

                            AppDescriptor app = AppsResolver.resolveInstalledApp(getPackageManager(), packageName, 0);
                            String label = (app != null) ? app.getName() : packageName;

                            AppLogger.i(TAG, "Blocking newly installed app: " + packageName + ((app != null) ? " - " + app.getUid() : ""));

                            PendingIntent pi = PendingIntent.getActivity(CaptureService.this, 0,
                                                                         new Intent(CaptureService.this, FirewallActivity.class), Utils.getIntentFlags(0));

                            PendingIntent unblockIntent = PendingIntent.getBroadcast(CaptureService.this, 0,
                                                                                     new Intent(CaptureService.this, ActionReceiver.class)
                                                                                     .putExtra(ActionReceiver.EXTRA_UNBLOCK_APP, packageName), Utils.getIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));

                            // Notify the user
                            NotificationManager man = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                            if(man.areNotificationsEnabled()) {
                                Notification notification = new Notification.Builder(CaptureService.this, NOTIFY_CHAN_OTHER)
                                    .setContentIntent(pi)
                                    .setSmallIcon(R.drawable.ic_logo)
                                    .setColor(getColor(R.color.colorPrimary))
                                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                                    .setCategory(Notification.CATEGORY_STATUS)
                                    .setContentTitle(getString(R.string.app_blocked))
                                    .setContentText(getString(R.string.app_blocked_info, label))
                                    .setAutoCancel(true)
                                    //.addAction(R.drawable.ic_check_solid, getString(R.string.action_unblock), unblockIntent)
                                    .build();

                                man.notify(NOTIFY_ID_APP_BLOCKED, notification);
                            }
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addDataScheme("package");
            registerReceiver(mNewAppsInstallReceiver, filter);
            
        }

        // Start the native capture thread
        mQueueFull = false;
        mCaptureThread = new Thread(this, "PacketCapture");
        mCaptureThread.start();

        // If the service is killed (e.g. due to low memory), then restart it with a NULL intent
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        AppLogger.d(CaptureService.TAG, "onRevoke");
        mRevoked = true;
        stopService();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        AppLogger.d(CaptureService.TAG, "onDestroy");

        // Do not nullify INSTANCE to allow its settings and the connections register to be accessible
        // after the capture is stopped
        //INSTANCE = null;

        unregisterNetworkCallbacks();

        if(mBlacklists != null)
            mBlacklists.abortUpdate();

        if(mCaptureThread != null)
            mCaptureThread.interrupt();
        if(mBlacklistsUpdateThread != null)
            mBlacklistsUpdateThread.interrupt();

        if(mNewAppsInstallReceiver != null) {
            unregisterReceiver(mNewAppsInstallReceiver);
            mNewAppsInstallReceiver = null;
        }
        try{if(handler!=null&&runnable!=null)handler.removeCallbacks(runnable);}catch (Exception e){LogUtil.logToFile(e.toString());}
        super.onDestroy();
    }

    private void setupNotifications() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // VPN running notification channel
            NotificationChannel chan = new NotificationChannel(NOTIFY_CHAN_VPNSERVICE,
                                                               NOTIFY_CHAN_VPNSERVICE, NotificationManager.IMPORTANCE_LOW); // low: no sound
            chan.setShowBadge(false);
            nm.createNotificationChannel(chan);

            // Blacklisted connection notification channel
            chan = new NotificationChannel(NOTIFY_CHAN_MALWARE_DETECTION,
                                           getString(R.string.malware_detection), NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(chan);

            // Other notifications
            chan = new NotificationChannel(NOTIFY_CHAN_OTHER,
                                           getString(R.string.other_prefs), NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(chan);
        }

        // Status notification builder
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                                                     new Intent(this, MainActivity.class), Utils.getIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));
        mStatusBuilder = new Notification.Builder(this, NOTIFY_CHAN_VPNSERVICE)
            .setSmallIcon(R.drawable.ic_logo)
            .setColor(getColor(R.color.colorPrimary))
            .setContentIntent(pi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentTitle(getResources().getString(R.string.capture_running))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setPriority(Notification.PRIORITY_LOW); // see IMPORTANCE_LOW

        // Malware notification builder
        mMalwareBuilder = new Notification.Builder(this, NOTIFY_CHAN_MALWARE_DETECTION)
            .setSmallIcon(R.drawable.ic_skull)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setPriority(Notification.PRIORITY_HIGH); // see IMPORTANCE_HIGH
    }

    private Notification getStatusNotification() {
        String msg = String.format(getString(R.string.notification_msg),
                                   Utils.formatBytes(last_bytes), Utils.formatNumber(this, last_connections));

        mStatusBuilder.setContentText(msg);

        return mStatusBuilder.build();
    }

    private void updateNotification() {
        if(mStopping)
            return;

        Notification notification = getStatusNotification();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFY_ID_VPNSERVICE, notification);
    }

    public void notifyBlacklistedConnection(ConnectionDescriptor conn) {
        int uid = conn.uid;

        AppsResolver resolver = new AppsResolver(this);
        AppDescriptor app = resolver.getAppByUid(conn.uid, 0);
        if(app == null)
            return;

        FilterDescriptor filter = new FilterDescriptor();
        filter.onlyBlacklisted = true;

        Intent intent = new Intent(this, ConnectionsActivity.class)
            .putExtra("filter", filter)
            .putExtra("query", app.getPackageName());
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                                                     intent, Utils.getIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));

        String rule_label;
        if(conn.isBlacklistedHost())
            rule_label = MatchList.getRuleLabel(this, MatchList.RuleType.HOST, conn.info);
        else
            rule_label = MatchList.getRuleLabel(this, MatchList.RuleType.IP, conn.dst_ip);
        String content = String.format(
            getResources().getString(R.string.malicious_connection_description),
            app.getName(),
            rule_label);

        mMalwareBuilder
            .setContentIntent(pi)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(getResources().getString(R.string.malware_detection))
            .setStyle(new Notification.BigTextStyle().bigText(content))
            .setContentText(content);
        Notification notification = mMalwareBuilder.build();

        // Use the UID as the notification ID to group alerts from the same app
        //mHandler.post(() -> Utils.sendImportantNotification(this, uid, notification));
    }

    public void notifyLowMemory(CharSequence msg) {
        Notification notification = new Notification.Builder(this, NOTIFY_CHAN_OTHER)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_logo)
            .setColor(getColor( R.color.colorPrimary))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(getString(R.string.low_memory))
            .setContentText(msg)
            .build();

        //mHandler.post(() -> Utils.sendImportantNotification(this, NOTIFY_ID_LOW_MEMORY, notification));
    }

   // @RequiresApi(api = Build.VERSION_CODES.M)
    private void registerNetworkCallbacks() {
        if(mNetworkCallback != null)
            return;

        final String fallbackDns = Prefs.getDnsServerV4(mPrefs);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                AppLogger.d(TAG, "onLost " + network);
                
                // If the network goes offline we roll back to the fallback DNS server to
                // avoid possibly using a private IP DNS server not reachable anymore
                if(network.getNetworkHandle() == mMonitoredNetwork) {
                    AppLogger.i(TAG, "Main network " + network + " lost, using fallback DNS " + fallbackDns);
                    dns_server = fallbackDns;
                    mMonitoredNetwork = 0;
                    //unregisterNetworkCallbacks();

                    // change native
                    setDnsServer(dns_server);
                }
                LogUtil.logToFile("lost="+dns_server);
            }
            
            //new
            @Override
            public void onAvailable(@NonNull Network network) {
                
                String fallbackDnsV4 = Prefs.getDnsServerV4(mPrefs);
                dns_server = fallbackDnsV4;
                

                // Map network interfaces
                mIfIndexToName = new SparseArray<>();

                Enumeration<NetworkInterface> ifaces = Utils.getNetworkInterfaces();
                while(ifaces.hasMoreElements()) {
                    NetworkInterface iface = ifaces.nextElement();

                    AppLogger.d(TAG, "ifidx " + iface.getIndex() + " -> " + iface.getName());
                    mIfIndexToName.put(iface.getIndex(), iface.getName());
                }

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);
                    Network  net=network;
                    if(net != null) {
                        handleLinkProperties(cm.getLinkProperties(net));
                        
                        if(Prefs.useSystemDns(mPrefs) || mSettings.root_capture) {
                            dns_server = Utils.getDnsServer(cm, net);
                            if (dns_server == null)
                                dns_server = fallbackDnsV4;
                            else {
                                mMonitoredNetwork = net.getNetworkHandle();
                                //registerNetworkCallbacks();
                            }
                        } else
                            dns_server = fallbackDnsV4;
                            
                    }
                }
                setDnsServer(dns_server);
                LogUtil.logToFile("dns="+dns_server);
            }
            //

            //@RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                AppLogger.d(TAG, "onLinkPropertiesChanged " + network);

                if(network.getNetworkHandle() == mMonitoredNetwork)
                    handleLinkProperties(linkProperties);
            }
        };

        try {
            AppLogger.d(TAG, "registerNetworkCallback");
            cm.registerNetworkCallback(
                new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                mNetworkCallback);
        } catch (SecurityException e) {
            // this is a bug in Android 11 - https://issuetracker.google.com/issues/175055271?pli=1
            e.printStackTrace();
            LogUtil.logToFile(e.toString());
            AppLogger.w(TAG, "registerNetworkCallback failed, DNS server detection disabled");
            dns_server = fallbackDns;
            mNetworkCallback = null;
        }
    }

    private void unregisterNetworkCallbacks() {
        if(mNetworkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);

            try {
                AppLogger.d(TAG, "unregisterNetworkCallback");
                cm.unregisterNetworkCallback(mNetworkCallback);
            } catch(IllegalArgumentException e) {
                AppLogger.w(TAG, "unregisterNetworkCallback failed: " + e);
                LogUtil.logToFile(e.toString());
            }

            mNetworkCallback = null;
        }
    }

    private void handleLinkProperties(LinkProperties linkProperties) {
        if(linkProperties == null)
            return;

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            mPrivateDnsMode = Utils.getPrivateDnsMode(linkProperties);
            AppLogger.i(TAG, "Private DNS: " + mPrivateDnsMode);

            if(mSettings.readFromPcap()) {
                mDnsEncrypted = false;
                setPrivateDnsBlocked(false);
            } else if(!mSettings.root_capture && mSettings.auto_block_private_dns) {
                mDnsEncrypted = mPrivateDnsMode.equals(Utils.PrivateDnsMode.STRICT);
                boolean opportunistic_mode = mPrivateDnsMode.equals(Utils.PrivateDnsMode.OPPORTUNISTIC);

                /* Private DNS can be in one of these modes:
                 *  1. Off
                 *  2. Automatic (default): also called "opportunistic", only use it if not blocked
                 *  3. Strict: private DNS is enforced, Internet unavailable if blocked. User must set a specific DNS server.
                 * When in opportunistic mode, PCAPdroid will block private DNS connections to force the use of plain-text
                 * DNS queries, which can be extracted by PCAPdroid. */
                if (mBlockPrivateDns != opportunistic_mode) {
                    mBlockPrivateDns = opportunistic_mode;
                    setPrivateDnsBlocked(mBlockPrivateDns);
                }
            } else {
                // in root capture we don't block private DNS requests in opportunistic mode
                mDnsEncrypted = !mPrivateDnsMode.equals(Utils.PrivateDnsMode.DISABLED);
                setPrivateDnsBlocked(false);
            }

            if(mDnsEncrypted && !mStrictDnsNoticeShown) {
                mStrictDnsNoticeShown = true;
                Utils.showToastLong(this, R.string.private_dns_message_notice);
            }
            LogUtil.logToFile("linkprop="+mBlockPrivateDns);
        }
    }

    private void signalServicesTermination() {
        mPendingUpdates.offer(new Pair<>(null, null));
        stopPcapDump();
    }

    // NOTE: do not call this on the main thread, otherwise it will be an ANR
    private void stopAndJoinThreads() {
        try{
            signalServicesTermination();

            AppLogger.d(TAG, "Joining threads...");

            while((mConnUpdateThread != null) && (mConnUpdateThread.isAlive())) {
                try {
                    AppLogger.d(TAG, "Joining conn update thread...");
                    mConnUpdateThread.join();
                } catch (InterruptedException ignored) {
                    mPendingUpdates.offer(new Pair<>(null, null));
                    LogUtil.logToFile(ignored.toString());
                }
            }
            mConnUpdateThread = null;

            while((mDumperThread != null) && (mDumperThread.isAlive())) {
                try {
                    AppLogger.d(TAG, "Joining dumper thread...");
                    mDumperThread.join();
                } catch (InterruptedException ignored) {
                    stopPcapDump();
                    LogUtil.logToFile(ignored.toString());
                }
            }
            mDumperThread = null;
            mDumper = null;

            if(mMitmReceiver != null) {
                try {
                    mMitmReceiver.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.logToFile(e.toString());
                }
                mMitmReceiver = null;
            }
        } catch (Exception e){LogUtil.logToFile(e.toString());}
    }

    /* Stops the running Service. The SERVICE_STATUS_STOPPED notification is sent asynchronously
     * when mCaptureThread terminates. */
    @SuppressWarnings("deprecation")
    public static void stopService() {
        CaptureService captureService = INSTANCE;
        AppLogger.d(TAG, "stopService called (instance? " + (captureService != null) + ")");

        if(captureService == null)
            return;

        captureService.mStopping = true;
        try{
            stopPacketLoop();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                captureService.stopForeground(STOP_FOREGROUND_REMOVE);
            else
                captureService.stopForeground(true);

            if(captureService == null)
                return;
            captureService.stopSelf();
        } catch (Exception e){LogUtil.logToFile(e.toString());}
        try{if(handler!=null&&runnable!=null)handler.removeCallbacks(runnable);}catch (Exception e){LogUtil.logToFile(e.toString());}
    }

    /* Check if the VPN service was launched */
    public static boolean isServiceActive() {
        return((INSTANCE != null) &&
            (INSTANCE.mCaptureThread != null));
    }

    public static MitmReceiver.Status getMitmProxyStatus() {
        if((INSTANCE == null) || (INSTANCE.mMitmReceiver == null))
            return MitmReceiver.Status.NOT_STARTED;

        return INSTANCE.mMitmReceiver.getProxyStatus();
    }

    public static boolean isLowMemory() {
        return((INSTANCE != null) && (INSTANCE.mLowMemory));
    }

    public static boolean isAlwaysOnVPN() {
        return((INSTANCE != null) && INSTANCE.mIsAlwaysOnVPN);
    }

    public static boolean checkAlwaysOnVpnActivated() {
        CaptureService instance = INSTANCE;
        if (instance == null)
            return false;

        if (!instance.mIsAlwaysOnVPN && instance.isAlwaysOnVpnDetected()) {
            AppLogger.i(TAG, "Always-on VPN was activated");
            instance.mIsAlwaysOnVPN = true;
            return true;
        }

        return false;
    }

  //  @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean isLockdownVPN() {
        return ((INSTANCE != null) && INSTANCE.isLockdownEnabled());
    }

    private void checkBlacklistsUpdates(boolean firstUpdate) {
        if(!mMalwareDetectionEnabled || (mBlacklistsUpdateThread != null))
            return;

        if(mBlacklistsUpdateRequested || mBlacklists.needsUpdate(firstUpdate)) {
            mBlacklistsUpdateThread = new Thread("Blacklists Update"){
    @Override
    public void run() {
        updateBlacklistsWork();
    }};
            mBlacklistsUpdateThread.start();
        }
    }

    private void updateBlacklistsWork() {
        mBlacklistsUpdateRequested = false;
        mBlacklists.update();
        reloadBlacklists();
        mBlacklistsUpdateThread = null;
    }

    private String getIfname(int ifidx) {
        if(ifidx <= 0)
            return "";

        String rv = mIfIndexToName.get(ifidx);
        if(rv != null)
            return rv;

        // Not found, try to retrieve it
        NetworkInterface iface = null;
        try {
            iface = NetworkInterface.getByIndex(ifidx);
        } catch (SocketException ignored) {LogUtil.logToFile(ignored.toString());}
        rv = (iface != null) ? iface.getName() : "";

        // store it even if not found, to avoid looking up it again
        mIfIndexToName.put(ifidx, rv);
        return rv;
    }

    public static Set<String> getAppFilter() {
        return((INSTANCE != null) ? INSTANCE.mSettings.app_filter : null);
    }

    public static Uri getPcapUri() {
        return ((INSTANCE != null) ? INSTANCE.mPcapUri : null);
    }

    public static String getPcapFname() {
        return ((INSTANCE != null) ? INSTANCE.mPcapFname : null);
    }

    public static boolean isUserDefinedPcapUri() {
        return (INSTANCE == null || !INSTANCE.mSettings.pcap_uri.isEmpty());
    }

    public static long getBytes() {
        return((INSTANCE != null) ? INSTANCE.last_bytes : 0);
    }

    public static String getCollectorAddress() {
        return((INSTANCE != null) ? INSTANCE.mSettings.collector_address : "");
    }

    public static int getCollectorPort() {
        return((INSTANCE != null) ? INSTANCE.mSettings.collector_port : 0);
    }

    public static int getHTTPServerPort() {
        return((INSTANCE != null) ? INSTANCE.mSettings.http_server_port : 0);
    }

    public static Prefs.DumpMode getDumpMode() {
        return((INSTANCE != null) ? INSTANCE.mSettings.dump_mode : Prefs.DumpMode.NONE);
    }

    public static String getDNSServer() {
        return((INSTANCE != null) ? INSTANCE.getDnsServer() : "");
    }

    public static boolean isDNSEncrypted() {
        return((INSTANCE != null) && INSTANCE.mDnsEncrypted);
    }

    public static @NonNull CaptureService requireInstance() {
        CaptureService inst = INSTANCE;
        assert(inst != null);
        return(inst);
    }

    public static @Nullable ConnectionsRegister getConnsRegister() {
        return((INSTANCE != null) ? INSTANCE.conn_reg : null);
    }

    public static @NonNull ConnectionsRegister requireConnsRegister() {
        ConnectionsRegister reg = getConnsRegister();

        assert(reg != null);

        return reg;
    }

    public static Blocklist getBlocklist() {
        return (INSTANCE != null) ? INSTANCE.mBlocklist : null;
    }

    public static MatchList getMalwareWhitelist() {
        return (INSTANCE != null) ? INSTANCE.mMalwareWhitelist : null;
    }

    public static MatchList getFirewallWhitelist() {
        return (INSTANCE != null) ? INSTANCE.mFirewallWhitelist : null;
    }

    public static MatchList getDecryptionList() {
        return (INSTANCE != null) ? INSTANCE.mDecryptionList : null;
    }

    public static boolean isCapturingAsRoot() {
        return((INSTANCE != null) &&
            (INSTANCE.isRootCapture() == 1));
    }

    public static boolean isDecryptingTLS() {
        return((INSTANCE != null) &&
            (INSTANCE.isTlsDecryptionEnabled() == 1));
    }

    public static boolean isReadingFromPcapFile() {
        return((INSTANCE != null) &&
            (INSTANCE.isPcapFileCapture() == 1));
    }

    public static boolean isIPv6Enabled() {
        return((INSTANCE != null) &&
            (INSTANCE.getIPv6Enabled() == 1));
    }

    public static boolean isDecryptionListEnabled() {
        return(INSTANCE != null && (INSTANCE.mDecryptionList != null));
    }

    public static Prefs.PayloadMode getCurPayloadMode() {
        if(INSTANCE == null)
            return Prefs.PayloadMode.MINIMAL;

        return INSTANCE.mSettings.full_payload ? Prefs.PayloadMode.FULL : Prefs.PayloadMode.MINIMAL;
    }

    public static void requestBlacklistsUpdate() {
        if(INSTANCE != null) {
            INSTANCE.mBlacklistsUpdateRequested = true;

            // Wake the update thread to run the blacklist thread
            INSTANCE.mPendingUpdates.offer(new Pair<>(new ConnectionDescriptor[0], new ConnectionUpdate[0]));
        }
    }

    public static String getInterfaceName(int ifidx) {
        String ifname = null;

        if(INSTANCE != null)
            ifname = INSTANCE.getIfname(ifidx);
        return (ifname != null) ? ifname : "";
    }

    // Inside the mCaptureThread
    @Override
    public void run() {
        if(mSettings.root_capture || mSettings.readFromPcap()) {
            // Check for INTERACT_ACROSS_USERS, required to query apps of other users/work profiles
            if(mSettings.root_capture && (checkCallingOrSelfPermission(Utils.INTERACT_ACROSS_USERS) != PackageManager.PERMISSION_GRANTED)) {
                //final boolean success = Utils.rootGrantPermission(this, Utils.INTERACT_ACROSS_USERS);
                //mHandler.post(new Runnable(){
                //        @Override
                 //       public void run() {
                //        Utils.showToast(CaptureService.this, success ? R.string.permission_granted : R.string.permission_grant_fail, "INTERACT_ACROSS_USERS");}});
            }

            runPacketLoop(-1, this, Build.VERSION.SDK_INT);
        } else {
            if(mParcelFileDescriptor != null) {
                int fd = mParcelFileDescriptor.getFd();
                int fd_setsize = getFdSetSize();

                if((fd > 0) && (fd < fd_setsize)) {
                    AppLogger.d(TAG, "VPN fd: " + fd + " - FD_SETSIZE: " + fd_setsize);
                    runPacketLoop(fd, this, Build.VERSION.SDK_INT);

                    // if always-on VPN is stopped, it's not an always-on anymore
                    mIsAlwaysOnVPN = false;
                } else
                    AppLogger.e(TAG, "Invalid VPN fd: " + fd);
            }
        }

        // After the capture is stopped
        if(mMalwareDetectionEnabled)
            mBlacklists.save();

        // Important: the fd must be closed to properly terminate the VPN
        if(mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
                LogUtil.logToFile(e.toString());
            }
            mParcelFileDescriptor = null;
        }

        // NOTE: join the threads here instead in onDestroy to avoid ANR
        stopAndJoinThreads();

        stopService();

        mLock.lock();
        mCaptureThread = null;
        mCaptureStopped.signalAll();
        mLock.unlock();

        // Notify
        mHandler.post(new Runnable(){

                @Override
                public void run() {
                
                
            updateServiceStatus(ServiceStatus.STOPPED);
            // CaptureCtrl.notifyCaptureStopped
            }
        });
    }

    private void connUpdateWork() {
        while(true) {
            Pair<ConnectionDescriptor[], ConnectionUpdate[]> item;
            try {
                item = mPendingUpdates.take();
                //LogUtil.logToFile("taking");
            } catch (InterruptedException e) {
                LogUtil.logToFile(e.toString()+e.getStackTrace()[0].getMethodName()+e.getStackTrace()[0].getLineNumber());
                continue;
            }

            if(item.first == null) { // termination request
                LogUtil.logToFile("taking=null");
                AppLogger.i(TAG, "Connection update thread exit requested");
                break;
            }

            ConnectionDescriptor[] new_conns = item.first;
            ConnectionUpdate[] conns_updates = item.second;

            checkBlacklistsUpdates(false);
            if(mBlocklist.checkGracePeriods())
                mHandler.post(new Runnable(){
                        @Override
                        public void run() {
                            reloadBlocklist();}});

            if(!mLowMemory)
                checkAvailableHeap();

            // synchronize the conn_reg to ensure that newConnections and connectionsUpdates run atomically
            // thus preventing the ConnectionsAdapter from interleaving other operations
            synchronized (conn_reg) {
                if(new_conns.length > 0)
                    conn_reg.newConnections(new_conns);

                if(conns_updates.length > 0)
                    conn_reg.connectionsUpdates(conns_updates);
            }
            //new Automatically clean after 5000 connections
            if(conn_reg.getConnCount()>5000)
                conn_reg.reset();
            //end new
        }
    }

    private void dumpWork() {
        AppLogger.d(TAG, "Starting the dumper");

        try {
            mDumper.startDumper();
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
            LogUtil.logToFile(e.toString());
            reportError(e.getLocalizedMessage());
            mHandler.post(new Runnable(){

                    @Override
                    public void run() {
                    
                    stopPacketLoop();}});
            return;
        }

        AppLogger.d(TAG, "Dumper running");

        while(true) {
            byte[] data;
            try {
                data = mDumpQueue.take();
            } catch (InterruptedException e) {
                LogUtil.logToFile(e.toString());
                continue;
            }

            if(data.length == 0) // termination request
                break;

            try {
                mDumper.dumpData(data);
            } catch (IOException e) {
                // Stop the capture
                e.printStackTrace();
                LogUtil.logToFile(e.toString());
                reportError(e.getLocalizedMessage());
                mHandler.post(new Runnable(){

                        @Override
                        public void run() {
                            stopPacketLoop();}});
                break;
            }
        }

        try {
            mDumper.stopDumper();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.logToFile(e.toString());
        }
    }

    private void checkAvailableHeap() {
        // This does not account per-app jvm limits
        long availableHeap = Utils.getAvailableHeap();

        if(availableHeap <= Utils.LOW_HEAP_THRESHOLD) {
            AppLogger.w(TAG, "Detected low HEAP memory: " + Utils.formatBytes(availableHeap));
            handleLowMemory();
        }
    }

    // NOTE: this is only called on low system memory (e.g. obtained via getMemoryInfo). The app
    // may still run out of heap memory, whose monitoring requires polling (see checkAvailableHeap)
    @Override
    @SuppressWarnings("deprecation")
    public void onTrimMemory(int level) {
        // NOTE: most trim levels are not available anymore since API 34
        String lvlStr = Utils.trimlvl2str(level);
        boolean lowMemory = (level != TRIM_MEMORY_UI_HIDDEN) && (level >= TRIM_MEMORY_RUNNING_LOW);
        boolean critical = lowMemory && (level >= TRIM_MEMORY_COMPLETE);

        AppLogger.d(TAG, "onTrimMemory: " + lvlStr + " - low=" + lowMemory + ", critical=" + critical);

        if(critical && !mLowMemory)
            handleLowMemory();
    }

    private void handleLowMemory() {
        AppLogger.w(TAG, "handleLowMemory called");
        mLowMemory = true;
        boolean fullPayload = getCurPayloadMode() == Prefs.PayloadMode.FULL;

        if(fullPayload) {
            AppLogger.w(TAG, "Disabling full payload");

            // Disable full payload for new connections
            mSettings.full_payload = false;
            setPayloadMode(Prefs.PayloadMode.NONE.ordinal());

            if(mSettings.tls_decryption) {
                // TLS decryption without payload has little use, stop the capture all together
                stopService();
                notifyLowMemory(getString(R.string.capture_stopped_low_memory));
            } else {
                // Release memory for existing connections
                if(conn_reg != null) {
                    conn_reg.releasePayloadMemory();

                    // *possibly* call the gc
                    System.gc();

                    AppLogger.i(TAG, "Memory stats full payload release:\n" + Utils.getMemoryStats(this));
                }

                notifyLowMemory(getString(R.string.full_payload_disabled));
            }
        } else {
            // TODO lower memory consumption (e.g. reduce connections register size)
            AppLogger.w(TAG, "low memory detected, expect crashes");
            notifyLowMemory(getString(R.string.low_memory_info));
        }
    }

    /* The following methods are called from native code */

    public String getVpnIPv4() {
        return(vpn_ipv4);
    }

    public String getVpnDns() {
        return(vpn_dns);
    }

    public String getDnsServer() {
        return(dns_server);
    }

    public String getIpv6DnsServer() { return(Prefs.getDnsServerV6(mPrefs)); }

    public int getSocks5Enabled() { return mSocks5Enabled ? 1 : 0; }

    public String getSocks5ProxyAddress() {  return(mSocks5Address); }

    public int getSocks5ProxyPort() {  return(mSocks5Port);  }

    public String getSocks5ProxyAuth() {  return(mSocks5Auth);  }

    public int getIPv4Enabled() { return((mSettings.ip_mode != Prefs.IpMode.IPV6_ONLY) ? 1 : 0); }

    public int getIPv6Enabled() { return((mSettings.ip_mode != Prefs.IpMode.IPV4_ONLY) ? 1 : 0); }

    public int isVpnCapture() { return (isRootCapture() | isPcapFileCapture()) == 1 ? 0 : 1; }

    public int isRootCapture() { return(mSettings.root_capture ? 1 : 0); }

    public int isPcapFileCapture() { return(mSettings.readFromPcap() ? 1 : 0); }

    public int isTlsDecryptionEnabled() { return mSettings.tls_decryption ? 1 : 0; }

    public int malwareDetectionEnabled() { return(mMalwareDetectionEnabled ? 1 : 0); }

    public int firewallEnabled() { return(mFirewallEnabled ? 1 : 0); }

    public int dumpExtensionsEnabled() { return(mSettings.dump_extensions ? 1 : 0); }

    public int isPcapngEnabled() { return(mSettings.pcapng_format ? 1 : 0); }

    public int[] getAppFilterUids() { return(mAppFilterUids); }

    public int getMitmAddonUid() {
        return MitmAddon.getUid(this);
    }

    public String getCaptureInterface() { return(mSettings.capture_interface); }

    public int getSnaplen() {  return mSettings.snaplen; }

    public int getMaxPktsPerFlow() {  return mSettings.max_pkts_per_flow; }

    public int getMaxDumpSize() {  return mSettings.max_dump_size; }

    public int getPayloadMode() { return getCurPayloadMode().ordinal(); }

    public int getVpnMTU()      { return VPN_MTU; }

    public int getBlockQuickMode() { return mSettings.block_quic_mode.ordinal(); }

    //new
    public int isPayload() { return (Prefs.ispayload(mPrefs)) ? 1 : 0; }
    //end new
    
    // returns 1 if dumpPcapData should be called
    public int pcapDumpEnabled() {
        return((mSettings.dump_mode != Prefs.DumpMode.NONE) ? 1 : 0);
    }

    public String getPcapDumperBpf() { return((mDumper != null) ? mDumper.getBpf() : ""); }

    @Override
    public boolean protect(int socket) {
        // Do not call protect in root mode
        if(mSettings.root_capture)
            return true;

        return super.protect(socket);
    }

    // from NetGuard
    @TargetApi(Build.VERSION_CODES.Q)
    public int getUidQ(int protocol, String saddr, int sport, String daddr, int dport) {
        if (protocol != 6 /* TCP */ && protocol != 17 /* UDP */)
            return Utils.UID_UNKNOWN;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null)
            return Utils.UID_UNKNOWN;

        InetSocketAddress local = new InetSocketAddress(saddr, sport);
        InetSocketAddress remote = new InetSocketAddress(daddr, dport);

        //AppLogger.d(TAG, "Get uid local=" + local + " remote=" + remote);
        return cm.getConnectionOwnerUid(protocol, local, remote);
    }

    public void updateConnections(ConnectionDescriptor[] new_conns, ConnectionUpdate[] conns_updates) {
        if(mQueueFull)
        // if the queue is full, stop receiving updates to avoid inconsistent incr_ids
            return;

        // Put the update into a queue to avoid performing much work on the capture thread.
        // This will be processed by mConnUpdateThread.
        if(!mPendingUpdates.offer(new Pair<>(new_conns, conns_updates))) {
            AppLogger.e(TAG, "The updates queue is full, this should never happen!");
            mQueueFull = true;
            mHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        stopPacketLoop();}});
        }
    }

    public static boolean isUsharkAvailable(Context ctx) {
        return new File(getLibprogPath(ctx, "ushark")).exists();
    }

    // called from native
    public void sendStatsDump(CaptureStats stats) {
        //AppLogger.d(TAG, "sendStatsDump");
        try{
        last_bytes = stats.bytes_sent + stats.bytes_rcvd;
        last_connections = stats.tot_conns;
        mHandler.post(new Runnable(){
                @Override
                public void run() {
                    updateNotification();}});

        // notify the observers
        lastStats.postValue(stats);
        }catch(Exception e){LogUtil.logToFile(e.toString());}
       //AppLogger.d("request notify stat","lol");
        //LogUtil.logToFile("request notify stat");
    }

    // called from native
    public void sendServiceStatus(String cur_status) {
        //AppLogger.i(TAG, "lol lol reloading malware whitelist");
        //LogUtil.logToFile("lol lol reloading malware whitelist");
        updateServiceStatus(cur_status.equals("started") ? ServiceStatus.STARTED : ServiceStatus.STOPPED);
       
    }

    private void updateServiceStatus(final ServiceStatus cur_status) {
        // notify the observers
        // NOTE: new subscribers will receive the STOPPED status right after their registration
       // serviceStatus.postValue(cur_status);
        getMainExecutor().execute(new Runnable(){

                @Override
                public void run() {
             
       try{
           
        serviceStatus.upall(cur_status);
        
        if(cur_status == ServiceStatus.STARTED) {
            if(mMalwareDetectionEnabled)
                reloadMalwareWhitelist();
            if(mDecryptionList != null){
                reloadDecryptionList();
            }
            reloadBlocklist();
            reloadFirewallWhitelist();
        } else if (cur_status == ServiceStatus.STOPPED) {
            if (mRevoked && Prefs.restartOnDisconnect(mPrefs) && !mIsAlwaysOnVPN && (isVpnCapture() == 1)) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    AppLogger.i(TAG, "VPN disconnected, starting reconnect service");

                    final Intent intent = new Intent(CaptureService.this, VpnReconnectService.class);
                    startForegroundService( intent);
                }
            }
        }
        }catch(Exception e){
            LogUtil.logToFile("upstatuserr="+e.toString());
        }
        }});
    }

    // NOTE: to be invoked only by the native code
    public String getApplicationByUid(int uid) {
        AppDescriptor dsc = mNativeAppsResolver.getAppByUid(uid, 0);

        if(dsc == null)
            return "";

        return dsc.getName();
    }

    public String getPackageNameByUid(int uid) {
        AppDescriptor dsc = mNativeAppsResolver.getAppByUid(uid, 0);

        if(dsc == null)
            return "";

        return dsc.getPackageName();
    }

    public void loadUidMapping(int uid, String package_name, String app_name) {
        if (uid < 0)
            return;

        AppDescriptor dsc = mNativeAppsResolver.getAppByUid(uid, 0);

        if ((dsc == null) || !dsc.getPackageName().equals(package_name)) {
            // This uid corresponds to a different app than the one on the Pcapng
            AppsResolver.addMappedApp(uid, package_name, app_name);
        }
    }

    public String getCountryCode(String host) {
       /* if (mNativeGeolocation.isAvailable()) {
            try {
                InetAddress addr = InetAddress.getByName(host);
                return mNativeGeolocation.getCountryCode(addr);
            } catch (UnknownHostException ignored) {}
        }*/

        return "";
    }

    /* Exports a PCAP data chunk */
    public void dumpPcapData(byte[] data) {
        if((mDumper != null) && (data.length > 0)) {
            while(true) {
                try {
                    // wait until the queue has space to insert the data. If the queue is full, we
                    // will experience slow-downs/drops but this is expected
                    mDumpQueue.put(data);
                    break;
                } catch (InterruptedException e) {
                    LogUtil.logToFile(e.toString());
                    // retry
                }
            }
        }
    }

    public void stopPcapDump() {
        if((mDumpQueue != null) && (mDumperThread != null) && (mDumperThread.isAlive()))
            mDumpQueue.offer(new byte[0]);
    }

    public void reportError(final String msg) {
        HAS_ERROR = true;

        mHandler.post(new Runnable() {

                @Override
                public void run() {
                
                
            String err = msg;

            // Try to get a translated string (see errors.h)
            switch (msg) {
                case "Unsupported PCAP/Pcapng file":
                    err = getString(R.string.unsupported_pcap_file);
                    break;
                case "Invalid PCAP/Pcapng file":
                    err = getString(R.string.invalid_pcap_file);
                    break;
                case "Could not open the capture interface":
                    err = getString(R.string.capture_interface_open_error);
                    break;
                case "Unsupported datalink":
                    err = getString(R.string.unsupported_pcap_datalink);
                    break;
                case "The specified PCAP/Pcapng file does not exist":
                    err = getString(R.string.pcap_file_not_exists);
                    break;
                case "pcapd daemon start failure":
                    if(mSettings.root_capture)
                        err = getString(R.string.root_capture_pcapd_start_failure);
                    break;
                case "pcapd daemon did not spawn":
                    if(mSettings.root_capture)
                        err = getString(R.string.root_capture_start_failed);
                    break;
                case "PCAP/Pcapng read error":
                    err = getString(R.string.pcap_read_error);
                    break;
            }

                    Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
            }
        });
    }

    public String getWorkingDir() {
        return getCacheDir().getAbsolutePath();
    }
    public String getPersistentDir() { return getFilesDir().getAbsolutePath(); }

    public String getLibprogPath(String prog_name) {
        return getLibprogPath(this, prog_name);
    }

    public static String getLibprogPath(Context ctx, String prog_name) {
        // executable binaries are stored into the /lib folder of the app
        String dir = ctx.getApplicationInfo().nativeLibraryDir;
        return(dir + "/lib" + prog_name + ".so");
    }

    public void notifyBlacklistsLoaded(final Blacklists.NativeBlacklistStatus[] loaded_blacklists) {
        // this is invoked from the packet capture thread. Use the handler to save time.
        mHandler.post(new Runnable(){
                @Override
                public void run() {
                mBlacklists.onNativeLoaded(loaded_blacklists);
    }
    });
    }

    public BlacklistDescriptor[] getBlacklistsInfo() {
        BlacklistDescriptor[] blsinfo = new BlacklistDescriptor[mBlacklists.getNumBlacklists()];
        int i = 0;

        Iterator<BlacklistDescriptor> it = mBlacklists.iter();
        while(it.hasNext())
            blsinfo[i++] = it.next();

        return blsinfo;
    }

    public void reloadBlocklist() {
        AppLogger.i(TAG, "reloading firewall blocklist");
        reloadBlocklist(mBlocklist.toListDescriptor());
    }

    public void reloadFirewallWhitelist() {
        AppLogger.i(TAG, "reloading firewall whitelist");
        reloadFirewallWhitelist(Prefs.isFirewallWhitelistMode(mPrefs) ? mFirewallWhitelist.toListDescriptor() : null);
    }

    public static void reloadMalwareWhitelist() {
        if((INSTANCE == null))
            return;

        AppLogger.i(TAG, "reloading malware whitelist");
        reloadMalwareWhitelist(INSTANCE.mMalwareWhitelist.toListDescriptor());
    }

    public static void reloadDecryptionList() {
        if((INSTANCE == null) || (INSTANCE.mDecryptionList == null))
            return;

        AppLogger.i(TAG, "reloading TLS decryption whitelist");
        reloadDecryptionList(INSTANCE.mDecryptionList.toListDescriptor());
    }

    public static void setFirewallEnabled(boolean enabled) {
        if(INSTANCE == null)
            return;

        INSTANCE.mFirewallEnabled = enabled;
        nativeSetFirewallEnabled(enabled);
    }
/*
    public static @NonNull CaptureStats getStats() {
        CaptureStats stats = lastStats.getValue();
        return((stats != null) ? stats : new CaptureStats());
    }*/
    public static CaptureStats getStats() {
        CaptureStats stats = (CaptureStats)lastStats.getValue();
        return((stats != null) ? stats : new CaptureStats());
    }
/*
    public static void observeStats(LifecycleOwner lifecycleOwner, Observer<CaptureStats> observer) {
        lastStats.observe(lifecycleOwner, observer);
    }*/
    public static void observeStats(obseobj observer) {
        lastStats.addObs( observer);
    }
/*
    public static void observeStatus(LifecycleOwner lifecycleOwner, Observer<ServiceStatus> observer) {
        serviceStatus.observe(lifecycleOwner, observer);
    }
*/
    public synchronized static void observeStatus( obseobj observer) {
        serviceStatus.addObs(observer);
    }
    public static void waitForCaptureStop() {
        if(INSTANCE == null)
            return;

        AppLogger.d(TAG, "waitForCaptureStop " + Thread.currentThread().getName());
        INSTANCE.mLock.lock();
        try {
            while(INSTANCE.mCaptureThread != null) {
                try {
                    INSTANCE.mCaptureStopped.await();
                } catch (InterruptedException ignored) {LogUtil.logToFile(ignored.toString());}
            }
        } finally {
            INSTANCE.mLock.unlock();
        }
        AppLogger.d(TAG, "waitForCaptureStop done " + Thread.currentThread().getName());
    }

    public static boolean hasError() {
        return HAS_ERROR;
    }

    public static @Nullable Utils.PrivateDnsMode getPrivateDnsMode() {
        return isServiceActive() ? INSTANCE.mPrivateDnsMode : null;
    }

    public static native int initLogger(String path, int level);
    public static native int writeLog(int logger, int lvl, String message);
    private static native void initPlatformInfo(String appver, String device, String os);
    private static native void runPacketLoop(int fd, CaptureService vpn, int sdk);
    private static native void stopPacketLoop();
    private static native int getFdSetSize();
    private static native void setPrivateDnsBlocked(boolean to_block);
    private static native void setDnsServer(String server);
    private static native void addPortMapping(int ipproto, int orig_port, int redirect_port, String redirect_ip);
    private static native void reloadBlacklists();
    private static native boolean reloadBlocklist(MatchList.ListDescriptor blocklist);
    private static native boolean reloadFirewallWhitelist(MatchList.ListDescriptor whitelist);
    private static native boolean reloadMalwareWhitelist(MatchList.ListDescriptor whitelist);
    private static native boolean reloadDecryptionList(MatchList.ListDescriptor whitelist);
    public static native void askStatsDump();
    public static native byte[] getPcapHeader();
    public static native void nativeSetFirewallEnabled(boolean enabled);
    public static native int getNumCheckedMalwareConnections();
    public static native int getNumCheckedFirewallConnections();
    public static native int rootCmd(String prog, String args);
    public static native void setPayloadMode(int mode);
    public static native List<String> getL7Protocols();
    public static native void dumpMasterSecret(byte[] secret);
    public static native boolean hasSeenDumpExtensions();
    public static native boolean extractKeylogFromPcapng(String pcapng_path, String out_path);
    public static native void setdebug(boolean enabled);
    public static native void setisNetfree(boolean enabled);
    
}

