package pl.prosteradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Ekran ustawień: język, kraj, bufor, aktualizacje, kontakt, wsparcie i informacje o aplikacji. */
public class SettingsActivity extends Activity {
    private static final String SUPPORT_URL = "https://www.buymeacoffee.com/retroandroidradio.com";
    private static final String CONTACT_EMAIL = "contact@retroandroidradio.com";

    private TextView languageValue, bufferValue, countryValue;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppSettings.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        languageValue = findViewById(R.id.language_value);
        bufferValue = findViewById(R.id.buffer_value);
        countryValue = findViewById(R.id.country_value);
        findViewById(R.id.country_row).setOnClickListener(v -> chooseCountry());

        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.language_row).setOnClickListener(v -> chooseLanguage());
        findViewById(R.id.buffer_row).setOnClickListener(v -> chooseBuffer());
        ((TextView) findViewById(R.id.version)).setText(
                getString(R.string.app_name) + "  ·  " + getString(R.string.version_label, versionText()));
        findViewById(R.id.about_row).setOnClickListener(v -> showAbout());
        findViewById(R.id.updates_row).setOnClickListener(v -> openLink(websiteUrl()));
        findViewById(R.id.support_row).setOnClickListener(v -> openLink(SUPPORT_URL));
        findViewById(R.id.contact_row).setOnClickListener(v -> writeEmail());
        findViewById(R.id.contact_row).setOnLongClickListener(v -> {
            copy(CONTACT_EMAIL);
            return true;
        });
        refresh();
    }

    private void refresh() {
        languageValue.setText(AppSettings.LANG_NAMES[AppSettings.languageIndex(this)]);
        bufferValue.setText(getString(R.string.seconds, AppSettings.bufferSeconds(this)));
        countryValue.setText(AppSettings.countryName(this, AppSettings.country(this)));
    }

    /**
     * Adres strony z aktualizacjami. Android starszy niż 7.1.1 nie ufa certyfikatom Let's Encrypt,
     * więc tam otwieramy zwykłe http (serwer nie przekierowuje starych Androidów na https).
     */
    private static String websiteUrl() {
        return (Build.VERSION.SDK_INT >= 25 ? "https" : "http") + "://retroandroidradio.com/";
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            copy(url);
        }
    }

    private void writeEmail() {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + CONTACT_EMAIL));
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + versionText());
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            copy(CONTACT_EMAIL);
        }
    }

    /** Kopiuje tekst do schowka (gdy nie ma przeglądarki / programu pocztowego). */
    private void copy(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText(text, text));
            Toast.makeText(this, getString(R.string.copied) + ": " + text, Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        }
    }

    /** Numer wersji w postaci „v.1.3.3”. */
    private String versionText() {
        try {
            return "v." + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressWarnings("deprecation")
    private void showAbout() {
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle(R.string.about)
                .setMessage(getString(R.string.app_name) + "\n"
                        + getString(R.string.version_label, versionText()) + "\n\n"
                        + getString(R.string.about_sources))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void chooseLanguage() {
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(AppSettings.LANG_NAMES, AppSettings.languageIndex(this), (d, which) -> {
                    d.dismiss();
                    String code = AppSettings.LANG_CODES[which];
                    if (!code.equals(AppSettings.language(this))) {
                        AppSettings.setLanguage(this, code);
                        recreate(); // przeładuj ekran w nowym języku
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Wszystkie kraje, posortowane alfabetycznie w języku aplikacji. */
    @SuppressWarnings("deprecation")
    private void chooseCountry() {
        final Locale lang = AppSettings.locale(AppSettings.language(this));
        final Collator coll = Collator.getInstance(lang);
        List<String[]> all = new ArrayList<>();
        for (String code : Locale.getISOCountries()) {
            String name = new Locale("", code).getDisplayCountry(lang);
            if (name != null && name.length() > 0 && !name.equals(code)) all.add(new String[]{code, name});
        }
        Collections.sort(all, (a, b) -> coll.compare(a[1], b[1]));
        final String[] codes = new String[all.size()];
        String[] names = new String[all.size()];
        int checked = -1;
        String current = AppSettings.country(this);
        for (int i = 0; i < all.size(); i++) {
            codes[i] = all.get(i)[0];
            names[i] = all.get(i)[1];
            if (codes[i].equals(current)) checked = i;
        }
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle(R.string.settings_country)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    AppSettings.setCountry(this, codes[which]);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void chooseBuffer() {
        final int[] choices = AppSettings.BUFFER_CHOICES;
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) labels[i] = getString(R.string.seconds, choices[i]);
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle(R.string.settings_buffer)
                .setSingleChoiceItems(labels, AppSettings.bufferIndex(this), (d, which) -> {
                    d.dismiss();
                    AppSettings.setBufferSeconds(this, choices[which]);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
