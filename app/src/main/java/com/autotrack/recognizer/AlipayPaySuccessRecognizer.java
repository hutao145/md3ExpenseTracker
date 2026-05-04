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

public final class AlipayPaySuccessRecognizer implements PageRecognizer {
    private static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("[¥￥]?\\s*([0-9,]+(\\.\\d{1,2})?)\\s*元?");

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && ALIPAY_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean paySuccess = false;
        String payee = "";
        String paymentMethod = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;

            if (isPaySuccessMarker(content)) {
                paySuccess = true;
            }

            if (paySuccess && amount < 0) {
                Double parsedAmount = parseAmount(content);
                if (parsedAmount != null) {
                    amount = parsedAmount;
                    payee = firstPayeeAfterAmount(nodes, i);
                }
            }

            if ("付款方式".equals(content)) {
                paymentMethod = firstReadableAfter(nodes, i);
            }

            if ("收款方".equals(content) && payee.isEmpty()) {
                payee = firstReadableAfter(nodes, i);
            }
        }

        if (!paySuccess || amount <= 0) return RecognitionResult.empty();
        if (payee.isEmpty()) payee = "支付宝支付";
        if (paymentMethod.isEmpty()) paymentMethod = "支付宝";

        long now = System.currentTimeMillis();
        String note = formatDisplayTime(now) + " " + payee;
        String category = inferCategory(payee);
        String signature = "alipay_pay_success-" + amount + "-" + payee;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "支付宝" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint(category)
                .merchant(payee)
                .paymentMethod(paymentMethod)
                .note(note)
                .transactionTimeMillis(now)
                .signature(signature)
                .confidence(0.86)
                .build();
        return RecognitionResult.matched(candidate, "支付宝支付成功页");
    }

    private String firstPayeeAfterAmount(List<NodeSnapshot> nodes, int index) {
        for (int i = index + 1; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;
            if (isPaySuccessMarker(content)) continue;
            if (content.contains("￥") || content.contains("¥") || content.startsWith("-")) continue;
            if (content.matches(".*\\d+(\\.\\d{1,2})?\\s*元?.*")) continue;
            if ("完成".equals(content) || "查看订单".equals(content) || "付款方式".equals(content) || "支付方式".equals(content)) continue;
            return content;
        }
        return "";
    }

    private boolean isPaySuccessMarker(String content) {
        return "支付成功".equals(content)
                || "付款成功".equals(content)
                || "交易成功".equals(content)
                || content.startsWith("支付成功")
                || content.contains("付款成功");
    }

    private Double parseAmount(String content) {
        if (!content.contains("¥") && !content.contains("￥") && !content.contains("元")
                && !content.matches("^\\d+(\\.\\d{1,2})?$")) {
            return null;
        }
        Matcher matcher = AMOUNT_PATTERN.matcher(content.replace(",", ""));
        double best = -1;
        while (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                if (value > best && value < 1000000) best = value;
            } catch (NumberFormatException ignored) {
            }
        }
        return best > 0 ? best : null;
    }

    private String firstReadableAfter(List<NodeSnapshot> nodes, int index) {
        for (int i = index + 1; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (!content.isEmpty()) return content;
        }
        return "";
    }

    private String inferCategory(String payee) {
        if (payee.contains("麻辣烫") || payee.contains("餐饮") || payee.contains("吃") || payee.contains("食") || payee.contains("外卖")) {
            return "餐饮";
        }
        return "购物";
    }

    private String formatDisplayTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
}
