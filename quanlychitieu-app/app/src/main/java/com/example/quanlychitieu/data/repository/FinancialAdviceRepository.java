package com.example.quanlychitieu.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.quanlychitieu.data.api.ApiClient;
import com.example.quanlychitieu.data.api.FinancialAdviceService;
import com.example.quanlychitieu.data.model.FinancialAdviceRequest;
import com.example.quanlychitieu.data.model.FinancialAdviceResponse;
import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FinancialAdviceRepository {
    private static final String TAG = "FinancialAdviceRepo";
    private final FinancialAdviceService apiService;
    private final FirebaseAuth auth;
    private static FinancialAdviceRepository instance;

    public static synchronized FinancialAdviceRepository getInstance() {
        if (instance == null) {
            instance = new FinancialAdviceRepository();
        }
        return instance;
    }

    private FinancialAdviceRepository() {
        apiService = ApiClient.getClient().create(FinancialAdviceService.class);
        auth = FirebaseAuth.getInstance();
    }

    public LiveData<FinancialAdviceResponse> getFinancialAdvice(String message) {
        MutableLiveData<FinancialAdviceResponse> adviceLiveData = new MutableLiveData<>();
        
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        FinancialAdviceRequest request = new FinancialAdviceRequest(userId, message);
        
        apiService.getFinancialAdvice(request).enqueue(new Callback<FinancialAdviceResponse>() {
            @Override
            public void onResponse(Call<FinancialAdviceResponse> call, Response<FinancialAdviceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adviceLiveData.setValue(response.body());
                } else {
                    Log.e(TAG, "API error: " + (response.errorBody() != null ? response.errorBody().toString() : "Unknown error"));
                    FinancialAdviceResponse errorResponse = new FinancialAdviceResponse();
                    errorResponse.setResponse("Không thể kết nối đến dịch vụ tư vấn tài chính. Vui lòng thử lại sau.");
                    adviceLiveData.setValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<FinancialAdviceResponse> call, Throwable t) {
                Log.e(TAG, "API failure", t);
                FinancialAdviceResponse errorResponse = new FinancialAdviceResponse();
                errorResponse.setResponse("Không thể kết nối đến dịch vụ tư vấn tài chính. Vui lòng thử lại sau.");
                adviceLiveData.setValue(errorResponse);
            }
        });
        
        return adviceLiveData;
    }
} 