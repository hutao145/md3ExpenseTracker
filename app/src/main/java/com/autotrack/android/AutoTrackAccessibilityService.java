package com.autotrack.android;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.autotrack.core.DraftPresenter;
import com.autotrack.core.DraftSink;
import com.autotrack.core.HostMapping;
import com.autotrack.core.MappedAutoTrackCoordinator;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionEngine;
import com.autotrack.recognizer.DefaultRecognizers;

import java.util.List;

public abstract class AutoTrackAccessibilityService extends AccessibilityService {
    private static final String TAG = "AutoTrackService";
    private static final int MAX_DEBUG_TEXT_LENGTH = 3500;
    private static final long DEBUG_LOG_INTERVAL_MILLIS = 1500;
    private static final int MAX_IN_APP_LOG_LENGTH = 30000;
    private static final String PREFS_NAME = "ExpenseAppPrefs";
    private static final String KEY_IN_APP_LOG_ENABLED = "autotrack_in_app_log_enabled";
    private static final String KEY_IN_APP_LOG_TEXT = "autotrack_in_app_log_text";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MappedAutoTrackCoordinator coordinator;
    private boolean handling;
    private String lastDebugSnapshot = "";
    private long lastDebugSnapshotAt = 0;

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            handleCurrentWindow();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        List<PageRecognizer> recognizers = createRecognizers();
        coordinator = new MappedAutoTrackCoordinator(
                new RecognitionEngine(recognizers),
                createHostMapping(),
                createDraftPresenter(),
                createDraftSink()
        );
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isAutoTrackEnabled()) return;
        if (coordinator == null || handling) return;
        handler.removeCallbacks(scanRunnable);
        handler.postDelayed(scanRunnable, getScanDelayMillis());
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(scanRunnable);
    }

    protected List<PageRecognizer> createRecognizers() {
        return DefaultRecognizers.create();
    }

    protected HostMapping createHostMapping() {
        return HostMapping.identity();
    }

    protected long getScanDelayMillis() {
        return 300;
    }

    protected boolean isAutoTrackEnabled() {
        return true;
    }

    protected boolean isDebugSnapshotLoggingEnabled() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    protected boolean isInAppSnapshotLogEnabled() {
        if (!isDebugSnapshotLoggingEnabled()) return false;
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IN_APP_LOG_ENABLED, true);
    }

    protected String getReadableAppName(String packageName) {
        if (packageName == null) return "";
        String pkg = packageName.toLowerCase();
        if (pkg.contains("tencent.mm")) return "微信";
        if (pkg.contains("alipay")) return "支付宝";
        if (pkg.contains("pinduoduo")) return "拼多多";
        if (pkg.equals("com.jingdong.app.mall")) return "京东";
        if (pkg.contains("meituan")) return "美团";
        if (pkg.equals("com.ss.android.ugc.aweme")) return "抖音";
        if (pkg.equals("com.unionpay")) return "云闪付";
        return packageName;
    }

    protected abstract DraftPresenter createDraftPresenter();

    protected abstract DraftSink createDraftSink();

    private void handleCurrentWindow() {
        handling = true;
        try {
            boolean handled = false;
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                for (AccessibilityWindowInfo window : windows) {
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root == null) continue;
                    try {
                        handled = handleRoot(root);
                        if (handled) return;
                    } finally {
                        root.recycle();
                    }
                }
            }

            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            try {
                handleRoot(root);
            } finally {
                root.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "Scan failed", e);
        } finally {
            handling = false;
        }
    }

    private boolean handleRoot(AccessibilityNodeInfo root) {
        String packageName = root.getPackageName() == null ? "" : root.getPackageName().toString();
        if (!shouldScanPackage(packageName)) return false;
        PageSnapshot snapshot = AccessibilitySnapshotMapper.fromRoot(root, getReadableAppName(packageName));
        logDebugSnapshot(snapshot);
        return coordinator.handle(snapshot);
    }

    protected boolean shouldScanPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        if (packageName.equals(getPackageName())) return false;
        return isLikelyPaymentPackage(packageName);
    }

    private void logDebugSnapshot(PageSnapshot snapshot) {
        if (!isDebugSnapshotLoggingEnabled() || snapshot == null) return;
        String packageName = snapshot.getPackageName();
        if (!isLikelyPaymentPackage(packageName)) return;

        String text = snapshot.getAllText();
        if (text == null || text.trim().isEmpty()) return;

        String normalizedText = text.replaceAll("\\s+", " ").trim();
        if (normalizedText.length() > MAX_DEBUG_TEXT_LENGTH) {
            normalizedText = normalizedText.substring(0, MAX_DEBUG_TEXT_LENGTH) + "...";
        }

        String signature = packageName + "|" + normalizedText;
        long now = System.currentTimeMillis();
        if (signature.equals(lastDebugSnapshot) && now - lastDebugSnapshotAt < DEBUG_LOG_INTERVAL_MILLIS) {
            return;
        }

        lastDebugSnapshot = signature;
        lastDebugSnapshotAt = now;
        Log.d(TAG, "snapshot package=" + packageName
                + ", app=" + snapshot.getAppName()
                + ", text=" + normalizedText);
        appendInAppLog(now, packageName, snapshot.getAppName(), normalizedText);
    }

    private void appendInAppLog(long timestamp, String packageName, String appName, String text) {
        if (!isInAppSnapshotLogEnabled()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String line = formatLogTime(timestamp)
                + " package=" + packageName
                + ", app=" + appName
                + ", text=" + text
                + "\n\n";
        String oldLog = prefs.getString(KEY_IN_APP_LOG_TEXT, "");
        String newLog = line + (oldLog == null ? "" : oldLog);
        if (newLog.length() > MAX_IN_APP_LOG_LENGTH) {
            newLog = newLog.substring(0, MAX_IN_APP_LOG_LENGTH);
        }
        prefs.edit().putString(KEY_IN_APP_LOG_TEXT, newLog).apply();
    }

    private String formatLogTime(long timestamp) {
        return new java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())
                .format(new java.util.Date(timestamp));
    }

    private boolean isLikelyPaymentPackage(String packageName) {
        if (packageName == null) return false;
        String pkg = packageName.toLowerCase();
        return pkg.contains("tencent.mm")
                || pkg.contains("alipay")
                || pkg.contains("pinduoduo")
                || pkg.contains("unionpay")
                || pkg.contains("jingdong")
                || pkg.contains("meituan")
                || pkg.contains("aweme")
                || pkg.contains("lifeservices")
                || pkg.contains("tongyi");
    }
}
