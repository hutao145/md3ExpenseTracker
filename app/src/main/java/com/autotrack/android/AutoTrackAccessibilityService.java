package com.autotrack.android;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.autotrack.core.DraftPresenter;
import com.autotrack.core.DraftSink;
import com.autotrack.core.HostMapping;
import com.autotrack.core.MappedAutoTrackCoordinator;
import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionEngine;
import com.autotrack.recognizer.DefaultRecognizers;

import java.util.ArrayList;
import java.util.List;

public abstract class AutoTrackAccessibilityService extends AccessibilityService {
    private static final String TAG = "AutoTrackService";
    private static final int MAX_DEBUG_TEXT_LENGTH = 3500;
    private static final long DEBUG_LOG_INTERVAL_MILLIS = 1500;
    private static final int MAX_IN_APP_LOG_LENGTH = 30000;
    private static final String PREFS_NAME = "ExpenseAppPrefs";
    private static final String KEY_IN_APP_LOG_ENABLED = "autotrack_in_app_log_enabled";
    private static final String KEY_IN_APP_LOG_TEXT = "autotrack_in_app_log_text";
    private static final long[] SCAN_RETRY_DELAYS_MILLIS = {350, 900, 1600, 2600};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Runnable> pendingScanRunnables = new ArrayList<>();
    private MappedAutoTrackCoordinator coordinator;
    private boolean handling;
    private String lastDebugSnapshot = "";
    private long lastDebugSnapshotAt = 0;
    private String pendingScanPackageName = "";
    private String lastDiagnosticLog = "";
    private long lastDiagnosticLogAt = 0;
    private String lastEventLogPackage = "";
    private long lastEventLogAt = 0;

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            handleCurrentWindow(pendingScanPackageName);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        configureServiceInfo();
        List<PageRecognizer> recognizers = createRecognizers();
        coordinator = new MappedAutoTrackCoordinator(
                new RecognitionEngine(recognizers),
                createHostMapping(),
                createDraftPresenter(),
                createDraftSink()
        );
        appendDiagnosticLog("service", getPackageName(), "无障碍服务已连接，识别器数量=" + recognizers.size());
    }

    private void configureServiceInfo() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.eventTypes |= AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOWS_CHANGED
                | AccessibilityEvent.TYPE_VIEW_CLICKED
                | AccessibilityEvent.TYPE_VIEW_FOCUSED
                | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isAutoTrackEnabled()) return;
        String eventPackageName = event == null || event.getPackageName() == null
                ? ""
                : event.getPackageName().toString();
        if (isLikelyPaymentPackage(eventPackageName)) {
            appendEventLog(eventPackageName, event == null ? -1 : event.getEventType());
        }
        if (coordinator == null) {
            appendDiagnosticLog("event", eventPackageName, "识别器尚未初始化，跳过本次事件");
            return;
        }
        if (handling) return;
        ScanResult eventTextResult = handleEventText(event, eventPackageName);
        if (eventTextResult.handled) return;
        ScanResult eventSourceResult = handleEventSource(event, eventPackageName);
        if (eventSourceResult.handled) return;
        pendingScanPackageName = eventPackageName;
        scheduleWindowScans(eventPackageName);
    }

    @Override
    public void onInterrupt() {
        clearPendingWindowScans();
    }

    private void scheduleWindowScans(String eventPackageName) {
        clearPendingWindowScans();
        long baseDelay = getScanDelayMillis();
        for (long retryDelay : SCAN_RETRY_DELAYS_MILLIS) {
            long delay = Math.max(baseDelay, retryDelay);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    handleCurrentWindow(eventPackageName);
                }
            };
            pendingScanRunnables.add(runnable);
            handler.postDelayed(runnable, delay);
        }
    }

    private void clearPendingWindowScans() {
        handler.removeCallbacks(scanRunnable);
        for (Runnable runnable : pendingScanRunnables) {
            handler.removeCallbacks(runnable);
        }
        pendingScanRunnables.clear();
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
        return true;
    }

    protected boolean isInAppSnapshotLogEnabled() {
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

    private void handleCurrentWindow(String eventPackageName) {
        handling = true;
        try {
            ScanResult scanResult = ScanResult.skipped();
            int readableRootCount = 0;
            StringBuilder skippedPackages = new StringBuilder();
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                for (AccessibilityWindowInfo window : windows) {
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root == null) continue;
                    readableRootCount++;
                    try {
                        scanResult = scanResult.merge(handleRoot(root, eventPackageName));
                        if (scanResult.handled) return;
                    } finally {
                        appendSkippedPackage(skippedPackages, root);
                        root.recycle();
                    }
                }
            }

            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                logScanMiss(eventPackageName, readableRootCount, skippedPackages.toString(), "activeRoot=null");
                return;
            }
            try {
                readableRootCount++;
                scanResult = scanResult.merge(handleRoot(root, eventPackageName));
            } finally {
                appendSkippedPackage(skippedPackages, root);
                root.recycle();
            }
            if (!scanResult.handled) {
                logScanMiss(eventPackageName, readableRootCount, skippedPackages.toString(), scanResult.missReason());
            }
        } catch (Exception e) {
            Log.e(TAG, "Scan failed", e);
            appendDiagnosticLog("scan", eventPackageName, "扫描异常：" + e.getClass().getSimpleName() + " " + e.getMessage());
        } finally {
            handling = false;
        }
    }

    private ScanResult handleEventText(AccessibilityEvent event, String eventPackageName) {
        if (!isLikelyPaymentPackage(eventPackageName)) return ScanResult.skipped();
        String text = collectEventText(event);
        if (text.isEmpty()) return ScanResult.skipped();
        PageSnapshot snapshot = new PageSnapshot(
                eventPackageName,
                getReadableAppName(eventPackageName),
                new NodeSnapshot(text, "", "AccessibilityEvent", "", null)
        );
        boolean wroteSnapshot = logDebugSnapshot(snapshot);
        boolean handled = coordinator.handle(snapshot);
        if (wroteSnapshot && !handled) {
            appendDiagnosticLog("event-text", eventPackageName, "事件文本已写入，但未命中识别规则");
        }
        return new ScanResult(true, wroteSnapshot, handled);
    }

    private ScanResult handleEventSource(AccessibilityEvent event, String eventPackageName) {
        if (!isLikelyPaymentPackage(eventPackageName)) return ScanResult.skipped();
        AccessibilityNodeInfo root = getEventSourceRoot(event);
        if (root == null) return ScanResult.skipped();
        try {
            root.refresh();
            ScanResult result = handleRoot(root, eventPackageName);
            if (result.scannedPayment && !result.wroteSnapshot) {
                appendDiagnosticLog("source", eventPackageName, "事件源节点没有可读文本，继续扫描窗口根节点");
            }
            return result;
        } finally {
            root.recycle();
        }
    }

    private AccessibilityNodeInfo getEventSourceRoot(AccessibilityEvent event) {
        if (event == null) return null;
        AccessibilityNodeInfo current = event.getSource();
        if (current == null) return null;
        while (true) {
            AccessibilityNodeInfo parent = current.getParent();
            if (parent == null) return current;
            current.recycle();
            current = parent;
        }
    }

    private ScanResult handleRoot(AccessibilityNodeInfo root, String eventPackageName) {
        if (root == null) return ScanResult.skipped();
        root.refresh();
        String packageName = resolvePackageName(root, eventPackageName);
        if (!shouldScanPackage(packageName)) return ScanResult.skipped();
        PageSnapshot snapshot = new PageSnapshot(
                packageName,
                getReadableAppName(packageName),
                AccessibilitySnapshotMapper.fromNode(root)
        );
        boolean wroteSnapshot = logDebugSnapshot(snapshot);
        boolean handled = coordinator.handle(snapshot);
        if (!wroteSnapshot) {
            appendDiagnosticLog("empty-root", packageName, describeNodeStructure(root));
        }
        return new ScanResult(true, wroteSnapshot, handled);
    }

    private String resolvePackageName(AccessibilityNodeInfo root, String eventPackageName) {
        String rootPackageName = root == null || root.getPackageName() == null ? "" : root.getPackageName().toString();
        if (!rootPackageName.isEmpty()) return rootPackageName;
        return eventPackageName == null ? "" : eventPackageName;
    }

    private String collectEventText(AccessibilityEvent event) {
        if (event == null) return "";
        StringBuilder builder = new StringBuilder();
        for (CharSequence item : event.getText()) {
            appendText(builder, item);
        }
        appendText(builder, event.getContentDescription());
        appendText(builder, event.getBeforeText());

        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            appendNotificationText(builder, (Notification) parcelable);
        }
        return builder.toString();
    }

    private void appendNotificationText(StringBuilder builder, Notification notification) {
        if (notification == null || notification.extras == null) return;
        Bundle extras = notification.extras;
        appendText(builder, extras.getCharSequence(Notification.EXTRA_TITLE));
        appendText(builder, extras.getCharSequence(Notification.EXTRA_TEXT));
        appendText(builder, extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        appendText(builder, extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        appendText(builder, extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            for (CharSequence line : lines) {
                appendText(builder, line);
            }
        }
    }

    private void appendText(StringBuilder builder, CharSequence value) {
        if (value == null) return;
        String text = value.toString().replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return;
        if (builder.indexOf(text) >= 0) return;
        if (builder.length() > 0) builder.append(' ');
        builder.append(text);
    }

    protected boolean shouldScanPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        if (packageName.equals(getPackageName())) return false;
        return isLikelyPaymentPackage(packageName);
    }

    private boolean logDebugSnapshot(PageSnapshot snapshot) {
        if (!isDebugSnapshotLoggingEnabled() || snapshot == null) return false;
        String packageName = snapshot.getPackageName();
        if (!isLikelyPaymentPackage(packageName)) return false;

        String text = snapshot.getAllText();
        if (text == null || text.trim().isEmpty()) return false;

        String normalizedText = text.replaceAll("\\s+", " ").trim();
        if (normalizedText.length() > MAX_DEBUG_TEXT_LENGTH) {
            normalizedText = normalizedText.substring(0, MAX_DEBUG_TEXT_LENGTH) + "...";
        }

        String signature = packageName + "|" + normalizedText;
        long now = System.currentTimeMillis();
        if (signature.equals(lastDebugSnapshot) && now - lastDebugSnapshotAt < DEBUG_LOG_INTERVAL_MILLIS) {
            return true;
        }

        lastDebugSnapshot = signature;
        lastDebugSnapshotAt = now;
        Log.d(TAG, "snapshot package=" + packageName
                + ", app=" + snapshot.getAppName()
                + ", text=" + normalizedText);
        appendInAppLog(now, packageName, snapshot.getAppName(), normalizedText);
        return true;
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

    private void appendDiagnosticLog(String stage, String packageName, String message) {
        if (!isInAppSnapshotLogEnabled()) return;
        long now = System.currentTimeMillis();
        String signature = stage + "|" + packageName + "|" + message;
        if (signature.equals(lastDiagnosticLog) && now - lastDiagnosticLogAt < DEBUG_LOG_INTERVAL_MILLIS) {
            return;
        }
        lastDiagnosticLog = signature;
        lastDiagnosticLogAt = now;
        appendInAppLog(now, packageName == null ? "" : packageName, "AutoTrack", "[" + stage + "] " + message);
    }

    private void appendEventLog(String packageName, int eventType) {
        if (!isInAppSnapshotLogEnabled()) return;
        long now = System.currentTimeMillis();
        if (packageName.equals(lastEventLogPackage) && now - lastEventLogAt < 2000) {
            return;
        }
        lastEventLogPackage = packageName;
        lastEventLogAt = now;
        appendInAppLog(now, packageName, "AutoTrack", "[event] 收到支付类应用事件 type=" + eventType);
    }

    private void logScanMiss(String eventPackageName, int readableRootCount, String skippedPackages, String reason) {
        if (!isLikelyPaymentPackage(eventPackageName)) return;
        String message = "收到支付类应用事件，但未写入页面快照"
                + " reason=" + reason
                + ", roots=" + readableRootCount
                + ", rootPackages=" + skippedPackages;
        appendDiagnosticLog("scan", eventPackageName, message);
    }

    private void appendSkippedPackage(StringBuilder builder, AccessibilityNodeInfo root) {
        if (builder == null || root == null || root.getPackageName() == null) return;
        String packageName = root.getPackageName().toString();
        if (packageName.isEmpty() || builder.toString().contains(packageName)) return;
        if (builder.length() > 0) builder.append("|");
        builder.append(packageName);
    }

    private String describeNodeStructure(AccessibilityNodeInfo root) {
        StringBuilder builder = new StringBuilder();
        appendNodeStructure(builder, root, 0, new int[]{0});
        if (builder.length() == 0) return "微信根节点没有可读文本，且节点结构为空";
        return "微信根节点没有可读文本，节点结构=" + builder;
    }

    private void appendNodeStructure(StringBuilder builder, AccessibilityNodeInfo node, int depth, int[] count) {
        if (node == null || depth > 3 || count[0] >= 30) return;
        count[0]++;
        if (builder.length() > 0) builder.append(" / ");
        builder.append("{d=").append(depth)
                .append(", class=").append(node.getClassName())
                .append(", id=").append(node.getViewIdResourceName())
                .append(", children=").append(node.getChildCount())
                .append(", textLen=").append(node.getText() == null ? 0 : node.getText().length())
                .append(", descLen=").append(node.getContentDescription() == null ? 0 : node.getContentDescription().length())
                .append("}");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                appendNodeStructure(builder, child, depth + 1, count);
            } finally {
                child.recycle();
            }
        }
    }

    private static final class ScanResult {
        final boolean scannedPayment;
        final boolean wroteSnapshot;
        final boolean handled;

        ScanResult(boolean scannedPayment, boolean wroteSnapshot, boolean handled) {
            this.scannedPayment = scannedPayment;
            this.wroteSnapshot = wroteSnapshot;
            this.handled = handled;
        }

        static ScanResult skipped() {
            return new ScanResult(false, false, false);
        }

        ScanResult merge(ScanResult other) {
            if (other == null) return this;
            return new ScanResult(
                    scannedPayment || other.scannedPayment,
                    wroteSnapshot || other.wroteSnapshot,
                    handled || other.handled
            );
        }

        String missReason() {
            if (wroteSnapshot) return "snapshotWrittenButNoRecognizerMatch";
            if (scannedPayment) return "paymentRootHasNoReadableText";
            return "noMatchedPaymentRoot";
        }
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
