package com.example.quanlychitieu.ui.dashboard;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.CategoryManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartHelper {

    /**
     * Lấy màu từ resources
     */
    public static int[] getChartColors(Context context) {
        return new int[] {
                ContextCompat.getColor(context, R.color.chart_1),
                ContextCompat.getColor(context, R.color.chart_2),
                ContextCompat.getColor(context, R.color.chart_3),
                ContextCompat.getColor(context, R.color.chart_4),
                ContextCompat.getColor(context, R.color.chart_5),
                ContextCompat.getColor(context, R.color.chart_6),
                ContextCompat.getColor(context, R.color.chart_7),
                ContextCompat.getColor(context, R.color.chart_8)
        };
    }

    /**
     * Cấu hình và thiết lập biểu đồ tròn
     */
    public static void setupPieChart(PieChart chart) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
//        chart.setExtraOffsets(2, 2, 2, 2);

        chart.setDragDecelerationFrictionCoef(0.95f);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);
        chart.setHoleRadius(0f);
        chart.setTransparentCircleRadius(5f);
        chart.setDrawCenterText(false);
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        Legend legend = chart.getLegend();
        legend.setEnabled(false); // Ẩn chú thích

        chart.setEntryLabelColor(Color.WHITE);
        chart.setEntryLabelTextSize(12f);

        chart.setDrawEntryLabels(false);
    }


    /**
     * Cập nhật dữ liệu cho biểu đồ chi tiêu
     */
    public static void updateExpenseChart(PieChart chart, Map<String, Double> categoryExpenses, Context context) {
        if (chart == null || categoryExpenses == null || categoryExpenses.isEmpty()) {
            chart.setNoDataText("Không có dữ liệu chi tiêu");
            chart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();

        // Đảm bảo thứ tự nhất quán khi thêm dữ liệu vào biểu đồ
        List<String> categories = new ArrayList<>(categoryExpenses.keySet());
        // Sắp xếp danh mục để đảm bảo thứ tự nhất quán
        Collections.sort(categories);

        for (String category : categories) {
            double value = categoryExpenses.get(category);
            if (value > 0) {
                entries.add(new PieEntry((float) value, category));
            }
        }

        if (entries.isEmpty()) {
            chart.setNoDataText("Không có dữ liệu chi tiêu");
            chart.invalidate();
            return;
        }
        Map<String, Integer> categoryColorMap = createCategoryColorMap(context);

        List<Integer> colorList = new ArrayList<>();
        for (PieEntry entry : entries) {
            String category = entry.getLabel();
            int color = categoryColorMap.getOrDefault(category, ContextCompat.getColor(context, R.color.chart_1));
            colorList.add(color);
        }

        PieDataSet dataSet = new PieDataSet(entries, "Chi tiêu theo danh mục");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Lấy màu trực tiếp từ resources và gán cho dataSet
        dataSet.setColors(colorList);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(chart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        chart.setData(data);
        chart.highlightValues(null);
        chart.invalidate();
    }

    /**
     * Cập nhật dữ liệu cho biểu đồ ngân sách
     */
    public static void updateBudgetChart(PieChart chart, Map<String, Double> categoryBudgets, Context context) {
        if (chart == null || categoryBudgets == null || categoryBudgets.isEmpty()) {
            chart.setNoDataText("Không có dữ liệu ngân sách");
            chart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();

        // Thêm dữ liệu vào biểu đồ
        for (Map.Entry<String, Double> entry : categoryBudgets.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            chart.setNoDataText("Không có dữ liệu ngân sách");
            chart.invalidate();
            return;
        }

        Map<String, Integer> categoryColorMap = createCategoryColorMap(context);

        // Tạo mảng màu cho các mục trong biểu đồ, theo đúng thứ tự
        List<Integer> colorList = new ArrayList<>();
        for (PieEntry entry : entries) {
            String category = entry.getLabel();
            int color = categoryColorMap.getOrDefault(category, ContextCompat.getColor(context, R.color.chart_1));
            colorList.add(color);
        }

        PieDataSet dataSet = new PieDataSet(entries, "Ngân sách theo danh mục");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(colorList);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(chart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        chart.setData(data);
        chart.highlightValues(null);
        chart.invalidate();
    }

    public static Map<String, Integer> createCategoryColorMap(Context context) {
        // Lấy danh sách các danh mục chi tiêu
        List<String> categories = CategoryManager.getInstance().getExpenseCategories();
        // Lấy màu từ resources
        int[] colors = getChartColors(context);

        // Tạo map ánh xạ danh mục với màu
        Map<String, Integer> categoryColorMap = new HashMap<>();

        for (int i = 0; i < categories.size(); i++) {
            categoryColorMap.put(categories.get(i), colors[i % colors.length]);
        }

        return categoryColorMap;
    }
}
