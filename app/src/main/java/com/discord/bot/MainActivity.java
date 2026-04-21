package com.discord.bot;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String API_BASE = "https://discord.com/api/v10";
    private static final String PREFS_NAME = "discord_bot_prefs";

    private OkHttpClient client = new OkHttpClient();
    private String botToken = "";
    private SharedPreferences prefs;

    private LinearLayout loginLayout, mainLayout, serversLayout, dmsLayout;
    private EditText tokenInput;
    private TextView statusText;

    private RecyclerView serverListView, dmListView;
    private ServerAdapter serverAdapter, dmAdapter;

    private List<ServerItem> servers = new ArrayList<>();
    private List<ServerItem> dms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loginLayout = findViewById(R.id.loginLayout);
        mainLayout = findViewById(R.id.mainLayout);
        serversLayout = findViewById(R.id.serversLayout);
        dmsLayout = findViewById(R.id.dmsLayout);

        tokenInput = findViewById(R.id.tokenInput);
        statusText = findViewById(R.id.statusText);

        serverListView = findViewById(R.id.serverListView);
        dmListView = findViewById(R.id.dmListView);

        serverAdapter = new ServerAdapter(servers);
        dmAdapter = new ServerAdapter(dms);

        serverListView.setLayoutManager(new LinearLayoutManager(this));
        dmListView.setLayoutManager(new LinearLayoutManager(this));

        serverListView.setAdapter(serverAdapter);
        dmListView.setAdapter(dmAdapter);

        String savedToken = prefs.getString("bot_token", "");
        if (!savedToken.isEmpty()) {
            botToken = savedToken;
            showMainLayout();
            loadData();
        }
    }

    public void onLoginClick(View v) {
        String token = tokenInput.getText().toString().trim();

        if (token.isEmpty()) {
            Toast.makeText(this, "Enter bot token", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();

        statusText.setText("Logging in...");
        botToken = token;

        prefs.edit().putString("bot_token", token).apply();
        verifyToken(token);
    }

    private void verifyToken(String token) {
        Request request = new Request.Builder()
                .url(API_BASE + "/users/@me")
                .addHeader("Authorization", "Bot " + token.replace("Bot ", ""))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        statusText.setText("Connection failed")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showMainLayout();
                        loadData();
                    });
                } else {
                    runOnUiThread(() -> {
                        statusText.setText("Invalid token");
                        prefs.edit().remove("bot_token").apply();
                    });
                }
            }
        });
    }

    private void loadData() {
        statusText.setText("Loading...");
        loadGuilds();
        loadDMs();
    }

    private void loadGuilds() {
        Request request = new Request.Builder()
                .url(API_BASE + "/users/@me/guilds")
                .addHeader("Authorization", "Bot " + botToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> statusText.setText("Failed to load servers"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray arr = new JSONArray(response.body().string());

                    servers.clear();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject g = arr.getJSONObject(i);

                        servers.add(new ServerItem(
                                g.getString("id"),
                                g.optString("name", "Unknown Server"),
                                g.optString("icon"),
                                "GUILD"
                        ));
                    }

                    runOnUiThread(() -> {
                        statusText.setText(servers.size() + " servers");
                        serverAdapter.notifyDataSetChanged();
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> statusText.setText("Error loading servers"));
                }
            }
        });
    }

    private void loadDMs() {
        Request request = new Request.Builder()
                .url(API_BASE + "/users/@me/channels")
                .addHeader("Authorization", "Bot " + botToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> statusText.setText("Failed to load DMs"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray arr = new JSONArray(response.body().string());

                    dms.clear();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject g = arr.getJSONObject(i);

                        String name = "DM";

                        if (g.has("recipients")) {
                            JSONArray rcps = g.getJSONArray("recipients");
                            if (rcps.length() > 0) {
                                JSONObject user = rcps.getJSONObject(0);
                                name = user.optString("username", "DM");
                            }
                        }

                        dms.add(new ServerItem(
                                g.getString("id"),
                                name,
                                "",
                                "DM"
                        ));
                    }

                    runOnUiThread(() -> dmAdapter.notifyDataSetChanged());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onLogoutClick(View v) {
        prefs.edit().remove("bot_token").apply();
        botToken = "";

        loginLayout.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.GONE);
    }

    private void showMainLayout() {
        loginLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
