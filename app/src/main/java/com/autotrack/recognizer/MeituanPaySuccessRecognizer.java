package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MeituanPaySuccessRecognizer implements PageRecognizer {
    private static final Pattern AMOUNT_WITH_SYMBOL = Pattern.compile("[¥￥](\\d+(?:\\.\\d{1,2})?)");

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && snapshot.getPackageName().contains("meituan");
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean paySuccess = false;
        double amount = -1;
        String paymentMethod = "";

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.contains("支付成功")) {
                paySuccess = true;
                Matcher matcher = AMOUNT_WITH_SYMBOL.matcher(content);
                if (matcher.find()) {
                    Double parsed = RecognizerUtils.parseAmount(matcher.group(1));
                    if (parsed != null) amount = parsed;
                }
            }
            if ("支付方式".equals(content)) {
                paymentMethod = RecognizerUtils.firstReadableAfter(nodes, i);
            }
        }

        if (!paySuccess || amount <= 0) return RecognitionResult.empty();
        if (paymentMethod.isEmpty()) paymentMethod = "美团";
        long now = System.currentTimeMillis();
        String note = RecognizerUtils.formatDisplayTime(now) + " 美团消费";

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "美团" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("餐饮")
                .merchant("美团消费")
                .paymentMethod(paymentMethod)
                .note(note)
                .transactionTimeMillis(now)
                .signature("meituan_pay_success-" + amount + "-" + paymentMethod)
                .confidence(0.86)
                .build();
        return RecognitionResult.matched(candidate, "美团支付成功页");
    }
}
