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

import com.example.quanlychitieu.MainActivity;
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
    private String currentPeriodType = "month";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        // Định dạng tiền tệ
        currencyFormat.setMaximumFractionDigits(0);

        // Thiết lập bộ chọn khoảng thời gian
        setupPeriodSelector();

        // Thiết lập biểu đồ
        setupPieChart();
        setupBarChart();

        // Thiết lập danh sách danh mục
        setupCategoryList();

        // Thiết lập nút xuất báo cáo
        binding.exportReportButton.setOnClickListener(v -> exportReport());

        // Tải dữ liệu ban đầu
        updatePeriodDisplay();
        loadData();
    }

    private void setupPeriodSelector() {
        // Thiết lập radio group để chọn khoảng thời gian
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

        // Thiết lập các nút điều hướng
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
                // Lấy ngày đầu tiên của tuần
                Calendar first = (Calendar) currentPeriod.clone();
                first.set(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek());
                // Lấy ngày cuối cùng của tuần
                Calendar last = (Calendar) first.clone();
                last.add(Calendar.DAY_OF_WEEK, 6);

                // Định dạng ngày tháng
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

        // Vô hiệu hóa nút tiếp theo nếu khoảng thời gian hiện tại là thời gian hiện tại
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
        // Khởi tạo biểu đồ tròn
        pieChart = new PieChart(requireContext());
        binding.pieChartContainer.addView(pieChart);

        // Thiết lập giao diện biểu đồ
        ChartHelper.setupPieChart(pieChart);
        pieChart.setNoDataText("Không có dữ liệu chi tiêu trong khoảng thời gian này");
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);

        // Cấu hình chú giải
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
        // Khởi tạo biểu đồ cột
        barChart = new BarChart(requireContext());
        binding.barChartContainer.addView(barChart);

        // Thiết lập giao diện biểu đồ
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.setNoDataText("Không có dữ liệu chi tiêu trong khoảng thời gian này");

        // Cấu hình trục X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);

        // Cấu hình trục Y
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setSpaceTop(35f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatShortCurrency(value);
            }
        });

        barChart.getAxisRight().setEnabled(false);

        // Cấu hình chú giải
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setXEntrySpace(20f);
        barChart.setExtraBottomOffset(15f);
    }

    private void setupCategoryList() {
        // Tạo RecyclerView cho các danh mục
        RecyclerView categoryRecyclerView = new RecyclerView(requireContext());
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryStatisticsAdapter(requireContext());
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Thêm vào container
        binding.categoriesList.addView(categoryRecyclerView);
    }

    private void loadData() {
        // Lấy khoảng ngày cho khoảng thời gian đã chọn
        Pair<Date, Date> dateRange = getDateRangeForPeriod();
        Date startDate = dateRange.first;
        Date endDate = dateRange.second;

        // Tải dữ liệu tài chính cho khoảng thời gian
        viewModel.loadFinancialData(startDate, endDate);

        // Theo dõi dữ liệu tài chính
        viewModel.getIncome().observe(getViewLifecycleOwner(), income -> {
            binding.incomeAmount.setText(formatCurrency(income));
        });

        viewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            binding.expenseAmount.setText(formatCurrency(expenses));
        });

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            binding.balanceAmount.setText(formatCurrency(balance));

            // Đặt màu dựa trên số dư
            if (balance < 0) {
                binding.balanceAmount.setTextColor(getResources().getColor(R.color.expense_red, null));
            } else {
                binding.balanceAmount.setTextColor(getResources().getColor(R.color.income_green, null));
            }
        });

        // Tải dữ liệu biểu đồ
        viewModel.getCategoryExpenses().observe(getViewLifecycleOwner(), categoryExpenses -> {
            updatePieChart(categoryExpenses);
            categoryAdapter.updateData(categoryExpenses);
        });

        // Tải dữ liệu chuỗi thời gian cho biểu đồ cột
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

        // Tạo các mục nhập thu nhập
        List<BarEntry> incomeEntries = new ArrayList<>();
        for (int i = 0; i < incomeValues.size(); i++) {
            incomeEntries.add(new BarEntry(i, incomeValues.get(i)));
        }

        // Tạo các mục nhập chi tiêu
        List<BarEntry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < expenseValues.size(); i++) {
            expenseEntries.add(new BarEntry(i, expenseValues.get(i)));
        }

        // Tạo các tập dữ liệu
        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, "Thu nhập");
        incomeDataSet.setColor(getResources().getColor(R.color.income_green, null));
        incomeDataSet.setValueTextSize(10f);
        incomeDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return formatShortCurrency(value);
            }
        });

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseDataSet.setColor(getResources().getColor(R.color.expense_red, null));
        expenseDataSet.setValueTextSize(10f);
        expenseDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return formatShortCurrency(value);
            }
        });

        // Tạo dữ liệu biểu đồ cột
        BarData barData = new BarData(incomeDataSet, expenseDataSet);
        barData.setBarWidth(0.4f);

        // Đặt nhãn trục x
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        // Nhóm các cột
        float groupSpace = 0.3f;
        float barSpace = 0f;
        barData.groupBars(0, groupSpace, barSpace);

        // Đặt dữ liệu và cập nhật
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
                // Đặt thành ngày đầu tiên của tuần
                start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                // Đặt thành ngày cuối cùng của tuần
                end.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek() + 6);
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                end.set(Calendar.MILLISECOND, 999);
                break;

            case "month":
                // Đặt thành ngày đầu tiên của tháng
                start.set(Calendar.DAY_OF_MONTH, 1);
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                // Đặt thành ngày cuối cùng của tháng
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                end.set(Calendar.MILLISECOND, 999);
                break;

            case "year":
                // Đặt thành ngày đầu tiên của năm
                start.set(Calendar.DAY_OF_YEAR, 1);
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                // Đặt thành ngày cuối cùng của năm
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
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (!mainActivity.hasStoragePermission()) {
                Toast.makeText(requireContext(),
                        "Cần quyền lưu trữ để xuất báo cáo", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            // Lấy chuỗi khoảng thời gian cho tên tệp
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

            // Tạo nội dung báo cáo
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

            // Tạo tệp
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "finance_report_" + periodStr + ".txt";
            File file = new File(path, fileName);

            // Ghi vào tệp
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(reportContent.toString().getBytes());
            stream.close();

            // Chia sẻ tệp
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Lớp trợ giúp cho Pair vì chúng ta có thể không có androidx.core.util.Pair
    private static class Pair<F, S> {
        public final F first;
        public final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }
    private String formatShortCurrency(float value) {
        // Định dạng số tiền ngắn gọn
        if (value >= 1_000_000_000) {
            return String.format("%.1fB", value / 1_000_000_000); // Tỷ
        } else if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000); // Triệu
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000); // Ngàn
        } else {
            return String.format("%.0f", value); // Đồng (hoặc đơn vị cơ bản)
        }
    }
}