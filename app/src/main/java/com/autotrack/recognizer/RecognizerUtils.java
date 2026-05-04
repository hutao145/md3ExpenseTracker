package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class RecognizerUtils {
    private RecognizerUtils() {
    }

    static String firstReadableAfter(List<NodeSnapshot> nodes, int index) {
        for (int i = index + 1; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (!content.isEmpty()) return content;
        }
        return "";
    }

    static String formatDisplayTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    static String inferShoppingOrDining(String text) {
        if (text == null) return "购物";
        if (text.contains("烧烤") || text.contains("餐饮") || text.contains("面") || text.contains("饭")
                || text.contains("吃") || text.contains("汉堡") || text.contains("外卖") || text.contains("麻辣烫")
                || text.contains("食")) {
            return "餐饮";
        }
        return "购物";
    }

    static Double parseAmount(String value) {
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
