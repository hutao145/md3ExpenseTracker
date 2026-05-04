package com.autotrack.recognizer;

import com.autotrack.core.PageRecognizer;
import com.autotrack.core.PageSnapshot;
import com.autotrack.core.RecognitionResult;
import com.autotrack.core.RecordType;
import com.autotrack.core.TransactionCandidate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenericKeywordAmountRecognizer implements PageRecognizer {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([^0-9\\s]{0,3})\\s*([0-9,]+(\\.\\d{1,2})?)");
    private static final Pattern CURRENCY_AMOUNT_PATTERN = Pattern.compile("[¥￥]\\s*([0-9,]+(\\.\\d{1,2})?)");

    private final Set<String> expenseKeywords = new HashSet<>(Arrays.asList("支付成功", "付款成功", "已支付", "支出"));
    private final Set<String> incomeKeywords = new HashSet<>(Arrays.asList("收款成功", "已收款", "收入", "到账"));

    @Override
    public boolean supports(PageSnapshot snapshot) {
        return snapshot != null && !snapshot.getAllText().isEmpty();
    }

    @Override
    public RecognitionResult recognize(PageSnapshot snapshot) {
        String text = snapshot.getAllText();
        RecordType type = resolveType(text);
        if (type == RecordType.UNKNOWN) return RecognitionResult.empty();

        Double amount = findLikelyAmount(text);
        if (amount == null || amount <= 0) return RecognitionResult.empty();

        TransactionCandidate candidate = TransactionCandidate.builder()
                .sourcePackage(snapshot.getPackageName())
                .sourceAppName(snapshot.getAppName())
                .type(type)
                .amount(amount)
                .currencySymbol("¥")
                .categoryHint(type == RecordType.INCOME ? "其他收入" : snapshot.getAppName())
                .note(snapshot.getAppName())
                .confidence(0.45)
                .build();
        return RecognitionResult.matched(candidate, "通用关键字金额识别");
    }

    private RecordType resolveType(String text) {
        for (String keyword : incomeKeywords) {
            if (text.contains(keyword)) return RecordType.INCOME;
        }
        for (String keyword : expenseKeywords) {
            if (text.contains(keyword)) return RecordType.EXPENSE;
        }
        return RecordType.UNKNOWN;
    }

    private Double findLikelyAmount(String text) {
        Double currencyAmount = findCurrencyAmount(text);
        if (currencyAmount != null) return currencyAmount;

        String cleanText = text
                .replaceAll("\\d{4}年\\d{1,2}月\\d{1,2}日\\s*\\d{1,2}:\\d{2}(:\\d{2})?", " ")
                .replaceAll("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\s*\\d{1,2}:\\d{2}(:\\d{2})?", " ")
                .replaceAll("\\d{1,2}:\\d{2}(:\\d{2})?", " ")
                .replaceAll("\\b(19|20)\\d{2}\\b", " ");
        Matcher matcher = AMOUNT_PATTERN.matcher(cleanText);
        double best = -1;
        while (matcher.find()) {
            try {
                String number = matcher.group(2).replace(",", "");
                double value = Double.parseDouble(number);
                if (value > best && value < 1000000 && !looksLikeCalendarNumber(value)) best = value;
            } catch (NumberFormatException ignored) {
            }
        }
        return best > 0 ? best : null;
    }

    private Double findCurrencyAmount(String text) {
        Matcher matcher = CURRENCY_AMOUNT_PATTERN.matcher(text.replace(",", ""));
        while (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                if (value > 0 && value < 1000000) return value;
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private boolean looksLikeCalendarNumber(double value) {
        return value >= 1900 && value <= 2100;
    }
}
