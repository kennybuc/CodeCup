package com.app.flexivendsymbol.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface APIService {

    @FormUrlEncoded
    @POST("RegisterNumber")
    Call<ResponseBody> registerNumber(@Field("APIKey") String apiKey, @Field("CUPNO") String number);

}
