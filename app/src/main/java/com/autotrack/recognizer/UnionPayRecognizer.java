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

public final class UnionPayRecognizer implements PageRecognizer {
    private static final String UNION_PAY_PACKAGE = "com.unionpay";

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && UNION_PAY_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        RecognitionResult detail = recognizeBillDetail(snapshot);
        if (detail.isMatched()) return detail;
        return recognizePaySuccess(snapshot);
    }

    private RecognitionResult recognizeBillDetail(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean targetPage = false;
        String note = "";
        String timeText = "";
        String paymentMethod = "";
        double amount = -1;
        RecordType type = RecordType.EXPENSE;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if ("云闪付交易详情".equals(content) || "云闪付APP".equals(content)) targetPage = true;
            if (content.matches("^[-+]?¥?\\d+\\.\\d{2}$") && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(content.replace("¥", ""));
                if (parsed != null && parsed != 0) {
                    type = parsed > 0 ? RecordType.INCOME : RecordType.EXPENSE;
                    amount = Math.abs(parsed);
                    if (i > 0) note = nodes.get(i - 1).getReadableText();
                }
            }
            if ("付款方式".equals(content) || "支付方式".equals(content)) paymentMethod = RecognizerUtils.firstReadableAfter(nodes, i);
            if ("订单时间".equals(content) || "交易时间".equals(content)) timeText = RecognizerUtils.firstReadableAfter(nodes, i);
        }

        if (!targetPage || amount < 0) return RecognitionResult.empty();
        if (note.isEmpty()) note = "云闪付交易";
        if (paymentMethod.isEmpty()) paymentMethod = "云闪付";
        long timestamp = parseChineseTime(timeText, System.currentTimeMillis());
        return RecognitionResult.matched(base(snapshot, amount, type, categoryFor(note), note, paymentMethod, timestamp,
                "unionpay_bill-" + amount + "-" + note + "-" + timeText), "云闪付交易详情页");
    }

    private RecognitionResult recognizePaySuccess(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean paySuccess = false;
        String merchant = "";
        String paymentMethod = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if ("支付成功".equals(content)) paySuccess = true;
            if ((content.contains("¥") || content.contains("￥")) && !content.contains("-") && !content.contains("优惠") && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(content.replace("¥", "").replace("￥", ""));
                if (parsed != null && parsed > 0) {
                    amount = parsed;
                    if (i > 0 && !"支付成功".equals(nodes.get(i - 1).getReadableText())) merchant = nodes.get(i - 1).getReadableText();
                }
            }
            if ("付款方式".equals(content)) paymentMethod = RecognizerUtils.firstReadableAfter(nodes, i);
        }

        if (!paySuccess || amount <= 0) return RecognitionResult.empty();
        if (merchant.isEmpty()) merchant = "云闪付消费";
        if (paymentMethod.isEmpty()) paymentMethod = "云闪付";
        long now = System.currentTimeMillis();
        return RecognitionResult.matched(base(snapshot, amount, RecordType.EXPENSE, categoryFor(merchant), merchant, paymentMethod, now,
                "unionpay_success-" + amount + "-" + merchant), "云闪付支付成功页");
    }

    private TransactionCandidate base(PageSnapshot snapshot, double amount, RecordType type, String category, String merchant,
                                      String paymentMethod, long timestamp, String signature) {
        return TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "云闪付" : snapshot.getAppName())
                .type(type)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint(category)
                .merchant(merchant)
                .paymentMethod(paymentMethod)
                .note(RecognizerUtils.formatDisplayTime(timestamp) + " " + merchant)
                .transactionTimeMillis(timestamp)
                .signature(signature)
                .confidence(0.86)
                .build();
    }

    private String categoryFor(String note) {
        if (note.contains("联通") || note.contains("移动") || note.contains("电信") || note.contains("话费") || note.contains("充值")) return "通讯";
        return RecognizerUtils.inferShoppingOrDining(note);
    }

    private long parseChineseTime(String timeText, long fallback) {
        if (timeText == null || timeText.isEmpty()) return fallback;
        try {
            Date date = new SimpleDateFormat("yyyy年M月d日 HH:mm:ss", Locale.getDefault()).parse(timeText);
            return date == null ? fallback : date.getTime();
        } catch (ParseException ignored) {
            return fallback;
        }
    }
}
