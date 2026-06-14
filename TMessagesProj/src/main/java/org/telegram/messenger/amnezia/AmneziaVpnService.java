package org.telegram.messenger.amnezia;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.amnezia.awg.backend.GoBackend;
import org.amnezia.awg.backend.Tunnel;
import org.amnezia.awg.backend.TunnelActionHandler;
import org.amnezia.awg.config.Config;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class AmneziaVpnService extends VpnService implements TunnelActionHandler {

    public static final String ACTION_START = "org.telegram.messenger.amnezia.START";
    public static final String ACTION_STOP = "org.telegram.messenger.amnezia.STOP";
    public static final String EXTRA_CONFIG = "config";
    public static final String EXTRA_NAME = "name";

    private GoBackend backend;
    private static AwgTunnel tunnel;
    private static Tunnel.State tunnelState = Tunnel.State.DOWN;
    private static boolean isConnecting = false;

    private static class AwgTunnel implements Tunnel {
        private final String name;
        AwgTunnel(String name) { this.name = name; }
        
        @Override 
        public String getName() { return name; }
        
        @Override 
        public void onStateChange(State newState) {
            tunnelState = newState;
            isConnecting = false;
            FileLog.d("AmneziaVPN state changed: " + newState);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            updateNotificationInternal();
        }

        @Override public Boolean isMetered() { return false; }
        @Override public Boolean isIpv4ResolutionPreferred() { return false; }
    }

    private static AmneziaVpnService instance;
    private static AmneziaVpnService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        backend = new GoBackend(this, this);
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public void runPreUp(Collection<String> scripts) {}
    @Override
    public void runPostUp(Collection<String> scripts) {}
    @Override
    public void runPreDown(Collection<String> scripts) {}
    @Override
    public void runPostDown(Collection<String> scripts) {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                isConnecting = true;
                createNotificationChannel();
                Notification notification = createNotification("Подключение...");
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else {
                    startForeground(1001, notification);
                }

                String configText = intent.getStringExtra(EXTRA_CONFIG);
                String name = intent.getStringExtra(EXTRA_NAME);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                
                new Thread(() -> startTunnelInternal(name, configText)).start();
            } else if (ACTION_STOP.equals(action)) {
                stopTunnel();
            }
        }
        return START_STICKY;
    }

    private void startTunnelInternal(String name, String configText) {
        try {
            FileLog.d("AmneziaVPN: startTunnelInternal");
            if (tunnel != null) {
                backend.setState(tunnel, Tunnel.State.DOWN, null);
            }
            Config config = Config.parse(new ByteArrayInputStream(configText.getBytes(StandardCharsets.UTF_8)));
            tunnel = new AwgTunnel(name != null ? name : "AmneziaWG");
            backend.setState(tunnel, Tunnel.State.UP, config);
        } catch (Exception e) {
            FileLog.e("AmneziaVPN Error", e);
            isConnecting = false;
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            stopTunnel();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("amnezia_vpn", "AmneziaWG VPN", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, "amnezia_vpn")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("AmneziaWG")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private static void updateNotificationInternal() {
        AmneziaVpnService service = getInstance();
        if (service != null) {
            NotificationManager manager = service.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(1001, service.createNotification(getStatus()));
            }
        }
    }

    private void stopTunnel() {
        if (tunnel != null) {
            try {
                backend.setState(tunnel, Tunnel.State.DOWN, null);
            } catch (Exception ignore) {}
            tunnel = null;
        }
        tunnelState = Tunnel.State.DOWN;
        isConnecting = false;
        stopForeground(true);
        stopSelf();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    public static boolean isRunning() {
        return tunnel != null && tunnelState == Tunnel.State.UP;
    }

    public static String getStatus() {
        if (isConnecting) return "Connecting...";
        if (tunnel == null || tunnelState == Tunnel.State.DOWN) return "Disconnected";
        if (tunnelState == Tunnel.State.UP) return "Connected";
        return "Disconnected";
    }
}
