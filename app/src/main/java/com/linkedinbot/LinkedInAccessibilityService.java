package com.linkedinbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class LinkedInAccessibilityService extends AccessibilityService {

    private static final String TAG = "LinkedInBotService";
    public static final String ACTION_START_BOT = "com.linkedinbot.ACTION_START_BOT";
    public static final String ACTION_STOP_BOT = "com.linkedinbot.ACTION_STOP_BOT";
    public static final String EXTRA_KEYWORD = "com.linkedinbot.EXTRA_KEYWORD";

    private String searchKeyword;
    private boolean botRunning = false;
    private int followedCount = 0;
    private Set<String> processedElements = new HashSet<>();
    private Random random = new Random();
    private Handler handler = new Handler(Looper.getMainLooper());

    private static LinkedInAccessibilityService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.packageNames = new String[]{"com.linkedin.android"}; // Limita al pacchetto LinkedIn
        info.notificationTimeout = 100; // Millisecondi
        this.setServiceInfo(info);
        logStatus("Servizio di Accessibilità LinkedIn Bot connesso.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_BOT.equals(action)) {
                searchKeyword = intent.getStringExtra(EXTRA_KEYWORD);
                startBot();
            } else if (ACTION_STOP_BOT.equals(action)) {
                stopBot();
            }
        }
        return START_STICKY;
    }

    private void startBot() {
        if (botRunning) return;
        botRunning = true;
        followedCount = 0;
        processedElements.clear();
        logStatus("Bot avviato per la ricerca: " + searchKeyword);
        openLinkedInApp();
    }

    private void stopBot() {
        if (!botRunning) return;
        botRunning = false;
        logStatus("Bot fermato. Follow eseguiti: " + followedCount);
        Toast.makeText(this, "Bot LinkedIn fermato.", Toast.LENGTH_SHORT).show();
    }

    private void openLinkedInApp() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.linkedin.android");
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            logStatus("Apertura app LinkedIn...");
        } else {
            logStatus("App LinkedIn non trovata.");
            Toast.makeText(this, "App LinkedIn non installata!", Toast.LENGTH_LONG).show();
            stopBot();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!botRunning || event.getSource() == null) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";

        if (packageName.equals("com.linkedin.android")) {
            handler.postDelayed(() -> performLinkedInActions(rootNode), random.nextInt(500) + 500); // Piccola pausa casuale
        }
    }

    private void performLinkedInActions(AccessibilityNodeInfo rootNode) {
        if (!botRunning) return;

        // 1. Cerca la barra di ricerca (elemento con testo 


"'Cerca'")
        List<AccessibilityNodeInfo> searchBars = rootNode.findAccessibilityNodeInfosByText("Cerca");
        if (!searchBars.isEmpty()) {
            for (AccessibilityNodeInfo node : searchBars) {
                if (node.getClassName().equals("android.widget.TextView") && node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    logStatus("Cliccato sulla barra di ricerca.");
                    sleep(2000, 4000);
                    return;
                }
            }
        }

        // 2. Inserisci la keyword e premi invio
        List<AccessibilityNodeInfo> searchInputs = rootNode.findAccessibilityNodeInfosByViewId("com.linkedin.android:id/search_bar_text");
        if (!searchInputs.isEmpty()) {
            AccessibilityNodeInfo searchInput = searchInputs.get(0);
            if (searchInput.isEditable()) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchKeyword);
                searchInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                logStatus("Inserita keyword: " + searchKeyword);
                sleep(1000, 2000);
                // Simula il tasto Invio cercando un pulsante di ricerca o un'azione implicita
                // In molti casi, l'input stesso triggera la ricerca o appare un pulsante 


di conferma.
                // Per ora, cerchiamo un pulsante di ricerca esplicito se disponibile.
                List<AccessibilityNodeInfo> searchConfirmButtons = rootNode.findAccessibilityNodeInfosByText("Cerca");
                if (!searchConfirmButtons.isEmpty()) {
                    for (AccessibilityNodeInfo node : searchConfirmButtons) {
                        if (node.isClickable()) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            logStatus("Cliccato pulsante di conferma ricerca.");
                            sleep(2000, 4000);
                            return;
                        }
                    }
                }
                // Se non troviamo un pulsante esplicito, assumiamo che l'input abbia già avviato la ricerca.
                sleep(4000, 7000); // Attendi i risultati
                return;
            }
        }

        // 3. Filtra per "Aziende"
        List<AccessibilityNodeInfo> companyFilterButtons = rootNode.findAccessibilityNodeInfosByText("Aziende");
        if (!companyFilterButtons.isEmpty()) {
            for (AccessibilityNodeInfo node : companyFilterButtons) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    logStatus("Filtro applicato: Aziende.");
                    sleep(3000, 5000);
                    return;
                }
            }
        }

        // 4. Ciclo di 'Follow'
        List<AccessibilityNodeInfo> followButtons = rootNode.findAccessibilityNodeInfosByText("Segui");
        boolean performedAction = false;
        if (!followButtons.isEmpty()) {
            for (AccessibilityNodeInfo button : followButtons) {
                // Usiamo una combinazione di testo e posizione per identificare in modo univoco i pulsanti
                String elementIdentifier = button.getText() + "_" + button.getBoundsInScreen().toString();

                if (button.isClickable() && !processedElements.contains(elementIdentifier)) {
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    followedCount++;
                    processedElements.add(elementIdentifier);
                    logStatus("Follow eseguiti: " + followedCount);
                    sleep(1000, 3000); // Pausa casuale tra i follow
                    performedAction = true;

                    if (followedCount >= 150) { // Limite di sicurezza
                        logStatus("Raggiunto limite massimo di follow per sessione.");
                        stopBot();
                        return;
                    }
                }
            }
        }

        // Se non abbiamo cliccato nessun pulsante 'Segui' o se la lista è finita, scrolla
        if (!performedAction || followButtons.isEmpty()) {
            if (followedCount < 150) { // Continua a scrollare solo se non abbiamo raggiunto il limite
                performScroll(rootNode);
                sleep(2000, 5000); // Pausa dopo lo scroll per caricare nuovi elementi
            } else {
                logStatus("Ciclo di follow completato o limite raggiunto.");
                stopBot();
            }
        }
    }

    private void performScroll(AccessibilityNodeInfo rootNode) {
        // Simula uno scroll verso il basso
        // Questo è un metodo generico, potrebbe essere necessario affinarlo per LinkedIn
        rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        logStatus("Scrollo la pagina...");
    }

    private void sleep(int minMillis, int maxMillis) {
        try {
            Thread.sleep(random.nextInt(maxMillis - minMillis) + minMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logStatus("Sleep interrotto.");
        }
    }

    private void logStatus(String message) {
        Log.d(TAG, message);
        // Potresti voler inviare questo stato alla MainActivity per aggiornare la UI
        // Esempio: inviare un broadcast o usare un EventBus
        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onInterrupt() {
        logStatus("Servizio di Accessibilità interrotto.");
        stopBot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        logStatus("Servizio di Accessibilità distrutto.");
    }

    // Metodo per ottenere l'istanza del servizio (se necessario per comunicare dalla MainActivity)
    public static LinkedInAccessibilityService getInstance() {
        return instance;
    }

    // Metodo per aggiornare il contatore dei follow (se necessario dalla MainActivity)
    public int getFollowedCount() {
        return followedCount;
    }
}


