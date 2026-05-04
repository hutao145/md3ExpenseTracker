package com.autotrack.android;

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
            NodeSnapshot child = fromNode(node.getChild(i));
            if (child != null) children.add(child);
        }
        return new NodeSnapshot(
                node.getText() == null ? "" : node.getText().toString(),
                node.getContentDescription() == null ? "" : node.getContentDescription().toString(),
                node.getClassName() == null ? "" : node.getClassName().toString(),
                node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName(),
                children
        );
    }
}
