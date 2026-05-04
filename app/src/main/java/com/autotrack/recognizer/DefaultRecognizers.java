package com.autotrack.recognizer;

import com.autotrack.core.PageRecognizer;

import java.util.Arrays;
import java.util.List;

public final class DefaultRecognizers {
    private DefaultRecognizers() {
    }

    public static List<PageRecognizer> create() {
        return Arrays.asList(
                new AlipayBillDetailRecognizer(),
                new AlipayPaySuccessRecognizer(),
                new WeChatRedPacketRecognizer(),
                new WeChatTransferRecognizer(),
                new WeChatBillDetailRecognizer(),
                new WeChatPaySuccessRecognizer(),
                new PinduoduoOrderDetailRecognizer(),
                new PinduoduoWalletPaymentRecognizer(),
                new UnionPayRecognizer(),
                new JDPaySuccessRecognizer(),
                new DouyinPaymentRecognizer(),
                new DouShengShengPaymentRecognizer(),
                new MeituanPaySuccessRecognizer(),
                new QwenPaymentConfirmRecognizer(),
                new GenericKeywordAmountRecognizer()
        );
    }
}
