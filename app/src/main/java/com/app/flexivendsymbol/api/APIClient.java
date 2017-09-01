package com.app.flexivendsymbol.api;

import retrofit2.Retrofit;

public class APIClient {

    private static final String BASE_URL = "http://www.prizecup.eu/flexivend/codescanapi.asmx/";

    public static APIService getAPIService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build();
        return retrofit.create(APIService.class);
    }

}
