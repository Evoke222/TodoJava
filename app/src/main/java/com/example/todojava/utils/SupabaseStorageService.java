package com.example.todojava.utils;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SupabaseStorageService {
    @POST("storage/v1/object/{bucket}/{filename}")
    Call<ResponseBody> uploadFile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Header("Content-Type") String contentType,
            @Header("x-upsert") String upsert,
            @Path("bucket") String bucket,
            @Path("filename") String filename,
            @Body RequestBody file
    );
}
