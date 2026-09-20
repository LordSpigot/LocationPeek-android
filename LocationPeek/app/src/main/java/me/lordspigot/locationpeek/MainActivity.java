package me.lordspigot.locationpeek;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "location_peek";
    private EditText latInput;
    private EditText lonInput;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        setContentView(buildUi());
    }

    private View buildUi() {
        int pad = dp(22);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(8, 10, 12));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Location Peek", 30, Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("Simulierter Android-Standort über die Entwickleroptionen", 15, Color.rgb(170, 178, 188));
        subtitle.setPadding(0, dp(4), 0, dp(22));
        root.addView(subtitle);

        status = text("Bereit. Wähle Location Peek zuerst als App für simulierte Standorte aus.", 15, Color.rgb(210, 216, 224));
        status.setBackgroundColor(Color.rgb(22, 27, 31));
        status.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(status, lpMatchWrap());

        Button devSettings = button("Entwickleroptionen öffnen");
        devSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        root.addView(devSettings, lpTop(14));

        TextView coordsTitle = text("Koordinaten", 20, Color.WHITE);
        coordsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        coordsTitle.setPadding(0, dp(28), 0, dp(10));
        root.addView(coordsTitle);

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedLat = p.getString("lat", "47.0707");
        String savedLon = p.getString("lon", "15.4395");

        latInput = input("Breitengrad", savedLat);
        lonInput = input("Längengrad", savedLon);
        root.addView(latInput, lpMatchWrap());
        root.addView(lonInput, lpTop(10));

        TextView presets = text("Schnellauswahl", 16, Color.rgb(180, 188, 198));
        presets.setPadding(0, dp(20), 0, dp(8));
        root.addView(presets);

        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setWeightSum(3f);
        addPreset(presetRow, "Graz", 47.0707, 15.4395);
        addPreset(presetRow, "Wien", 48.2082, 16.3738);
        addPreset(presetRow, "Berlin", 52.5200, 13.4050);
        root.addView(presetRow);

        Button start = button("Standort simulieren");
        start.setTextColor(Color.BLACK);
        start.setBackgroundColor(Color.rgb(98, 245, 139));
        start.setOnClickListener(v -> startMock());
        root.addView(start, lpTop(24));

        Button stop = button("Simulation stoppen");
        stop.setOnClickListener(v -> stopMock());
        root.addView(stop, lpTop(10));

        Space spacer = new Space(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(18)));

        TextView note = text("Hinweis: Android kennzeichnet Teststandorte als Mock Location. Andere Apps können das erkennen oder den Standort ignorieren.", 13, Color.rgb(130, 140, 150));
        note.setLineSpacing(0, 1.15f);
        root.addView(note);

        return scroll;
    }

    private void addPreset(LinearLayout row, String label, double lat, double lon) {
        Button b = button(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(params);
        b.setOnClickListener(v -> {
            latInput.setText(String.format(Locale.US, "%.6f", lat));
            lonInput.setText(String.format(Locale.US, "%.6f", lon));
        });
        row.addView(b);
    }

    private void startMock() {
        try {
            double lat = Double.parseDouble(latInput.getText().toString().trim().replace(',', '.'));
            double lon = Double.parseDouble(lonInput.getText().toString().trim().replace(',', '.'));
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();

            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("lat", Double.toString(lat))
                    .putString("lon", Double.toString(lon))
                    .apply();

            Intent i = new Intent(this, MockLocationService.class);
            i.setAction(MockLocationService.ACTION_START);
            i.putExtra(MockLocationService.EXTRA_LAT, lat);
            i.putExtra(MockLocationService.EXTRA_LON, lon);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);

            status.setText(String.format(Locale.US, "🟢 Simulation gestartet: %.6f, %.6f\nJetzt kannst du zu Lime wechseln.", lat, lon));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Bitte gültige Koordinaten eingeben.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            status.setText("🔴 Konnte die Simulation nicht starten: " + e.getMessage());
        }
    }

    private void stopMock() {
        Intent i = new Intent(this, MockLocationService.class);
        i.setAction(MockLocationService.ACTION_STOP);
        startService(i);
        status.setText("Simulation gestoppt.");
    }

    private TextView text(String value, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(115, 125, 135));
        e.setTextColor(Color.WHITE);
        e.setText(value);
        e.setTextSize(18);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setBackgroundColor(Color.rgb(22, 27, 31));
        return e;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        return b;
    }

    private LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams lpTop(int topDp) {
        LinearLayout.LayoutParams p = lpMatchWrap();
        p.topMargin = dp(topDp);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
