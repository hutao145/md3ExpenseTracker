package com.autotrack.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PageSnapshot {
    private final String packageName;
    private final String appName;
    private final NodeSnapshot root;

    public PageSnapshot(String packageName, String appName, NodeSnapshot root) {
        this.packageName = packageName == null ? "" : packageName;
        this.appName = appName == null ? "" : appName;
        this.root = root;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public NodeSnapshot getRoot() {
        return root;
    }

    public List<NodeSnapshot> flatten() {
        List<NodeSnapshot> nodes = new ArrayList<>();
        append(root, nodes);
        return Collections.unmodifiableList(nodes);
    }

    public String getAllText() {
        StringBuilder builder = new StringBuilder();
        for (NodeSnapshot node : flatten()) {
            String text = node.getReadableText();
            if (!text.isEmpty()) {
                if (builder.length() > 0) builder.append(' ');
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private static void append(NodeSnapshot node, List<NodeSnapshot> nodes) {
        if (node == null) return;
        nodes.add(node);
        for (NodeSnapshot child : node.getChildren()) {
            append(child, nodes);
        }
    }
}
