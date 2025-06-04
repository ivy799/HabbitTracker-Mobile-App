package com.example.habbittracker.Api_config;

import retrofit2.Retrofit;

public class RetrofitClient {
    private static final String BASE_URL = "https://zenquotes.io/api/";
    private static Retrofit retrofit;

    public static ApiService getQuoteApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
