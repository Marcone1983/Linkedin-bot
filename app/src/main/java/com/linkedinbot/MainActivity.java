package com.linkedinbot;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText searchKeywordEditText;
    private Button startBotButton;
    private Button stopBotButton;
    private TextView statusTextView;
    private TextView followedCountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchKeywordEditText = findViewById(R.id.search_keyword_edit_text);
        startBotButton = findViewById(R.id.start_bot_button);
        stopBotButton = findViewById(R.id.stop_bot_button);
        statusTextView = findViewById(R.id.status_text_view);
        followedCountTextView = findViewById(R.id.followed_count_text_view);

        startBotButton.setOnClickListener(v -> startBot());
        stopBotButton.setOnClickListener(v -> stopBot());

        updateUIState(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccessibilityServiceStatus();
    }

    private void checkAccessibilityServiceStatus() {
        if (!isAccessibilityServiceEnabled()) {
            statusTextView.setText("Servizio di Accessibilità NON abilitato. Clicca per abilitare.");
            statusTextView.setOnClickListener(v -> openAccessibilitySettings());
            startBotButton.setEnabled(false);
        } else {
            statusTextView.setText("Servizio di Accessibilità abilitato.");
            statusTextView.setOnClickListener(null);
            startBotButton.setEnabled(true);
        }
    }

    private void startBot() {
        String keyword = searchKeywordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "Inserisci una parola chiave per la ricerca!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Invia la keyword al servizio di accessibilità e avvia il bot
        Intent intent = new Intent(this, LinkedInAccessibilityService.class);
        intent.setAction(LinkedInAccessibilityService.ACTION_START_BOT);
        intent.putExtra(LinkedInAccessibilityService.EXTRA_KEYWORD, keyword);
        startService(intent);

        updateUIState(true);
        Toast.makeText(this, "Bot avviato per: " + keyword, Toast.LENGTH_SHORT).show();
    }

    private void stopBot() {
        // Invia un comando per fermare il bot al servizio di accessibilità
        Intent intent = new Intent(this, LinkedInAccessibilityService.class);
        intent.setAction(LinkedInAccessibilityService.ACTION_STOP_BOT);
        startService(intent);

        updateUIState(false);
        Toast.makeText(this, "Bot fermato.", Toast.LENGTH_SHORT).show();
    }

    private void updateUIState(boolean botRunning) {
        searchKeywordEditText.setEnabled(!botRunning);
        startBotButton.setEnabled(!botRunning && isAccessibilityServiceEnabled());
        stopBotButton.setEnabled(botRunning);
        if (!botRunning) {
            // Reset status and count only if not waiting for accessibility service
            if (isAccessibilityServiceEnabled()) {
                statusTextView.setText("In attesa...");
            }
            followedCountTextView.setText("Follow eseguiti: 0");
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().equals(getPackageName() + "/" + LinkedInAccessibilityService.class.getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
}


