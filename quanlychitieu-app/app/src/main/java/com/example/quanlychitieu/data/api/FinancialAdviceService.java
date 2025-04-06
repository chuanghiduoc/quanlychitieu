package com.example.quanlychitieu.data.api;

import com.example.quanlychitieu.data.model.FinancialAdviceRequest;
import com.example.quanlychitieu.data.model.FinancialAdviceResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FinancialAdviceService {
    @POST("api/ai")
    Call<FinancialAdviceResponse> getFinancialAdvice(@Body FinancialAdviceRequest request);
} 