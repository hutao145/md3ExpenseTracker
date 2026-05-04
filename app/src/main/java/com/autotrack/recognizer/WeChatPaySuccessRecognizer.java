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

public final class WeChatPaySuccessRecognizer implements PageRecognizer {
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("[¥￥]?\\s*([0-9,]+(\\.\\d{1,2})?)\\s*元?");

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && WECHAT_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean paySuccess = false;
        String merchant = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;

            if (isPaySuccessMarker(content)) {
                paySuccess = true;
                if (merchant.isEmpty()) {
                    merchant = firstMerchantAfter(nodes, i);
                }
            }

            Double parsedAmount = parseCurrencyAmount(content);
            if (parsedAmount != null && parsedAmount > 0) {
                amount = parsedAmount;
            }
        }

        if (!paySuccess || amount <= 0) return RecognitionResult.empty();
        if (merchant.isEmpty()) merchant = "微信支付";

        long now = System.currentTimeMillis();
        String displayTime = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(now));
        String note = displayTime + " " + merchant;
        String signature = "wechat_pay_success-" + amount + "-" + merchant;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "微信" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("购物")
                .merchant(merchant)
                .paymentMethod("微信")
                .note(note)
                .transactionTimeMillis(now)
                .signature(signature)
                .confidence(0.85)
                .build();
        return RecognitionResult.matched(candidate, "微信支付成功页");
    }

    private boolean isPaySuccessMarker(String content) {
        return "支付成功".equals(content)
                || "付款成功".equals(content)
                || "支付已完成".equals(content)
                || content.contains("支付成功")
                || content.contains("付款成功");
    }

    private String firstMerchantAfter(List<NodeSnapshot> nodes, int index) {
        for (int i = index + 1; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;
            if (isPaySuccessMarker(content)) continue;
            if (content.contains("¥") || content.contains("￥")) continue;
            if (content.matches(".*\\d+(\\.\\d{1,2})?\\s*元.*")) continue;
            if ("完成".equals(content) || "查看账单".equals(content) || "查看详情".equals(content)) continue;
            return content;
        }
        return "";
    }

    private Double parseCurrencyAmount(String content) {
        if (!content.contains("¥") && !content.contains("￥") && !content.contains("元")) return null;
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
}
