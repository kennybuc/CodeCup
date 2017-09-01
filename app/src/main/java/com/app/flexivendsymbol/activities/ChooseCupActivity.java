package com.app.flexivendsymbol.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.app.flexivendsymbol.R;
import com.app.flexivendsymbol.api.APIClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class ChooseCupActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String KEY_SYMBOL_CODE = ChooseCupActivity.class.getSimpleName();

    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_cup);

        // Map view elements to class members.
        webView = (WebView) findViewById(R.id.webView);

        // Wrap event handlers to view elements.
        findViewById(R.id.btnNewScan).setOnClickListener(this);

        // Load web page to web view.
        loadWebContent(getIntent().getStringExtra(KEY_SYMBOL_CODE));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebContent(final String code) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                submitCode(code);
            }
        });

        String url = getString(R.string.url_choose_up);
        webView.loadUrl(url);
    }

    private void submitCode(String code) {
        String apiKey = getString(R.string.API_KEY);
        APIClient.getAPIService().registerNumber(apiKey, code).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onClick(View v) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
