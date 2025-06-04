package com.example.habbittracker.Api_config;

import com.example.habbittracker.Models.Quotes;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("random")
    Call<List<Quotes>> getRandomQuote();

}


