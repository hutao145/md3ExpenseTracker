package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PinduoduoOrderDetailRecognizer implements PageRecognizer {
    private static final String PDD_PACKAGE = "com.xunmeng.pinduoduo";
    private static final Pattern PAID_AMOUNT_PATTERN = Pattern.compile("实付.*?(\\d+(?:\\.\\d{1,2})?)\\s*元");

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && PDD_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        boolean orderPage = false;
        String itemName = "";
        double amount = -1;

        for (NodeSnapshot node : snapshot.flatten()) {
            String content = node.getReadableText();
            if (content.isEmpty()) continue;

            if (content.contains("订单编号")) {
                orderPage = true;
            }

            if (content.contains("实付") && content.contains("元") && amount < 0) {
                Matcher matcher = PAID_AMOUNT_PATTERN.matcher(content);
                while (matcher.find()) {
                    try {
                        amount = Double.parseDouble(matcher.group(1));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (content.contains("商品名称：")) {
                itemName = parseItemName(content);
            }
        }

        if (!orderPage || amount <= 0) return RecognitionResult.empty();
        if (itemName.isEmpty()) itemName = "拼多多购物";

        long now = System.currentTimeMillis();
        String note = formatDisplayTime(now) + " " + itemName;
        String signature = "pdd_order_detail-" + amount + "-" + itemName;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "拼多多" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("购物")
                .merchant(itemName)
                .paymentMethod("拼多多")
                .note(note)
                .transactionTimeMillis(now)
                .signature(signature)
                .confidence(0.86)
                .build();
        return RecognitionResult.matched(candidate, "拼多多订单详情页");
    }

    private String parseItemName(String content) {
        String[] parts = content.split("[,，]单价");
        if (parts.length == 0) return "";
        String name = parts[0].replace("商品名称：", "").trim();
        if (name.length() > 18) {
            return name.substring(0, 16) + "...";
        }
        return name;
    }

    private String formatDisplayTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
}
