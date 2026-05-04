package com.autotrack.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NodeSnapshot {
    private final String text;
    private final String description;
    private final String className;
    private final String viewId;
    private final List<NodeSnapshot> children;

    public NodeSnapshot(String text, String description, String className, String viewId, List<NodeSnapshot> children) {
        this.text = safe(text);
        this.description = safe(description);
        this.className = safe(className);
        this.viewId = safe(viewId);
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? Collections.emptyList() : children));
    }

    public String getText() {
        return text;
    }

    public String getDescription() {
        return description;
    }

    public String getClassName() {
        return className;
    }

    public String getViewId() {
        return viewId;
    }

    public List<NodeSnapshot> getChildren() {
        return children;
    }

    public String getReadableText() {
        return text.isEmpty() ? description : text;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
