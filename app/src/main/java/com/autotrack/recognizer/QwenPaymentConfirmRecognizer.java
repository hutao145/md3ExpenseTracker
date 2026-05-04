package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QwenPaymentConfirmRecognizer implements PageRecognizer {
    private static final String PACKAGE_NAME = "com.aliyun.tongyi";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("支付金额(\\d+(?:\\.\\d{1,2})?)元");

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && PACKAGE_NAME.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        boolean confirm = false;
        double amount = -1;
        String note = "千问代下单";

        for (NodeSnapshot node : snapshot.flatten()) {
            String content = node.getReadableText();
            String desc = node.getDescription();
            if ("确认付款".equals(content)) confirm = true;
            Matcher matcher = AMOUNT_PATTERN.matcher(desc);
            if (matcher.find() && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(matcher.group(1));
                if (parsed != null) amount = parsed;
            }
            if (content.contains("帮你下单")) {
                String clean = content.replace("我正在", "")
                        .replace("帮你下单，请确认", "")
                        .replace("帮你下单", "")
                        .trim();
                if (!clean.isEmpty()) note = clean;
            }
        }

        if (!confirm || amount <= 0) return RecognitionResult.empty();
        long now = System.currentTimeMillis();
        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "通义千问" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("购物")
                .merchant(note)
                .paymentMethod("通义千问")
                .note(RecognizerUtils.formatDisplayTime(now) + " " + note)
                .transactionTimeMillis(now)
                .signature("qwen_confirm-" + amount + "-" + note)
                .confidence(0.84)
                .build();
        return RecognitionResult.matched(candidate, "通义千问确认付款页");
    }
}
