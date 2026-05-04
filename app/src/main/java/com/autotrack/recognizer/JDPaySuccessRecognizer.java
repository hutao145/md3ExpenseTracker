package com.autotrack.recognizer;

import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JDPaySuccessRecognizer implements PageRecognizer {
    private static final String JD_PACKAGE = "com.jingdong.app.mall";
    private static final Pattern COMBINED_PAY_PATTERN = Pattern.compile("(.*?)(?:付款|支付)[¥￥](\\d+(?:\\.\\d{1,2})?)");

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && JD_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        boolean paySuccess = false;
        double amount = -1;
        String paymentMethod = "";

        for (com.autotrack.core.NodeSnapshot node : snapshot.flatten()) {
            String content = node.getReadableText();
            if ("支付成功".equals(content)) {
                paySuccess = true;
            }
            if (amount < 0 && (content.contains("付款¥") || content.contains("付款￥") || content.contains("支付¥") || content.contains("支付￥"))) {
                Matcher matcher = COMBINED_PAY_PATTERN.matcher(content);
                if (matcher.find()) {
                    paymentMethod = matcher.group(1).trim();
                    Double parsed = RecognizerUtils.parseAmount(matcher.group(2));
                    if (parsed != null) amount = parsed;
                }
            }
        }

        if (!paySuccess || amount <= 0) return RecognitionResult.empty();
        if (paymentMethod.isEmpty()) paymentMethod = "京东";
        long now = System.currentTimeMillis();
        String note = RecognizerUtils.formatDisplayTime(now) + " 京东购物";

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "京东" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("购物")
                .merchant("京东购物")
                .paymentMethod(paymentMethod)
                .note(note)
                .transactionTimeMillis(now)
                .signature("jd_pay_success-" + amount + "-" + paymentMethod)
                .confidence(0.86)
                .build();
        return RecognitionResult.matched(candidate, "京东支付成功页");
    }
}
