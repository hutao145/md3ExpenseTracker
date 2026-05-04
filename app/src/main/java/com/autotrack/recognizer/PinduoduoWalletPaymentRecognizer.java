package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.util.List;

public final class PinduoduoWalletPaymentRecognizer implements PageRecognizer {
    private static final String PDD_PACKAGE = "com.xunmeng.pinduoduo";

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && PDD_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean pddPayment = false;
        String paymentMethod = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            NodeSnapshot node = nodes.get(i);
            String text = node.getText();
            String desc = node.getDescription();
            String content = node.getReadableText();
            if ("请输入多多钱包密码".equals(content) || content.contains("多多钱包密码") || content.contains("密码输入框")) {
                pddPayment = true;
            }
            if (desc.startsWith("人民币") && desc.endsWith("元") && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(desc.replace("人民币", "").replace("元", ""));
                if (parsed != null && parsed > 0) amount = parsed;
            } else if (text.matches("^\\d+\\.\\d{2}$") && amount < 0) {
                Double parsed = RecognizerUtils.parseAmount(text);
                if (parsed != null) amount = parsed;
            }
            if ("支付方式".equals(content)) {
                paymentMethod = joinPaymentMethod(nodes, i);
            }
        }

        if (!pddPayment || amount <= 0) return RecognitionResult.empty();
        if (paymentMethod.isEmpty()) paymentMethod = "多多钱包";
        long now = System.currentTimeMillis();
        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "拼多多" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("购物")
                .merchant("拼多多购物")
                .paymentMethod(paymentMethod)
                .note(RecognizerUtils.formatDisplayTime(now) + " 拼多多购物")
                .transactionTimeMillis(now)
                .signature("pdd_wallet_payment-" + amount + "-" + paymentMethod)
                .confidence(0.84)
                .build();
        return RecognitionResult.matched(candidate, "拼多多多多钱包支付页");
    }

    private String joinPaymentMethod(List<NodeSnapshot> nodes, int index) {
        StringBuilder builder = new StringBuilder();
        for (int i = index + 1; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.isEmpty()) continue;
            if (content.length() == 1 || "找回密码".equals(content) || content.contains("密码")) break;
            builder.append(content);
            if (content.contains(")")) break;
        }
        return builder.toString().trim();
    }
}
