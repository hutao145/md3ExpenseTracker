package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.util.List;

public final class DouShengShengPaymentRecognizer implements PageRecognizer {
    private static final String PACKAGE_NAME = "com.ss.android.ugc.lifeservices";

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && PACKAGE_NAME.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean paySuccess = false;
        double amount = -1;
        String paymentMethod = "";

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if ("支付成功".equals(content)) paySuccess = true;
            if (content.matches("^\\d+\\.\\d{2}$") && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(content);
                if (parsed != null) amount = parsed;
            }
            if ("支付方式".equals(content)) paymentMethod = RecognizerUtils.firstReadableAfter(nodes, i);
        }

        if (!paySuccess || amount <= 0) return RecognitionResult.empty();
        if (paymentMethod.isEmpty()) paymentMethod = "抖音支付";
        long now = System.currentTimeMillis();
        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "抖省省" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("购物")
                .merchant("抖省省消费")
                .paymentMethod(paymentMethod)
                .note(RecognizerUtils.formatDisplayTime(now) + " 抖省省消费")
                .transactionTimeMillis(now)
                .signature("doushengsheng_pay-" + amount + "-" + paymentMethod)
                .confidence(0.84)
                .build();
        return RecognitionResult.matched(candidate, "抖省省支付成功页");
    }
}
