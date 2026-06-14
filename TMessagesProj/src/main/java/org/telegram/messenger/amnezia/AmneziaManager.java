package org.telegram.messenger.amnezia;

import android.content.Context;
import android.content.Intent;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.SharedConfig;

import java.util.List;

public class AmneziaManager {

    public static void startVpn(Context context, SharedConfig.ProxyInfo proxyInfo) {
        if (proxyInfo == null || !proxyInfo.isAmneziaWG || proxyInfo.awgConfigId == null) {
            return;
        }

        AwgCacheManager cacheManager = new AwgCacheManager(context);
        List<AwgConfig> configs = cacheManager.getAllConfigs();
        AwgConfig activeConfig = null;
        for (AwgConfig config : configs) {
            if (config.getId().equals(proxyInfo.awgConfigId)) {
                activeConfig = config;
                break;
            }
        }

        if (activeConfig == null) {
            return;
        }

        try {
            Intent intent = new Intent(context, AmneziaVpnService.class);
            intent.setAction(AmneziaVpnService.ACTION_START);
            intent.putExtra(AmneziaVpnService.EXTRA_CONFIG, activeConfig.getConfigText());
            intent.putExtra(AmneziaVpnService.EXTRA_NAME, activeConfig.getName());
            context.startService(intent);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void stopVpn(Context context) {
        try {
            Intent intent = new Intent(context, AmneziaVpnService.class);
            intent.setAction(AmneziaVpnService.ACTION_STOP);
            context.startService(intent);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isVpnRunning() {
        return AmneziaVpnService.isRunning();
    }
}
