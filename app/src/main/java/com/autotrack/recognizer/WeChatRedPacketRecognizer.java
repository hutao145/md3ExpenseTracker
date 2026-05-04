package com.autotrack.recognizer;

import com.autotrack.core.NodeSnapshot;
import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeChatRedPacketRecognizer implements PageRecognizer {
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final Pattern CURRENCY_AMOUNT_PATTERN = Pattern.compile("[¥￥]\\s*([0-9,]+(\\.\\d{1,2})?)");
    private static final long PENDING_SENT_PACKET_WINDOW_MILLIS = 2 * 60 * 1000L;

    private PendingSentRedPacket pendingSentRedPacket;

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && WECHAT_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        RecognitionResult received = recognizeReceivedRedPacket(snapshot);
        if (received.isMatched()) return received;
        return recognizeSendingRedPacket(snapshot);
    }

    private RecognitionResult recognizeReceivedRedPacket(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean storedInBalance = false;
        String redPacketName = "微信红包";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            NodeSnapshot node = nodes.get(i);
            String text = node.getText();
            String desc = node.getDescription();

            if (desc.contains("已存入零钱")) {
                storedInBalance = true;
            }
            if (text.endsWith("的红包")) {
                redPacketName = text;
            }
            if ("元".equals(text) && i > 0) {
                amount = parsePlainAmount(nodes.get(i - 1).getReadableText(), amount);
            }
        }

        if (!storedInBalance || amount <= 0) return RecognitionResult.empty();
        long now = System.currentTimeMillis();
        String note = formatDisplayTime(now) + " " + redPacketName;
        String signature = "wechat_red_packet_received-" + amount + "-" + redPacketName;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "微信" : snapshot.getAppName())
                .type(RecordType.INCOME)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("红包")
                .merchant(redPacketName)
                .paymentMethod("微信零钱")
                .note(note)
                .transactionTimeMillis(now)
                .signature(signature)
                .confidence(0.9)
                .build();
        return RecognitionResult.matched(candidate, "微信红包领取详情页");
    }

    private RecognitionResult recognizeSendingRedPacket(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        String allText = snapshot.getAllText();
        RecognitionResult confirmedPending = recognizeConfirmedPendingSentRedPacket(snapshot, allText);
        if (confirmedPending.isMatched()) return confirmedPending;

        boolean hasPaymentMethod = false;
        boolean hasWeChatRedPacket = false;
        boolean hasSendRedPacketButton = false;
        String assetName = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            NodeSnapshot node = nodes.get(i);
            String text = node.getText();
            String desc = node.getDescription();
            String content = node.getReadableText();

            if ("付款方式".equals(text) || "付款方式".equals(desc)
                    || "支付方式".equals(text) || "支付方式".equals(desc)
                    || desc.contains("付款方式,已选择")
                    || content.contains("支付密码")
                    || content.contains("确认支付")
                    || content.contains("立即支付")) {
                hasPaymentMethod = true;
            }
            if (content.contains("塞钱进红包")) {
                hasSendRedPacketButton = true;
            }
            if ("微信红包".equals(text) || "微信红包".equals(desc)
                    || content.contains("微信红包")
                    || content.contains("发红包")
                    || content.contains("红包")) {
                hasWeChatRedPacket = true;
            }
            if (content.contains("￥") || content.contains("¥")) {
                amount = parseCurrencyAmount(content, amount);
            }
            if (desc.contains("付款方式") && desc.contains("已选择") && desc.contains("更改")) {
                assetName = parseSelectedAsset(desc);
            }
            if (("更改".equals(text) || "更改".equals(desc)) && assetName.isEmpty() && i + 1 < nodes.size()) {
                String next = nodes.get(i + 1).getReadableText();
                if (!next.isEmpty()) assetName = next;
            }
        }

        if (!hasPaymentMethod && (allText.contains("付款方式") || allText.contains("支付方式")
                || allText.contains("支付密码") || allText.contains("确认支付") || allText.contains("立即支付"))) {
            hasPaymentMethod = true;
        }
        if (!hasSendRedPacketButton && allText.contains("塞钱进红包")) {
            hasSendRedPacketButton = true;
        }
        if (!hasWeChatRedPacket && (allText.contains("红包") || allText.contains("发红包"))) {
            hasWeChatRedPacket = true;
        }

        if (!hasWeChatRedPacket || amount <= 0) return RecognitionResult.empty();
        if (assetName.isEmpty()) assetName = "微信支付";

        if (hasSendRedPacketButton && !hasPaymentMethod) {
            cachePendingSentRedPacket(snapshot, amount, assetName);
            return RecognitionResult.empty();
        }

        if (!hasPaymentMethod) return RecognitionResult.empty();

        long now = System.currentTimeMillis();
        String note = formatDisplayTime(now) + " 微信红包";
        String signature = "wechat_red_packet_sent-" + amount + "-" + assetName;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "微信" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint("红包")
                .merchant("微信红包")
                .paymentMethod(assetName)
                .note(note)
                .transactionTimeMillis(now)
                .signature(signature)
                .confidence(0.9)
                .build();
        return RecognitionResult.matched(candidate, "微信发红包支付确认页");
    }

    private RecognitionResult recognizeConfirmedPendingSentRedPacket(PageSnapshot snapshot, String allText) {
        if (pendingSentRedPacket == null) return RecognitionResult.empty();
        long now = System.currentTimeMillis();
        if (now - pendingSentRedPacket.createdAtMillis > PENDING_SENT_PACKET_WINDOW_MILLIS) {
            pendingSentRedPacket = null;
            return RecognitionResult.empty();
        }
        if (!isPaymentVerificationPage(allText)) return RecognitionResult.empty();

        PendingSentRedPacket pending = pendingSentRedPacket;
        pendingSentRedPacket = null;
        String note = formatDisplayTime(now) + " 微信红包";
        String signature = "wechat_red_packet_sent-" + pending.amount + "-" + pending.assetName + "-" + pending.createdAtMillis;

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName().isEmpty() ? "微信" : snapshot.getAppName())
                .type(RecordType.EXPENSE)
                .amount(pending.amount)
                .currencySymbol("¥")
                .categoryHint("红包")
                .merchant("微信红包")
                .paymentMethod(pending.assetName)
                .note(note)
                .transactionTimeMillis(now)
                .signature(signature)
                .confidence(0.82)
                .build();
        return RecognitionResult.matched(candidate, "微信发红包支付验证页");
    }

    private boolean isPaymentVerificationPage(String allText) {
        if (allText == null) return false;
        return allText.contains("请验证指纹")
                || allText.contains("验证指纹")
                || allText.contains("输入支付密码")
                || allText.contains("请输入支付密码")
                || allText.contains("微信支付");
    }

    private void cachePendingSentRedPacket(PageSnapshot snapshot, double amount, String assetName) {
        pendingSentRedPacket = new PendingSentRedPacket(
                amount,
                assetName == null || assetName.isEmpty() ? "微信支付" : assetName,
                System.currentTimeMillis()
        );
    }

    private double parsePlainAmount(String value, double fallback) {
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double parseCurrencyAmount(String value, double fallback) {
        Matcher matcher = CURRENCY_AMOUNT_PATTERN.matcher(value.replace(",", ""));
        while (matcher.find()) {
            try {
                double amount = Double.parseDouble(matcher.group(1));
                if (amount > 0 && amount < 1000000) return amount;
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String parseSelectedAsset(String desc) {
        String[] parts = desc.split(",");
        for (String part : parts) {
            if (part.startsWith("已选择")) {
                return part.replace("已选择", "").trim();
            }
        }
        return "";
    }

    private String formatDisplayTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private static final class PendingSentRedPacket {
        private final double amount;
        private final String assetName;
        private final long createdAtMillis;

        private PendingSentRedPacket(double amount, String assetName, long createdAtMillis) {
            this.amount = amount;
            this.assetName = assetName;
            this.createdAtMillis = createdAtMillis;
        }
    }
}
