package com.example.quanlychitieu.utils;

import android.util.Log;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {

    private static final String TAG = "CurrencyFormatter";


    public static String formatNumber(double amount) {
        try {
            // This is the key fix: use '#,##0' pattern which explicitly tells the formatter
            // to use grouping (and will use whatever grouping separator we define)
            DecimalFormat formatter = new DecimalFormat("#,##0");

            // Set the grouping separator to dot for Vietnamese format
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');

            formatter.setDecimalFormatSymbols(symbols);
            // Make sure grouping is used (sometimes needed explicitly)
            formatter.setGroupingUsed(true);

            return formatter.format(amount);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting number: " + amount, e);
            return String.valueOf(amount);
        }
    }


    // Chuyển đổi chuỗi định dạng tiền tệ sang double
    public static double parseVietnamCurrency(String amount) {
        if (amount == null || amount.isEmpty()) {
            return 0;
        }

        try {
            // Loại bỏ ký tự đơn vị tiền tệ và dấu chấm phân cách
            String cleanAmount = amount.replace("đ", "")
                    .replace(".", "")
                    .replace(",", ".")  // Nếu có dấu phẩy thập phân
                    .trim();

            return Double.parseDouble(cleanAmount);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing to double: " + amount, e);
            return 0;
        }
    }
    
}
