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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeChatTransferRecognizer implements PageRecognizer {
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final Pattern CURRENCY_AMOUNT_PATTERN = Pattern.compile("[¥￥]\\s*([0-9,]+(\\.\\d{1,2})?)");

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && WECHAT_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        RecognitionResult pending = recognizePendingTransfer(snapshot);
        if (pending.isMatched()) return pending;
        RecognitionResult detail = recognizeTransferDetail(snapshot);
        if (detail.isMatched()) return detail;
        return recognizeReceivedTransfer(snapshot);
    }

    private RecognitionResult recognizePendingTransfer(PageSnapshot snapshot) {
        boolean pending = false;
        String info = "";
        double amount = -1;

        for (NodeSnapshot node : snapshot.flatten()) {
            String content = node.getReadableText();
            if (content.contains("待") && content.contains("确认收款")) {
                pending = true;
                info = content;
            }
            if ((content.contains("￥") || content.contains("¥")) && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(content.replace("￥", "").replace("¥", ""));
                if (parsed != null && parsed > 0) amount = parsed;
            }
        }

        if (!pending || amount <= 0 || info.isEmpty()) return RecognitionResult.empty();
        long now = System.currentTimeMillis();
        return RecognitionResult.matched(base(snapshot, amount, RecordType.EXPENSE, "转账", info, "微信", now,
                "wechat_transfer_pending-" + amount + "-" + info), "微信待确认收款页");
    }

    private RecognitionResult recognizeTransferDetail(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean target = false;
        String targetAccount = "";
        String timeText = "";
        String paymentMethod = "";
        double amount = -1;
        RecordType type = RecordType.EXPENSE;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if ("转账单号".equals(content)) target = true;
            if (content.matches("^[-+]?\\d+\\.\\d{2}$") && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(content);
                if (parsed != null && parsed != 0) {
                    type = parsed > 0 ? RecordType.INCOME : RecordType.EXPENSE;
                    amount = Math.abs(parsed);
                    if (i > 0) targetAccount = nodes.get(i - 1).getReadableText();
                }
            }
            if ("支付方式".equals(content)) paymentMethod = RecognizerUtils.firstReadableAfter(nodes, i);
            if ("转账时间".equals(content) || "支付时间".equals(content)) timeText = RecognizerUtils.firstReadableAfter(nodes, i);
        }

        if (!target || amount < 0) return RecognitionResult.empty();
        if (targetAccount.isEmpty() || targetAccount.matches("^[-+]?\\d+\\.\\d{2}$")) targetAccount = "微信扫码付款";
        if (paymentMethod.isEmpty()) paymentMethod = "微信";
        long timestamp = parseWeChatTime(timeText, System.currentTimeMillis());
        return RecognitionResult.matched(base(snapshot, amount, type, targetAccount.contains("付款") ? "购物" : "转账",
                targetAccount, paymentMethod, timestamp, "wechat_transfer_detail-" + amount + "-" + targetAccount + "-" + timeText),
                "微信扫码/转账账单详情页");
    }

    private RecognitionResult recognizeReceivedTransfer(PageSnapshot snapshot) {
        String allText = snapshot.getAllText();
        if (!allText.contains("已收款") && !allText.contains("已存入零钱")) return RecognitionResult.empty();
        double amount = -1;
        String note = "微信转账已收款";
        for (NodeSnapshot node : snapshot.flatten()) {
            String content = node.getReadableText();
            if ((content.contains("￥") || content.contains("¥")) && amount < 0) {
                Double parsed = parseCurrencyAmount(content);
                if (parsed != null && parsed > 0) amount = parsed;
            } else if ((content.contains("转账") || content.contains("收款")) && content.length() < 40) {
                note = content;
            }
        }
        if (amount <= 0) return RecognitionResult.empty();
        long now = System.currentTimeMillis();
        return RecognitionResult.matched(base(snapshot, amount, RecordType.INCOME, "转账", note, "微信零钱", now,
                "wechat_transfer_received-" + amount + "-" + note), "微信转账已收款页");
    }

    private TransactionCandidate base(PageSnapshot snapshot, double amount, RecordType type, String category, String merchant,
                                      String paymentMethod, long timestamp, String signature) {
        return TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "微信" : snapshot.getAppName())
                .type(type)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint(category)
                .merchant(merchant)
                .paymentMethod(paymentMethod)
                .note(RecognizerUtils.formatDisplayTime(timestamp) + " " + merchant)
                .transactionTimeMillis(timestamp)
                .signature(signature)
                .confidence(0.84)
                .build();
    }

    private long parseWeChatTime(String timeText, long fallback) {
        if (timeText == null || timeText.isEmpty()) return fallback;
        try {
            Date date = new SimpleDateFormat("yyyy年M月d日 HH:mm:ss", Locale.getDefault()).parse(timeText);
            return date == null ? fallback : date.getTime();
        } catch (ParseException ignored) {
            return fallback;
        }
    }

    private Double parseCurrencyAmount(String content) {
        Matcher matcher = CURRENCY_AMOUNT_PATTERN.matcher(content.replace(",", ""));
        while (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                if (value > 0 && value < 1000000) return value;
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
