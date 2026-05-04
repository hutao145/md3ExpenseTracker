package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.util.List;

public final class DouyinPaymentRecognizer implements PageRecognizer {
    private static final String DOUYIN_PACKAGE = "com.ss.android.ugc.aweme";

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && DOUYIN_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean paymentPage = false;
        String paymentMethod = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if ("输入支付密码".equals(content) || content.contains("免密支付协议")) {
                paymentPage = true;
            }
            if (amount < 0 && content.matches("^\\d+\\.\\d{2}$")) {
                Double parsed = RecognizerUtils.parseAmount(content);
                if (parsed != null) amount = parsed;
            }
            if ("支付方式".equals(content)) {
                paymentMethod = RecognizerUtils.firstReadableAfter(nodes, i);
            }
        }

        if (!paymentPage || amount <= 0) return RecognitionResult.empty();
        if (paymentMethod.isEmpty()) paymentMethod = "抖音";
        long now = System.currentTimeMillis();
        String note = RecognizerUtils.formatDisplayTime(now) + " 抖音购物";

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "抖音" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("购物")
                .merchant("抖音购物")
                .paymentMethod(paymentMethod)
                .note(note)
                .transactionTimeMillis(now)
                .signature("douyin_payment-" + amount + "-" + paymentMethod)
                .confidence(0.84)
                .build();
        return RecognitionResult.matched(candidate, "抖音支付确认页");
    }
}
