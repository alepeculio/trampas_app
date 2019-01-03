package com.trampas.trampas.BD;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BDCliente {
    public static final String BASE_URL = "http://192.168.0.107/trampas/public/";
    //public static final String BASE_URL = "http://trapps.esy.es/servidor-trampas/public/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
