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
    private static final Pattern YUAN_AMOUNT_PATTERN = Pattern.compile("([0-9,]+(\\.\\d{1,2})?)\\s*元");
    private static final long PENDING_SENT_TRANSFER_WINDOW_MILLIS = 2 * 60 * 1000L;

    private PendingSentTransfer pendingSentTransfer;

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && WECHAT_PACKAGE.equals(snapshot.getPackageName());
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        String allText = snapshot.getAllText();
        RecognitionResult confirmedPending = recognizeConfirmedPendingSentTransfer(snapshot, allText);
        if (confirmedPending.isMatched()) return confirmedPending;
        RecognitionResult detail = recognizeTransferDetail(snapshot);
        if (detail.isMatched()) return detail;
        RecognitionResult received = recognizeReceivedTransfer(snapshot);
        if (received.isMatched()) return received;
        return recognizePendingTransfer(snapshot);
    }

    private RecognitionResult recognizeSendingTransfer(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        String allText = snapshot.getAllText();
        String compactText = compact(allText);
        boolean hasTransferContext = containsAny(compactText, "微信转账", "转账", "转账给", "转账金额", "收款方");
        boolean hasPaymentMethod = false;
        boolean hasSendAction = false;
        boolean hasSentSuccess = containsAny(
                compactText,
                "转账成功",
                "已转账",
                "你发起了一笔转账",
                "发起了一笔转账",
                "待对方收款",
                "待朋友确认收钱",
                "朋友确认收钱前",
                "1天内朋友未确认",
                "24小时内未被确认"
        );
        String paymentMethod = "";
        String payee = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            NodeSnapshot node = nodes.get(i);
            String text = node.getText();
            String desc = node.getDescription();
            String content = node.getReadableText();
            if (content.isEmpty()) continue;

            if (containsAny(content, "付款方式", "支付方式", "支付密码", "确认支付", "立即支付", "验证指纹", "微信支付")) {
                hasPaymentMethod = true;
            }
            if (containsAny(content, "转账", "确认转账", "继续转账", "转账给", "转账金额")) {
                hasSendAction = true;
            }
            if (paymentMethod.isEmpty()) {
                paymentMethod = extractPaymentMethod(nodes, i, text, desc, content);
            }
            if (payee.isEmpty()) {
                payee = extractPayee(nodes, i, content);
            }

            amount = parseAmountFromText(content, amount);
            if ("元".equals(text) && i > 0) {
                amount = parsePlainAmount(nodes.get(i - 1).getReadableText(), amount);
            }
        }

        amount = parseAmountFromText(allText, amount);
        if (payee.isEmpty()) payee = extractPayeeFromText(allText);
        if (paymentMethod.isEmpty()) paymentMethod = "微信支付";

        if (!hasTransferContext || amount <= 0) return RecognitionResult.empty();
        if (containsAny(compactText, "已收款", "已存入零钱", "收款到账", "已入账")) {
            return RecognitionResult.empty();
        }

        if (hasSentSuccess || hasPaymentMethod) {
            long now = System.currentTimeMillis();
            String merchant = payee.isEmpty() ? "微信转账" : payee;
            return RecognitionResult.matched(base(snapshot, amount, RecordType.EXPENSE, "转账", merchant, paymentMethod, now,
                    "wechat_transfer_sent-" + amount + "-" + merchant + "-" + hasSentSuccess), "微信转账支付页");
        }

        if (hasSendAction) {
            pendingSentTransfer = new PendingSentTransfer(
                    amount,
                    payee.isEmpty() ? "微信转账" : payee,
                    paymentMethod,
                    System.currentTimeMillis()
            );
        }
        return RecognitionResult.empty();
    }

    private RecognitionResult recognizeConfirmedPendingSentTransfer(PageSnapshot snapshot, String allText) {
        if (pendingSentTransfer == null) {
            return recognizeSendingTransfer(snapshot);
        }
        long now = System.currentTimeMillis();
        if (now - pendingSentTransfer.createdAtMillis > PENDING_SENT_TRANSFER_WINDOW_MILLIS) {
            pendingSentTransfer = null;
            return recognizeSendingTransfer(snapshot);
        }
        if (!isPaymentVerificationPage(allText)) {
            return recognizeSendingTransfer(snapshot);
        }

        PendingSentTransfer pending = pendingSentTransfer;
        pendingSentTransfer = null;
        return RecognitionResult.matched(base(snapshot, pending.amount, RecordType.EXPENSE, "转账", pending.payee,
                pending.paymentMethod, now, "wechat_transfer_sent-" + pending.amount + "-" + pending.payee + "-" + pending.createdAtMillis),
                "微信转账支付验证页");
    }

    private RecognitionResult recognizePendingTransfer(PageSnapshot snapshot) {
        List<NodeSnapshot> nodes = snapshot.flatten();
        boolean pending = false;
        String info = "";
        double amount = -1;

        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (content.contains("待") && content.contains("确认收款")) {
                pending = true;
                info = content;
            }
            amount = parseAmountFromText(content, amount);
            if ("元".equals(nodes.get(i).getText()) && i > 0) {
                amount = parsePlainAmount(nodes.get(i - 1).getReadableText(), amount);
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
        if (!containsAny(allText, "已收款", "已存入零钱", "收款成功", "零钱已入账", "转账已入账")) {
            return RecognitionResult.empty();
        }
        double amount = -1;
        String note = "微信转账已收款";
        List<NodeSnapshot> nodes = snapshot.flatten();
        for (int i = 0; i < nodes.size(); i++) {
            String content = nodes.get(i).getReadableText();
            if (amount < 0) {
                amount = parseAmountFromText(content, amount);
                if ("元".equals(nodes.get(i).getText()) && i > 0) {
                    amount = parsePlainAmount(nodes.get(i - 1).getReadableText(), amount);
                }
            } else if ((content.contains("转账") || content.contains("收款")) && content.length() < 40) {
                note = content;
            }
        }
        amount = parseAmountFromText(allText, amount);
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

    private double parseAmountFromText(String content, double fallback) {
        if (content == null || content.isEmpty()) return fallback;
        Double currency = parseCurrencyAmount(content);
        if (currency != null) return currency;
        Matcher yuanMatcher = YUAN_AMOUNT_PATTERN.matcher(content.replace(",", ""));
        while (yuanMatcher.find()) {
            try {
                double value = Double.parseDouble(yuanMatcher.group(1));
                if (value > 0 && value < 1000000) return value;
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private double parsePlainAmount(String value, double fallback) {
        try {
            double amount = Double.parseDouble(value.replace(",", "").trim());
            return amount > 0 && amount < 1000000 ? amount : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean isPaymentVerificationPage(String allText) {
        if (allText == null) return false;
        return containsAny(compact(allText), "请验证指纹", "验证指纹", "输入支付密码", "请输入支付密码", "支付密码", "确认支付", "微信支付");
    }

    private String extractPaymentMethod(List<NodeSnapshot> nodes, int index, String text, String desc, String content) {
        if (desc.contains("付款方式") && desc.contains("已选择")) {
            String[] parts = desc.split(",");
            for (String part : parts) {
                if (part.startsWith("已选择")) return part.replace("已选择", "").trim();
            }
        }
        if (("付款方式".equals(text) || "支付方式".equals(text) || content.contains("付款方式") || content.contains("支付方式"))
                && index + 1 < nodes.size()) {
            String next = nodes.get(index + 1).getReadableText();
            if (!next.isEmpty() && !containsAny(next, "更改", "付款方式", "支付方式")) return next;
        }
        if (containsAny(content, "零钱", "零钱通", "银行卡", "信用卡", "储蓄卡")) return content;
        return "";
    }

    private String extractPayee(List<NodeSnapshot> nodes, int index, String content) {
        String fromText = extractPayeeFromText(content);
        if (!fromText.isEmpty()) return fromText;
        if (containsAny(content, "转账给", "收款方", "对方") && index + 1 < nodes.size()) {
            String next = nodes.get(index + 1).getReadableText();
            if (!next.isEmpty() && !containsAny(next, "转账", "金额", "￥", "¥", "元")) return next;
        }
        return "";
    }

    private String extractPayeeFromText(String text) {
        if (text == null || text.isEmpty()) return "";
        Pattern[] patterns = new Pattern[]{
                Pattern.compile("转账给\\s*([^\\s¥￥元]{1,24})"),
                Pattern.compile("向\\s*([^\\s¥￥元]{1,24})\\s*转账"),
                Pattern.compile("收款方\\s*([^\\s¥￥元]{1,24})")
        };
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) return matcher.group(1).trim();
        }
        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty()) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String compact(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "");
    }

    private static final class PendingSentTransfer {
        private final double amount;
        private final String payee;
        private final String paymentMethod;
        private final long createdAtMillis;

        private PendingSentTransfer(double amount, String payee, String paymentMethod, long createdAtMillis) {
            this.amount = amount;
            this.payee = payee == null || payee.isEmpty() ? "微信转账" : payee;
            this.paymentMethod = paymentMethod == null || paymentMethod.isEmpty() ? "微信支付" : paymentMethod;
            this.createdAtMillis = createdAtMillis;
        }
    }
}
