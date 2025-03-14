package com.example.quanlychitieu.utils;

import android.util.Log;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {

    private static final String TAG = "CurrencyFormatter";

    // Định dạng tiền tệ Việt Nam: 1.231.323.123đ
    public static String formatVietnamCurrency(double amount) {
        try {
            // Use '#,##0' pattern with explicit grouping
            DecimalFormat formatter = new DecimalFormat("#,##0");

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');

            formatter.setDecimalFormatSymbols(symbols);
            formatter.setGroupingUsed(true);

            return formatter.format(amount) + "đ";
        } catch (Exception e) {
            Log.e(TAG, "Error formatting amount: " + amount, e);
            return String.valueOf(amount) + "đ";
        }
    }

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

    // Định dạng tiền tệ cho số BigDecimal
    public static String formatVietnamCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0đ";
        }
        try {
            DecimalFormat formatter = new DecimalFormat("###,###.###");

            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');

            formatter.setDecimalFormatSymbols(symbols);
            formatter.setMaximumFractionDigits(0);
            formatter.setMinimumFractionDigits(0);

            return formatter.format(amount) + "đ";
        } catch (Exception e) {
            Log.e(TAG, "Error formatting BigDecimal amount: " + amount, e);
            return amount.toString() + "đ";
        }
    }

    // Định dạng số BigDecimal không có ký hiệu đơn vị
    public static String formatNumber(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        try {
            DecimalFormat formatter = new DecimalFormat("###,###.###");

            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');

            formatter.setDecimalFormatSymbols(symbols);
            formatter.setMaximumFractionDigits(0);
            formatter.setMinimumFractionDigits(0);

            return formatter.format(amount);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting BigDecimal number: " + amount, e);
            return amount.toString();
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

    // Chuyển đổi chuỗi định dạng tiền tệ sang BigDecimal
    public static BigDecimal parseToBigDecimal(String amount) {
        if (amount == null || amount.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Loại bỏ ký tự đơn vị tiền tệ và dấu chấm phân cách
            String cleanAmount = amount.replace("đ", "")
                    .replace(".", "")
                    .replace(",", ".")  // Nếu có dấu phẩy thập phân
                    .trim();

            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing to BigDecimal: " + amount, e);
            return BigDecimal.ZERO;
        }
    }

    // Kiểm tra xem một chuỗi có thể chuyển thành số hợp lệ không
    public static boolean isValidAmount(String amount) {
        if (amount == null || amount.isEmpty()) {
            return false;
        }

        try {
            String cleanAmount = amount.replace("đ", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .trim();

            Double.parseDouble(cleanAmount);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Định dạng số lớn thành dạng rút gọn
    public static String formatCompactNumber(double number) {
        try {
            if (number >= 1_000_000_000_000L) {
                return formatNumber(number / 1_000_000_000_000L) + " nghìn tỷ";
            } else if (number >= 1_000_000_000L) {
                return formatNumber(number / 1_000_000_000L) + " tỷ";
            } else if (number >= 1_000_000L) {
                return formatNumber(number / 1_000_000L) + " triệu";
            } else if (number >= 1_000L) {
                return formatNumber(number / 1_000L) + " nghìn";
            } else {
                return formatNumber(number);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting compact number: " + number, e);
            return String.valueOf(number);
        }
    }
}
