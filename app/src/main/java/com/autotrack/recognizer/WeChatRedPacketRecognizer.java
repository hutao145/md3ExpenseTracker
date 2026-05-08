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
    private static final Pattern YUAN_AMOUNT_PATTERN = Pattern.compile("([0-9,]+(\\.\\d{1,2})?)\\s*元");
    private static final long PENDING_SENT_PACKET_WINDOW_MILLIS = 2 * 60 * 1000L;
    private static final long FRAGMENTED_PACKET_WINDOW_MILLIS = 2 * 60 * 1000L;

    private PendingSentRedPacket pendingSentRedPacket;
    private FragmentedSentRedPacket fragmentedSentRedPacket;

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
        String allText = snapshot.getAllText();
        boolean storedInBalance = false;
        boolean receivedMarker = false;
        String redPacketName = "微信红包";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            NodeSnapshot node = nodes.get(i);
            String text = node.getText();
            String desc = node.getDescription();
            String content = node.getReadableText();

            if (containsAny(text, "已存入零钱", "已存入零钱通")
                    || containsAny(desc, "已存入零钱", "已存入零钱通")
                    || containsAny(content, "已存入零钱", "已存入零钱通")) {
                storedInBalance = true;
            }
            if (containsAny(content, "已领取", "领取成功", "已拆开", "已收下", "已存入零钱")
                    && !containsAny(content, "对方已领取", "已被领取")) {
                receivedMarker = true;
            }
            if (text.endsWith("的红包") || content.endsWith("的红包")) {
                redPacketName = content.isEmpty() ? text : content;
            }
            if ("元".equals(text) && i > 0) {
                amount = parsePlainAmount(nodes.get(i - 1).getReadableText(), amount);
            }
            amount = parseAmountFromText(content, amount);
        }

        if (!storedInBalance && containsAny(allText, "已存入零钱", "已存入零钱通", "零钱已入账")) {
            storedInBalance = true;
        }
        if (!receivedMarker && containsAny(allText, "已领取", "领取成功", "已拆开", "已收下", "已存入零钱")
                && !containsAny(allText, "对方已领取", "已被领取")) {
            receivedMarker = true;
        }
        if ("微信红包".equals(redPacketName)) {
            String extracted = extractRedPacketName(allText);
            if (!extracted.isEmpty()) redPacketName = extracted;
        }
        amount = parseAmountFromText(allText, amount);

        boolean redPacketContext = containsAny(allText, "微信红包", "红包", "恭喜发财", "大吉大利");
        if ((!storedInBalance && !receivedMarker) || !redPacketContext || amount <= 0) {
            return RecognitionResult.empty();
        }
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
        RecognitionResult fragmented = recognizeFragmentedSendingRedPacket(snapshot, allText);
        if (fragmented.isMatched()) return fragmented;

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
                    || content.contains("立即支付")
                    || content.contains("验证指纹")
                    || content.contains("微信支付")) {
                hasPaymentMethod = true;
            }
            if (containsAny(content, "塞钱进红包", "发红包", "红包金额", "单个金额", "总金额")) {
                hasSendRedPacketButton = true;
            }
            if ("微信红包".equals(text) || "微信红包".equals(desc)
                    || content.contains("微信红包")
                    || content.contains("发红包")
                    || content.contains("红包")) {
                hasWeChatRedPacket = true;
            }
            amount = parseAmountFromText(content, amount);
            if ("元".equals(text) && i > 0) {
                amount = parsePlainAmount(nodes.get(i - 1).getReadableText(), amount);
            }
            if (desc.contains("付款方式") && desc.contains("已选择") && desc.contains("更改")) {
                assetName = parseSelectedAsset(desc);
            }
            if (("更改".equals(text) || "更改".equals(desc)
                    || "支付方式".equals(text) || "付款方式".equals(text)) && assetName.isEmpty() && i + 1 < nodes.size()) {
                String next = nodes.get(i + 1).getReadableText();
                if (!next.isEmpty()) assetName = next;
            }
        }

        if (!hasPaymentMethod && (allText.contains("付款方式") || allText.contains("支付方式")
                || allText.contains("支付密码") || allText.contains("确认支付") || allText.contains("立即支付"))) {
            hasPaymentMethod = true;
        }
        if (!hasPaymentMethod && containsAny(allText, "验证指纹", "输入支付密码", "请输入支付密码", "微信支付")) {
            hasPaymentMethod = true;
        }
        if (!hasSendRedPacketButton && containsAny(allText, "塞钱进红包", "发红包", "红包金额", "单个金额", "总金额")) {
            hasSendRedPacketButton = true;
        }
        if (!hasWeChatRedPacket && containsAny(allText, "微信红包", "红包", "发红包", "恭喜发财", "大吉大利")) {
            hasWeChatRedPacket = true;
        }
        amount = parseAmountFromText(allText, amount);

        if (!hasWeChatRedPacket || amount <= 0) return RecognitionResult.empty();
        if (containsAny(allText, "已存入零钱", "领取成功", "已收下")) {
            return RecognitionResult.empty();
        }
        if (assetName.isEmpty()) assetName = "微信支付";

        if (hasSendRedPacketButton && !hasPaymentMethod) {
            cachePendingSentRedPacket(snapshot, amount, assetName);
            return RecognitionResult.empty();
        }

        boolean sentSuccess = containsAny(allText, "红包已发送", "已发送", "等待对方领取", "对方已领取");
        if (!hasPaymentMethod && !sentSuccess) return RecognitionResult.empty();

        long now = System.currentTimeMillis();
        String note = formatDisplayTime(now) + " 微信红包";
        String signature = "wechat_red_packet_sent-" + amount + "-" + assetName + "-" + sentSuccess;

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

    private RecognitionResult recognizeFragmentedSendingRedPacket(PageSnapshot snapshot, String allText) {
        String compactText = compact(allText);
        if (compactText.isEmpty()) return RecognitionResult.empty();
        long now = System.currentTimeMillis();

        if (fragmentedSentRedPacket != null
                && now - fragmentedSentRedPacket.updatedAtMillis > FRAGMENTED_PACKET_WINDOW_MILLIS) {
            fragmentedSentRedPacket = null;
        }

        boolean redPacketFlowText = containsAny(compactText,
                "红包",
                "发红包",
                "填写个数",
                "红包个数",
                "塞钱进红包",
                "微信支付"
        );
        if (!redPacketFlowText && fragmentedSentRedPacket == null) {
            return RecognitionResult.empty();
        }

        FragmentedSentRedPacket state = ensureFragmentedSentRedPacket(now);
        state.updatedAtMillis = now;

        if (containsAny(compactText, "填写个数")) {
            state.expectingCountInput = true;
            return RecognitionResult.empty();
        }
        if (containsAny(compactText, "红包个数")) {
            state.expectingCountInput = false;
            state.countConfirmed = true;
            return RecognitionResult.empty();
        }

        Double standaloneAmount = parseStandaloneAmount(compactText);
        if (standaloneAmount != null) {
            if (state.expectingCountInput) {
                state.expectingCountInput = false;
                state.countConfirmed = true;
                return RecognitionResult.empty();
            }
            state.amount = standaloneAmount;
        }

        double parsedAmount = parseAmountFromText(compactText, -1);
        if (parsedAmount > 0) {
            state.amount = parsedAmount;
        }

        if (containsAny(compactText, "塞钱进红包")) {
            if (state.amount > 0) {
                cachePendingSentRedPacket(snapshot, state.amount, state.assetName);
            }
            return RecognitionResult.empty();
        }

        if (containsAny(compactText, "微信支付", "支付密码", "验证指纹", "确认支付") && state.amount > 0) {
            fragmentedSentRedPacket = null;
            String assetName = state.assetName.isEmpty() ? "微信支付" : state.assetName;
            String note = formatDisplayTime(now) + " 微信红包";
            String signature = "wechat_red_packet_sent-" + state.amount + "-" + assetName + "-" + now;
            TransactionCandidate candidate = TransactionCandidate.builder()
                    .sourcePackage(snapshot.getPackageName())
                    .sourceAppName(snapshot.getAppName().isEmpty() ? "微信" : snapshot.getAppName())
                    .type(RecordType.EXPENSE)
                    .amount(state.amount)
                    .currencySymbol("¥")
                    .categoryHint("红包")
                    .merchant("微信红包")
                    .paymentMethod(assetName)
                    .note(note)
                    .transactionTimeMillis(now)
                    .signature(signature)
                    .confidence(0.78)
                    .build();
            return RecognitionResult.matched(candidate, "微信发红包碎片化支付页");
        }

        return RecognitionResult.empty();
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
                || allText.contains("支付密码")
                || allText.contains("确认支付")
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

    private double parseYuanAmount(String value, double fallback) {
        Matcher matcher = YUAN_AMOUNT_PATTERN.matcher(value.replace(",", ""));
        while (matcher.find()) {
            try {
                double amount = Double.parseDouble(matcher.group(1));
                if (amount > 0 && amount < 1000000) return amount;
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private double parseAmountFromText(String value, double fallback) {
        if (value == null || value.isEmpty()) return fallback;
        double amount = parseCurrencyAmount(value, fallback);
        return parseYuanAmount(value, amount);
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

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty()) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private FragmentedSentRedPacket ensureFragmentedSentRedPacket(long now) {
        if (fragmentedSentRedPacket == null) {
            fragmentedSentRedPacket = new FragmentedSentRedPacket(now);
        }
        return fragmentedSentRedPacket;
    }

    private Double parseStandaloneAmount(String text) {
        if (text == null || text.isEmpty()) return null;
        if (!text.matches("^[0-9]+(\\.[0-9]{1,2})?$")) return null;
        try {
            double amount = Double.parseDouble(text);
            if (amount > 0 && amount < 1000000) return amount;
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private String compact(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "");
    }

    private String extractRedPacketName(String allText) {
        if (allText == null || allText.isEmpty()) return "";
        Matcher matcher = Pattern.compile("([^\\s]{1,24}的红包)").matcher(allText);
        if (matcher.find()) return matcher.group(1);
        return "";
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

    private static final class FragmentedSentRedPacket {
        private double amount = -1;
        private String assetName = "微信支付";
        private boolean expectingCountInput;
        private boolean countConfirmed;
        private long updatedAtMillis;

        private FragmentedSentRedPacket(long now) {
            this.updatedAtMillis = now;
        }
    }
}
