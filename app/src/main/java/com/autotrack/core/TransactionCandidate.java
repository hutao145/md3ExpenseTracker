package com.autotrack.core;

public final class TransactionCandidate {
    private final String sourcePackage;
    private final String sourceAppName;
    private final RecordType type;
    private final double amount;
    private final String currencySymbol;
    private final String categoryHint;
    private final String merchant;
    private final String paymentMethod;
    private final String note;
    private final long transactionTimeMillis;
    private final String signature;
    private final double confidence;

    private TransactionCandidate(Builder builder) {
        this.sourcePackage = builder.sourcePackage;
        this.sourceAppName = builder.sourceAppName;
        this.type = builder.type;
        this.amount = builder.amount;
        this.currencySymbol = builder.currencySymbol;
        this.categoryHint = builder.categoryHint;
        this.merchant = builder.merchant;
        this.paymentMethod = builder.paymentMethod;
        this.note = builder.note;
        this.transactionTimeMillis = builder.transactionTimeMillis;
        this.signature = builder.signature;
        this.confidence = builder.confidence;
    }

    public String getSourcePackage() {
        return sourcePackage;
    }

    public String getSourceAppName() {
        return sourceAppName;
    }

    public RecordType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public String getCategoryHint() {
        return categoryHint;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public long getTransactionTimeMillis() {
        return transactionTimeMillis;
    }

    public String getSignature() {
        return signature;
    }

    public double getConfidence() {
        return confidence;
    }

    public Builder toBuilder() {
        return new Builder()
                .sourcePackage(sourcePackage)
                .sourceAppName(sourceAppName)
                .type(type)
                .amount(amount)
                .currencySymbol(currencySymbol)
                .categoryHint(categoryHint)
                .merchant(merchant)
                .paymentMethod(paymentMethod)
                .note(note)
                .transactionTimeMillis(transactionTimeMillis)
                .signature(signature)
                .confidence(confidence);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sourcePackage = "";
        private String sourceAppName = "";
        private RecordType type = RecordType.UNKNOWN;
        private double amount = -1;
        private String currencySymbol = "¥";
        private String categoryHint = "";
        private String merchant = "";
        private String paymentMethod = "";
        private String note = "";
        private long transactionTimeMillis = System.currentTimeMillis();
        private String signature = "";
        private double confidence = 0.0;

        public Builder sourcePackage(String sourcePackage) {
            this.sourcePackage = safe(sourcePackage);
            return this;
        }

        public Builder sourceAppName(String sourceAppName) {
            this.sourceAppName = safe(sourceAppName);
            return this;
        }

        public Builder type(RecordType type) {
            this.type = type == null ? RecordType.UNKNOWN : type;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder currencySymbol(String currencySymbol) {
            this.currencySymbol = safe(currencySymbol);
            return this;
        }

        public Builder categoryHint(String categoryHint) {
            this.categoryHint = safe(categoryHint);
            return this;
        }

        public Builder merchant(String merchant) {
            this.merchant = safe(merchant);
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            this.paymentMethod = safe(paymentMethod);
            return this;
        }

        public Builder note(String note) {
            this.note = safe(note);
            return this;
        }

        public Builder transactionTimeMillis(long transactionTimeMillis) {
            this.transactionTimeMillis = transactionTimeMillis;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = safe(signature);
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            return this;
        }

        public TransactionCandidate build() {
            if (signature.isEmpty()) {
                signature = sourcePackage + "-" + type + "-" + amount + "-" + merchant + "-" + transactionTimeMillis;
            }
            return new TransactionCandidate(this);
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
