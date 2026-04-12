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


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import com.emanuelef.remote_capture.activities.MainActivity;
import com.emanuelef.remote_capture.model.CaptureSettings;

/**
 * Service which waits for other apps VPNService to terminate before
 * restarting the capture.
 */
//@RequiresApi(api = Build.VERSION_CODES.M)
public class VpnReconnectService extends Service {
    private static final String TAG = "VpnReconnectService";
    private static final String NOTIFY_CHAN_VPNRECONNECT = "VPN Reconnection";
    public static final int NOTIFY_ID_VPNRECONNECT = 10;
    private static final String STOP_ACTION = "stop";

    private static VpnReconnectService INSTANCE;
    private Handler mHandler;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private Network mActiveVpnNetwork;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        AppLogger.d(TAG, "onCreate");
        mHandler = new Handler(Looper.getMainLooper());

        INSTANCE = this;
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        AppLogger.d(TAG, "onDestroy");

        unregisterNetworkCallback();
        INSTANCE = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        AppLogger.d(TAG, "onStartCommand");

        if ((intent != null) && (intent.getAction() != null) && (intent.getAction().equals(STOP_ACTION))) {
            Utils.showToastLong(this, R.string.vpn_reconnection_aborted);
            stopService();
            return START_NOT_STICKY;
        }

        if (Build.VERSION.SDK_INT >= 34)
            startForeground(NOTIFY_ID_VPNRECONNECT, buildNotification(), 1073741824);
        else
            startForeground(NOTIFY_ID_VPNRECONNECT, buildNotification());
        
        mHandler.postDelayed(new Runnable(){

                @Override
                public void run() {
                
            AppLogger.i(TAG, "Could not detect a VPN within the timeout, automatic reconnection aborted");
            stopService();
        }}, 10000);

        if (!registerNetworkCallbacks()) {
            stopService();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    private Notification buildNotification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel chan = new NotificationChannel(NOTIFY_CHAN_VPNRECONNECT,
                    NOTIFY_CHAN_VPNRECONNECT, NotificationManager.IMPORTANCE_LOW); // low: no sound
            chan.setShowBadge(false);
            nm.createNotificationChannel(chan);
        }

        // Status notification builder
        PendingIntent startMainApp = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), Utils.getIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));

        Intent abortReconnectIntent = new Intent(this, VpnReconnectService.class);
        abortReconnectIntent.setAction(STOP_ACTION);
        PendingIntent abortReconnect = PendingIntent.getService(this, 0, abortReconnectIntent, Utils.getIntentFlags(0));

        Notification.Builder builder = new Notification.Builder(this, NOTIFY_CHAN_VPNRECONNECT)
                .setSmallIcon(R.drawable.ic_logo)
                .setColor(getColor(R.color.colorPrimary))
                .setContentIntent(startMainApp)
                .setDeleteIntent(abortReconnect)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.vpn_reconnection))
                .setContentText(getString(R.string.waiting_for_vpn_disconnect))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_STATUS)
                .setPriority(Notification.PRIORITY_LOW); // see IMPORTANCE_LOW

        AppLogger.d(TAG, "running");
        return builder.build();
    }

    private void checkAvailableNetwork(ConnectivityManager cm, Network network) {
        if (network.equals(mActiveVpnNetwork))
            return;

        NetworkCapabilities cap = cm.getNetworkCapabilities(network);
        if ((cap != null) && cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            mActiveVpnNetwork = network;
            AppLogger.d(TAG, "Detected active VPN network: " + mActiveVpnNetwork);

            // cancel the deadline timer / onLost timer
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private boolean registerNetworkCallbacks() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);

        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                AppLogger.d(TAG, "onAvailable: " + network);

                checkAvailableNetwork(cm, network);
            }

            @Override
            public void onLost(@NonNull Network network) {
                AppLogger.d(TAG, "onLost: " + network);

                // NOTE: when onLost is called, the TRANSPORT_VPN capability may already have been removed
                if (network.equals(mActiveVpnNetwork)) {
                    // NOTE: onAvailable and onLost may be called multiple times before the actual VPN is started.
                    // Use a debounce delay to prevent mis-detection
                    mHandler.postDelayed(new Runnable(){

                            @Override
                            public void run() {
                            
                            
                        AppLogger.i(TAG, "Active VPN disconnected, starting the capture");
                        unregisterNetworkCallback();
                        
                        Context ctx = VpnReconnectService.this;
                        CaptureSettings settings = new CaptureSettings(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

                        CaptureHelper helper = new CaptureHelper(ctx);
                       // helper.setListener(success -> stopService());
                        helper.startCapture(settings);
                        }
                    }, 3000);
                }
            }
        };

        try {
            AppLogger.d(TAG, "registerNetworkCallback");

            NetworkRequest.Builder builder = new NetworkRequest.Builder()
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);

            // necessary to see other apps network events on Android 12+
          //  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
              //  builder.setIncludeOtherUidNetworks(true);

            cm.registerNetworkCallback(builder.build(), mNetworkCallback);
        } catch (SecurityException e) {
            // this is a bug in Android 11 - https://issuetracker.google.com/issues/175055271?pli=1
            e.printStackTrace();

            AppLogger.e(TAG, "registerNetworkCallback failed");
            mNetworkCallback = null;
            return false;
        }

        // The VPN may already be active
        Network net = Utils.getRunningVpn(this);
        if (net != null)
            checkAvailableNetwork(cm, net);

        return true;
    }

    private void unregisterNetworkCallback() {
        if(mNetworkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);

            try {
                AppLogger.d(TAG, "unregisterNetworkCallback");
                cm.unregisterNetworkCallback(mNetworkCallback);
            } catch(IllegalArgumentException e) {
                AppLogger.w(TAG, "unregisterNetworkCallback failed: " + e);
            }

            mNetworkCallback = null;
        }
    }

    @SuppressLint("ObsoleteSdkInt")
  //  @RequiresApi(api = Build.VERSION_CODES.BASE)
    public static boolean isAvailable() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;
    }

    @SuppressWarnings("deprecation")
    public static void stopService() {
        AppLogger.d(TAG, "stopService called");
        VpnReconnectService service = INSTANCE;
        if (service == null)
            return;

        service.unregisterNetworkCallback();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            service.stopForeground(STOP_FOREGROUND_REMOVE);
        else
            service.stopForeground(true);

        service.stopSelf();
    }
}

