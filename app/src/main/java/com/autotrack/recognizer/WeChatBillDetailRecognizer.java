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

public final class WeChatBillDetailRecognizer implements PageRecognizer {
    private static final String WECHAT_PACKAGE = "com.tencent.mm";

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && WECHAT_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean billDetail = false;
        String fallbackNote = "";
        String directBelowNote = "";
        String productNote = "";
        String merchantNote = "";
        String timeText = "";
        String paymentMethod = "";
        double amount = -1;
        RecordType type = RecordType.EXPENSE;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;

            if ("交易单号".equals(content) || "商户单号".equals(content)) {
                billDetail = true;
            }

            if (content.matches("^[-+]?\\d+\\.\\d{2}$") && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(content);
                if (parsed != null && parsed != 0) {
                    type = parsed > 0 ? RecordType.INCOME : RecordType.EXPENSE;
                    amount = Math.abs(parsed);
                    fallbackNote = findFallbackNoteBefore(nodes, i);
                    directBelowNote = findDirectNoteAfterAmount(nodes, i);
                }
            }

            if ("商品".equals(content) || "商品名称".equals(content)) {
                productNote = RecognizerUtils.firstReadableAfter(nodes, i);
            }
            if ("商户全称".equals(content) || "收款方".equals(content)) {
                merchantNote = RecognizerUtils.firstReadableAfter(nodes, i);
            }
            if ("支付方式".equals(content) || "收款方式".equals(content) || "退款方式".equals(content)) {
                paymentMethod = RecognizerUtils.firstReadableAfter(nodes, i);
            }
            if ("支付时间".equals(content) || "交易时间".equals(content) || "退款时间".equals(content)) {
                timeText = RecognizerUtils.firstReadableAfter(nodes, i);
            }
        }

        if (!billDetail || amount < 0) return RecognitionResult.empty();

        String note = chooseNote(productNote, directBelowNote, merchantNote, fallbackNote);
        if (note.isEmpty()) note = "微信账单";
        if (note.length() > 50) note = note.substring(0, 48) + "...";

        long now = System.currentTimeMillis();
        long timestamp = parseWeChatTime(timeText, now);
        String displayTime = RecognizerUtils.formatDisplayTime(timestamp);
        String recordNote = displayTime + " " + note;
        String category = RecognizerUtils.inferShoppingOrDining(note);
        if (paymentMethod.isEmpty()) paymentMethod = "微信";
        String signature = "wechat_bill_detail-" + amount + "-" + type + "-" + note + "-" + timeText;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "微信" : snapshot.getAppName())
                .type(type)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint(category)
                .merchant(note)
                .paymentMethod(paymentMethod)
                .note(recordNote)
                .transactionTimeMillis(timestamp)
                .signature(signature)
                .confidence(0.88)
                .build();
        return RecognitionResult.matched(candidate, "微信账单详情页");
    }

    private String findFallbackNoteBefore(List<NodeSnapshot> nodes, int index) {
        for (int i = index - 1; i >= 0; i--) {
            String content = nodes.get(i).getReadableText();
            if (!content.isEmpty() && !content.contains("支出") && !content.contains("收入")) {
                return content;
            }
        }
        return "";
    }

    private String findDirectNoteAfterAmount(List<NodeSnapshot> nodes, int index) {
        for (int i = index + 1; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;
            if (!content.contains("原价") && !content.contains("优惠") && !content.contains("￥") && !content.contains("¥")) {
                return content;
            }
            return "";
        }
        return "";
    }

    private String chooseNote(String productNote, String directBelowNote, String merchantNote, String fallbackNote) {
        if (!productNote.isEmpty() && !productNote.startsWith("商户单号") && !productNote.startsWith("交易单号")) {
            return productNote;
        }
        if (!directBelowNote.isEmpty() && !directBelowNote.contains("交易详情") && !directBelowNote.contains("账单详情")) {
            return directBelowNote;
        }
        if (!merchantNote.isEmpty()) return merchantNote;
        if (!fallbackNote.isEmpty() && !fallbackNote.contains("交易详情") && !fallbackNote.contains("账单详情")) {
            return fallbackNote;
        }
        return "";
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
}
