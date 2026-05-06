package com.autotrack.android;

import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class AccessibilitySnapshotMapper {
    private AccessibilitySnapshotMapper() {
    }

    public static PageSnapshot fromRoot(AccessibilityNodeInfo root, String fallbackAppName) {
        if (root == null) return new PageSnapshot("", fallbackAppName, null);
        String packageName = root.getPackageName() == null ? "" : root.getPackageName().toString();
        return new PageSnapshot(packageName, fallbackAppName, fromNode(root));
    }

    public static NodeSnapshot fromNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        List<NodeSnapshot> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            if (childNode == null) continue;
            try {
                NodeSnapshot child = fromNode(childNode);
                if (child != null) children.add(child);
            } finally {
                childNode.recycle();
            }
        }
        return new NodeSnapshot(
                collectText(node),
                collectDescription(node),
                node.getClassName() == null ? "" : node.getClassName().toString(),
                node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName(),
                children
        );
    }

    private static String collectText(AccessibilityNodeInfo node) {
        StringBuilder builder = new StringBuilder();
        append(builder, node.getText());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            append(builder, node.getHintText());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            append(builder, node.getStateDescription());
        }
        appendExtras(builder, node.getExtras());
        return builder.toString();
    }

    private static String collectDescription(AccessibilityNodeInfo node) {
        StringBuilder builder = new StringBuilder();
        append(builder, node.getContentDescription());
        append(builder, node.getError());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            append(builder, node.getPaneTitle());
            append(builder, node.getTooltipText());
        }
        return builder.toString();
    }

    private static void appendExtras(StringBuilder builder, Bundle extras) {
        if (extras == null || extras.isEmpty()) return;
        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            if (value instanceof CharSequence) {
                append(builder, (CharSequence) value);
            } else if (value instanceof CharSequence[]) {
                for (CharSequence item : (CharSequence[]) value) {
                    append(builder, item);
                }
            }
        }
    }

    private static void append(StringBuilder builder, CharSequence value) {
        if (value == null) return;
        String text = value.toString().replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return;
        if (builder.indexOf(text) >= 0) return;
        if (builder.length() > 0) builder.append(' ');
        builder.append(text);
    }
}
