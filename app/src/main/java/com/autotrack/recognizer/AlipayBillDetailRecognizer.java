package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AlipayBillDetailRecognizer implements PageRecognizer {
    private static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && ALIPAY_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean billDetail = false;
        String merchant = "";
        String timeText = "";
        String paymentMethod = "";
        double amount = -1;
        RecordType type = RecordType.EXPENSE;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;

            if (isBillDetailMarker(content)) {
                billDetail = true;
            }

            AmountParseResult amountResult = parseAmount(content);
            if (amountResult != null && amount < 0) {
                amount = amountResult.amount;
                type = amountResult.type;
            }

            if ("付款方式".equals(content)) {
                paymentMethod = firstReadableAfter(nodes, i);
            }

            if (isMerchantLabel(content)) {
                String next = firstReadableAfter(nodes, i);
                if (!next.isEmpty() && (merchant.isEmpty() || "商品说明".equals(content) || "交易说明".equals(content))) {
                    merchant = next;
                }
            }

            if ("支付时间".equals(content) || "创建时间".equals(content) || "收款时间".equals(content)) {
                timeText = firstReadableAfter(nodes, i);
            }

            if (content.endsWith("免密支付") && merchant.isEmpty()) {
                merchant = content;
            }
        }

        if (!billDetail || amount < 0) return RecognitionResult.empty();
        if (merchant.isEmpty()) merchant = "支付宝账单";

        long timestamp = parseAlipayTime(timeText, System.currentTimeMillis());
        String displayTime = formatDisplayTime(timestamp);
        String note = displayTime + " " + merchant;
        String categoryHint = inferCategory(type, merchant);
        String assetHint = paymentMethod.isEmpty() ? "支付宝" : paymentMethod;
        String signature = "alipay_bill-" + amount + "-" + type + "-" + merchant + "-" + timeText;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "支付宝" : snapshot.getAppName())
                .type(type)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint(categoryHint)
                .merchant(merchant)
                .paymentMethod(assetHint)
                .note(note)
                .transactionTimeMillis(timestamp)
                .signature(signature)
                .confidence(0.9)
                .build();
        return RecognitionResult.matched(candidate, "支付宝账单详情页");
    }

    private boolean isBillDetailMarker(String content) {
        return "账单详情".equals(content)
                || "商家订单号".equals(content)
                || "订单号".equals(content)
                || "交易详情".equals(content)
                || "交易订单号".equals(content);
    }

    private boolean isMerchantLabel(String content) {
        return "商品说明".equals(content)
                || "收款方全称".equals(content)
                || "管理自动扣款".equals(content)
                || "交易说明".equals(content);
    }

    private String firstReadableAfter(List<NodeSnapshot> nodes, int index) {
        for (int i = index + 1; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (!content.isEmpty()) return content;
        }
        return "";
    }

    private AmountParseResult parseAmount(String content) {
        try {
            if (content.startsWith("支出") && content.endsWith("元")) {
                return new AmountParseResult(parseNumber(content.replace("支出", "").replace("元", "")), RecordType.EXPENSE);
            }
            if (content.startsWith("收入") && content.endsWith("元")) {
                return new AmountParseResult(parseNumber(content.replace("收入", "").replace("元", "")), RecordType.INCOME);
            }
            if (content.matches("^[+-]?\\d+(\\.\\d+)?元$")) {
                double value = parseNumber(content.replace("元", "").replace("+", ""));
                RecordType type = content.startsWith("+") ? RecordType.INCOME : RecordType.EXPENSE;
                return new AmountParseResult(Math.abs(value), type);
            }
            if (content.matches("^[-+]?\\d+(\\.\\d{1,2})?$")) {
                double value = parseNumber(content.replace("+", "").replace("-", ""));
                RecordType type = content.startsWith("+") ? RecordType.INCOME : RecordType.EXPENSE;
                return new AmountParseResult(value, type);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private double parseNumber(String value) {
        return Double.parseDouble(value.replace(",", "").trim());
    }

    private long parseAlipayTime(String timeText, long fallback) {
        if (timeText == null || timeText.isEmpty()) return fallback;
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(timeText);
            return date == null ? fallback : date.getTime();
        } catch (ParseException ignored) {
            return fallback;
        }
    }

    private String formatDisplayTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private String inferCategory(RecordType type, String merchant) {
        if (type == RecordType.INCOME) {
            if (merchant.contains("闲鱼") || merchant.contains("机")) return "二手交易";
            return "其他收入";
        }
        if (merchant.contains("美团") || merchant.contains("饿了么") || merchant.contains("餐饮") || merchant.contains("饭")) {
            return "餐饮";
        }
        return "购物";
    }

    private static final class AmountParseResult {
        private final double amount;
        private final RecordType type;

        private AmountParseResult(double amount, RecordType type) {
            this.amount = amount;
            this.type = type;
        }
    }
}
