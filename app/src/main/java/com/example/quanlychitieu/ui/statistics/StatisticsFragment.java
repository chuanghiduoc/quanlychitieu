package com.example.quanlychitieu.ui.statistics;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.CategoryStatisticsAdapter;
import com.example.quanlychitieu.databinding.FragmentStatisticsBinding;
import com.example.quanlychitieu.ui.dashboard.ChartHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";
    private FragmentStatisticsBinding binding;
    private StatisticsViewModel viewModel;
    private PieChart pieChart;
    private BarChart barChart;
    private CategoryStatisticsAdapter categoryAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private Calendar currentPeriod = Calendar.getInstance();
    private String currentPeriodType = "month"; // Default period type

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        // Format currency
        currencyFormat.setMaximumFractionDigits(0);

        // Setup period selector
        setupPeriodSelector();

        // Setup charts
        setupPieChart();
        setupBarChart();

        // Setup category list
        setupCategoryList();

        // Setup export button
        binding.exportReportButton.setOnClickListener(v -> exportReport());

        // Load initial data
        updatePeriodDisplay();
        loadData();
    }

    private void setupPeriodSelector() {
        // Set up radio group for period selection
        binding.timePeriodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.period_week) {
                currentPeriodType = "week";
            } else if (checkedId == R.id.period_month) {
                currentPeriodType = "month";
            } else if (checkedId == R.id.period_year) {
                currentPeriodType = "year";
            }
            updatePeriodDisplay();
            loadData();
        });

        // Set up navigation buttons
        binding.previousPeriodButton.setOnClickListener(v -> {
            navigatePeriod(-1);
        });

        binding.nextPeriodButton.setOnClickListener(v -> {
            navigatePeriod(1);
        });
    }

    private void navigatePeriod(int direction) {
        switch (currentPeriodType) {
            case "week":
                currentPeriod.add(Calendar.WEEK_OF_YEAR, direction);
                break;
            case "month":
                currentPeriod.add(Calendar.MONTH, direction);
                break;
            case "year":
                currentPeriod.add(Calendar.YEAR, direction);
                break;
        }
        updatePeriodDisplay();
        loadData();
    }

    private void updatePeriodDisplay() {
        String periodText;
        SimpleDateFormat formatter;

        switch (currentPeriodType) {
            case "week":
                // Get first day of week
                Calendar first = (Calendar) currentPeriod.clone();
                first.set(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek());
                // Get last day of week
                Calendar last = (Calendar) first.clone();
                last.add(Calendar.DAY_OF_WEEK, 6);

                // Format dates
                formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                periodText = String.format("Tuần %d: %s - %s",
                        currentPeriod.get(Calendar.WEEK_OF_YEAR),
                        formatter.format(first.getTime()),
                        formatter.format(last.getTime()));
                break;
            case "month":
                formatter = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                periodText = "Tháng " + formatter.format(currentPeriod.getTime());
                break;
            case "year":
                formatter = new SimpleDateFormat("yyyy", Locale.getDefault());
                periodText = "Năm " + formatter.format(currentPeriod.getTime());
                break;
            default:
                periodText = "Không xác định";
                break;
        }

        binding.currentPeriodText.setText(periodText);

        // Disable next button if current period is current time
        Calendar now = Calendar.getInstance();
        boolean isCurrentPeriod = false;

        switch (currentPeriodType) {
            case "week":
                isCurrentPeriod = now.get(Calendar.YEAR) == currentPeriod.get(Calendar.YEAR) &&
                        now.get(Calendar.WEEK_OF_YEAR) == currentPeriod.get(Calendar.WEEK_OF_YEAR);
                break;
            case "month":
                isCurrentPeriod = now.get(Calendar.YEAR) == currentPeriod.get(Calendar.YEAR) &&
                        now.get(Calendar.MONTH) == currentPeriod.get(Calendar.MONTH);
                break;
            case "year":
                isCurrentPeriod = now.get(Calendar.YEAR) == currentPeriod.get(Calendar.YEAR);
                break;
        }

        binding.nextPeriodButton.setEnabled(!isCurrentPeriod);
    }

    private void setupPieChart() {
        // Initialize pie chart
        pieChart = new PieChart(requireContext());
        binding.pieChartContainer.addView(pieChart);

        // Setup chart appearance
        ChartHelper.setupPieChart(pieChart);
        pieChart.setNoDataText("Không có dữ liệu chi tiêu trong khoảng thời gian này");
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);

        // Configure legend
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setWordWrapEnabled(true);
    }

    private void setupBarChart() {
        // Initialize bar chart
        barChart = new BarChart(requireContext());
        binding.barChartContainer.addView(barChart);

        // Setup chart appearance
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.setNoDataText("Không có dữ liệu chi tiêu trong khoảng thời gian này");

        // Configure X axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);

        // Configure Y axis
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setSpaceTop(35f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatCurrency(value);
            }
        });

        barChart.getAxisRight().setEnabled(false);

        // Configure legend
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
    }

    private void setupCategoryList() {
        // Create RecyclerView for categories
        RecyclerView categoryRecyclerView = new RecyclerView(requireContext());
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryStatisticsAdapter(requireContext());
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Add to the container
        binding.categoriesList.addView(categoryRecyclerView);
    }

    private void loadData() {
        // Get date range for selected period
        Pair<Date, Date> dateRange = getDateRangeForPeriod();
        Date startDate = dateRange.first;
        Date endDate = dateRange.second;

        // Load financial data for the period
        viewModel.loadFinancialData(startDate, endDate);

        // Observe financial data
        viewModel.getIncome().observe(getViewLifecycleOwner(), income -> {
            binding.incomeAmount.setText(formatCurrency(income));
        });

        viewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            binding.expenseAmount.setText(formatCurrency(expenses));
        });

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            binding.balanceAmount.setText(formatCurrency(balance));

            // Set color based on balance
            if (balance < 0) {
                binding.balanceAmount.setTextColor(getResources().getColor(R.color.expense_red, null));
            } else {
                binding.balanceAmount.setTextColor(getResources().getColor(R.color.income_green, null));
            }
        });

        // Load chart data
        viewModel.getCategoryExpenses().observe(getViewLifecycleOwner(), categoryExpenses -> {
            updatePieChart(categoryExpenses);
            categoryAdapter.updateData(categoryExpenses);
        });

        // Load time series data for bar chart
        viewModel.getTimeSeriesData().observe(getViewLifecycleOwner(), timeSeriesData -> {
            updateBarChart(timeSeriesData);
        });
    }

    private void updatePieChart(Map<String, Double> categoryExpenses) {
        if (categoryExpenses.isEmpty()) {
            pieChart.setData(null);
            pieChart.invalidate();
            return;
        }

        ChartHelper.updateExpenseChart(pieChart, categoryExpenses, requireContext());
    }

    private void updateBarChart(TimeSeriesData timeSeriesData) {
        if (timeSeriesData.getLabels().isEmpty()) {
            barChart.setData(null);
            barChart.invalidate();
            return;
        }

        List<String> labels = timeSeriesData.getLabels();
        List<Float> incomeValues = timeSeriesData.getIncomeValues();
        List<Float> expenseValues = timeSeriesData.getExpenseValues();

        // Create income entries
        List<BarEntry> incomeEntries = new ArrayList<>();
        for (int i = 0; i < incomeValues.size(); i++) {
            incomeEntries.add(new BarEntry(i, incomeValues.get(i)));
        }

        // Create expense entries
        List<BarEntry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < expenseValues.size(); i++) {
            expenseEntries.add(new BarEntry(i, expenseValues.get(i)));
        }

        // Create datasets
        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, "Thu nhập");
        incomeDataSet.setColor(getResources().getColor(R.color.income_green, null));
        incomeDataSet.setValueTextSize(10f);
        incomeDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return formatCurrency(value);
            }
        });

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseDataSet.setColor(getResources().getColor(R.color.expense_red, null));
        expenseDataSet.setValueTextSize(10f);
        expenseDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return formatCurrency(value);
            }
        });

        // Create bar data
        BarData barData = new BarData(incomeDataSet, expenseDataSet);
        barData.setBarWidth(0.4f);

        // Set x-axis labels
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        // Group bars
        float groupSpace = 0.2f;
        float barSpace = 0f;
        barData.groupBars(0, groupSpace, barSpace);

        // Set data and update
        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private Pair<Date, Date> getDateRangeForPeriod() {
        Calendar start = (Calendar) currentPeriod.clone();
        Calendar end = (Calendar) currentPeriod.clone();

        switch (currentPeriodType) {
            case "week":
                // Set to first day of week
                start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                // Set to last day of week
                end.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek() + 6);
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                end.set(Calendar.MILLISECOND, 999);
                break;

            case "month":
                // Set to first day of month
                start.set(Calendar.DAY_OF_MONTH, 1);
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                // Set to last day of month
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                end.set(Calendar.MILLISECOND, 999);
                break;

            case "year":
                // Set to first day of year
                start.set(Calendar.DAY_OF_YEAR, 1);
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                // Set to last day of year
                end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR));
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                end.set(Calendar.MILLISECOND, 999);
                break;
        }

        return new Pair<>(start.getTime(), end.getTime());
    }

    private void exportReport() {
        try {
            // Get period string for filename
            String periodStr;
            SimpleDateFormat formatter;

            switch (currentPeriodType) {
                case "week":
                    formatter = new SimpleDateFormat("'Week'_w_yyyy", Locale.getDefault());
                    periodStr = formatter.format(currentPeriod.getTime());
                    break;
                case "month":
                    formatter = new SimpleDateFormat("MM_yyyy", Locale.getDefault());
                    periodStr = formatter.format(currentPeriod.getTime());
                    break;
                case "year":
                    formatter = new SimpleDateFormat("yyyy", Locale.getDefault());
                    periodStr = formatter.format(currentPeriod.getTime());
                    break;
                default:
                    periodStr = "custom_period";
                    break;
            }

            // Create report content
            StringBuilder reportContent = new StringBuilder();
            reportContent.append("BÁO CÁO TÀI CHÍNH\n\n");
            reportContent.append(binding.currentPeriodText.getText()).append("\n\n");
            reportContent.append("Thu nhập: ").append(binding.incomeAmount.getText()).append("\n");
            reportContent.append("Chi tiêu: ").append(binding.expenseAmount.getText()).append("\n");
            reportContent.append("Số dư: ").append(binding.balanceAmount.getText()).append("\n\n");

            reportContent.append("CHI TIẾT CHI TIÊU THEO DANH MỤC:\n");
            Map<String, Double> categoryExpenses = viewModel.getCategoryExpenses().getValue();
            if (categoryExpenses != null) {
                for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                    reportContent.append(entry.getKey()).append(": ")
                            .append(formatCurrency(entry.getValue())).append("\n");
                }
            }

            // Create file
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "finance_report_" + periodStr + ".txt";
            File file = new File(path, fileName);

            // Write to file
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(reportContent.toString().getBytes());
            stream.close();

            // Share file
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo tài chính"));

            Toast.makeText(requireContext(),
                    "Báo cáo đã được lưu vào " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error exporting report", e);
            Toast.makeText(requireContext(),
                    "Không thể xuất báo cáo: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String formatCurrency(double amount) {
        return currencyFormat.format(amount)
                .replace("₫", "đ")
                .replace(",", ".");
    }

    private String formatCurrency(float amount) {
        return formatCurrency((double) amount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Helper class for Pair since we might not have androidx.core.util.Pair
    private static class Pair<F, S> {
        public final F first;
        public final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }
}