/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.net.Uri;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.RadioCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.QRCodeBottomSheet;
import org.telegram.messenger.amnezia.AwgCacheManager;
import org.telegram.messenger.amnezia.AwgConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ProxySettingsActivity extends BaseFragment {

    private final static int TYPE_SOCKS5 = 0;
    private final static int TYPE_MTPROTO = 1;
    private final static int TYPE_AMNEZIA = 2;

    private final static int FIELD_IP = 0;
    private final static int FIELD_PORT = 1;
    private final static int FIELD_USER = 2;
    private final static int FIELD_PASSWORD = 3;
    private final static int FIELD_SECRET = 4;

    private final static int FIELD_AWG_PRIVKEY = 5;
    private final static int FIELD_AWG_PUBKEY = 6;
    private final static int FIELD_AWG_PRESHAREDKEY = 7;
    private final static int FIELD_AWG_ADDRESS = 8;
    private final static int FIELD_AWG_DNS = 9;
    private final static int FIELD_AWG_JC = 10;
    private final static int FIELD_AWG_JMIN = 11;
    private final static int FIELD_AWG_JMAX = 12;
    private final static int FIELD_AWG_S1 = 13;
    private final static int FIELD_AWG_S2 = 14;
    private final static int FIELD_AWG_S3 = 15;
    private final static int FIELD_AWG_S4 = 16;
    private final static int FIELD_AWG_H1 = 17;
    private final static int FIELD_AWG_H2 = 18;
    private final static int FIELD_AWG_H3 = 19;
    private final static int FIELD_AWG_H4 = 20;
    private final static int FIELD_AWG_I1 = 21;
    private final static int FIELD_AWG_I2 = 22;
    private final static int FIELD_AWG_I3 = 23;
    private final static int FIELD_AWG_I4 = 24;
    private final static int FIELD_AWG_I5 = 25;
    private final static int FIELD_AWG_KEEPALIVE = 26;

    private EditTextBoldCursor[] inputFields;
    private ScrollView scrollView;
    private LinearLayout linearLayout2;
    private LinearLayout inputFieldsContainer;
    private HeaderCell headerCell;
    private ShadowSectionCell[] sectionCell = new ShadowSectionCell[3];
    private TextInfoPrivacyCell[] bottomCells = new TextInfoPrivacyCell[3];
    private TextSettingsCell shareCell;
    private TextSettingsCell pasteCell;
    private ActionBarMenuItem doneItem;

    private RadioCell[] typeCell = new RadioCell[3];
    private int currentType = -1;

    private int pasteType = -1;
    private String pasteString;
    private String[] pasteFields;

    private float shareDoneProgress = 1f;
    private float[] shareDoneProgressAnimValues = new float[2];
    private boolean shareDoneEnabled = true;
    private ValueAnimator shareDoneAnimator;

    private ClipboardManager clipboardManager;
    private AwgCacheManager awgCacheManager;

    private boolean addingNewProxy;
    private SharedConfig.ProxyInfo currentProxyInfo;
    private boolean ignoreOnTextChange;
    private Uri initialUri;

    private static final int done_button = 1;

    public ProxySettingsActivity() {
        super();
        currentProxyInfo = new SharedConfig.ProxyInfo("", 1080, "", "", "");
        addingNewProxy = true;
    }

    public ProxySettingsActivity(SharedConfig.ProxyInfo proxyInfo) {
        super();
        currentProxyInfo = proxyInfo;
    }

    public ProxySettingsActivity(Uri uri) {
        super();
        currentProxyInfo = new SharedConfig.ProxyInfo("", 1080, "", "", "");
        addingNewProxy = true;
        initialUri = uri;
        parseUri(uri);
    }

    private void parseUri(Uri uri) {
        try {
            currentProxyInfo.address = uri.getQueryParameter("server");
            currentProxyInfo.port = Utilities.parseInt(uri.getQueryParameter("port"));
            currentProxyInfo.username = uri.getQueryParameter("user");
            currentProxyInfo.password = uri.getQueryParameter("pass");
            currentProxyInfo.secret = uri.getQueryParameter("secret");
            
            String awg = uri.getQueryParameter("awg");
            if ("1".equals(awg) || uri.toString().startsWith("tg://amnezia") || uri.toString().startsWith("tg:amnezia")) {
                currentProxyInfo.isAmneziaWG = true;
            }

        } catch (Exception ignore) {}
    }

    private ClipboardManager.OnPrimaryClipChangedListener clipChangedListener = this::updatePasteCell;

    @Override
    public void onResume() {
        super.onResume();
        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
        clipboardManager.addPrimaryClipChangedListener(clipChangedListener);
        updatePasteCell();
    }

    @Override
    public void onPause() {
        super.onPause();
        clipboardManager.removePrimaryClipChangedListener(clipChangedListener);
    }

    @Override
    public View createView(Context context) {
        awgCacheManager = new AwgCacheManager(context);

        actionBar.setTitle(LocaleController.getString(R.string.ProxyDetails));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(false);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (getParentActivity() == null) {
                        return;
                    }

                    if (currentType == TYPE_AMNEZIA) {
                        String address = inputFields[FIELD_IP].getText().toString();
                        String portStr = inputFields[FIELD_PORT].getText().toString();
                        
                        StringBuilder sb = new StringBuilder();
                        sb.append("[Interface]\n");
                        sb.append("Address = ").append(inputFields[FIELD_AWG_ADDRESS].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_DNS].getText())) sb.append("DNS = ").append(inputFields[FIELD_AWG_DNS].getText().toString()).append("\n");
                        sb.append("PrivateKey = ").append(inputFields[FIELD_AWG_PRIVKEY].getText().toString()).append("\n");
                        
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_JC].getText())) sb.append("Jc = ").append(inputFields[FIELD_AWG_JC].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_JMIN].getText())) sb.append("Jmin = ").append(inputFields[FIELD_AWG_JMIN].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_JMAX].getText())) sb.append("Jmax = ").append(inputFields[FIELD_AWG_JMAX].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_S1].getText())) sb.append("S1 = ").append(inputFields[FIELD_AWG_S1].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_S2].getText())) sb.append("S2 = ").append(inputFields[FIELD_AWG_S2].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_S3].getText())) sb.append("S3 = ").append(inputFields[FIELD_AWG_S3].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_S4].getText())) sb.append("S4 = ").append(inputFields[FIELD_AWG_S4].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_H1].getText())) sb.append("H1 = ").append(inputFields[FIELD_AWG_H1].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_H2].getText())) sb.append("H2 = ").append(inputFields[FIELD_AWG_H2].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_H3].getText())) sb.append("H3 = ").append(inputFields[FIELD_AWG_H3].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_H4].getText())) sb.append("H4 = ").append(inputFields[FIELD_AWG_H4].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_I1].getText())) sb.append("I1 = ").append(inputFields[FIELD_AWG_I1].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_I2].getText())) sb.append("I2 = ").append(inputFields[FIELD_AWG_I2].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_I3].getText())) sb.append("I3 = ").append(inputFields[FIELD_AWG_I3].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_I4].getText())) sb.append("I4 = ").append(inputFields[FIELD_AWG_I4].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_I5].getText())) sb.append("I5 = ").append(inputFields[FIELD_AWG_I5].getText().toString()).append("\n\n");

                        sb.append("[Peer]\n");
                        sb.append("PublicKey = ").append(inputFields[FIELD_AWG_PUBKEY].getText().toString()).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_PRESHAREDKEY].getText())) sb.append("PresharedKey = ").append(inputFields[FIELD_AWG_PRESHAREDKEY].getText().toString()).append("\n");
                        sb.append("Endpoint = ").append(address).append(":").append(portStr).append("\n");
                        if (!TextUtils.isEmpty(inputFields[FIELD_AWG_KEEPALIVE].getText())) sb.append("PersistentKeepalive = ").append(inputFields[FIELD_AWG_KEEPALIVE].getText().toString()).append("\n");
                        sb.append("AllowedIPs = 0.0.0.0/0, ::/0\n");

                        String configId = "awg_config_" + System.currentTimeMillis();
                        AwgConfig awgConfig = new AwgConfig(configId, "AmneziaWG Tunnel", sb.toString());
                        awgCacheManager.saveConfig(awgConfig);
                        awgCacheManager.setActiveConfigId(configId);

                        currentProxyInfo.address = address;
                        currentProxyInfo.port = Utilities.parseInt(portStr);
                        currentProxyInfo.isAmneziaWG = true;
                        currentProxyInfo.awgConfigId = configId;
                        currentProxyInfo.username = "";
                        currentProxyInfo.password = "";
                        currentProxyInfo.secret = "";

                        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                        SharedPreferences.Editor editor = preferences.edit();
                        if (addingNewProxy) {
                            SharedConfig.addProxy(currentProxyInfo);
                        } else {
                            SharedConfig.saveProxyList();
                        }
                        SharedConfig.currentProxy = currentProxyInfo;

                        editor.putBoolean("proxy_enabled", true);
                        editor.putString("proxy_ip", currentProxyInfo.address);
                        editor.putInt("proxy_port", currentProxyInfo.port);
                        editor.putString("proxy_user", "");
                        editor.putString("proxy_pass", "");
                        editor.putString("proxy_secret", "");
                        editor.putString("proxy_awg_id", currentProxyInfo.awgConfigId);
                        editor.apply();

                        ConnectionsManager.setProxySettings(false, "", 0, "", "", "");
                    } else {
                        currentProxyInfo.address = inputFields[FIELD_IP].getText().toString();
                        currentProxyInfo.port = Utilities.parseInt(inputFields[FIELD_PORT].getText().toString());
                        if (currentType == TYPE_SOCKS5) {
                            currentProxyInfo.secret = "";
                            currentProxyInfo.username = inputFields[FIELD_USER].getText().toString();
                            currentProxyInfo.password = inputFields[FIELD_PASSWORD].getText().toString();
                        } else {
                            currentProxyInfo.secret = inputFields[FIELD_SECRET].getText().toString();
                            currentProxyInfo.username = "";
                            currentProxyInfo.password = "";
                        }

                        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled;
                        if (addingNewProxy) {
                            SharedConfig.addProxy(currentProxyInfo);
                            SharedConfig.currentProxy = currentProxyInfo;
                            editor.putBoolean("proxy_enabled", true);
                            enabled = true;
                        } else {
                            enabled = preferences.getBoolean("proxy_enabled", false);
                            SharedConfig.saveProxyList();
                        }
                        if (addingNewProxy || SharedConfig.currentProxy == currentProxyInfo) {
                            editor.putString("proxy_ip", currentProxyInfo.address);
                            editor.putString("proxy_pass", currentProxyInfo.password);
                            editor.putString("proxy_user", currentProxyInfo.username);
                            editor.putInt("proxy_port", currentProxyInfo.port);
                            editor.putString("proxy_secret", currentProxyInfo.secret);
                            editor.putString("proxy_awg_id", "");
                            ConnectionsManager.setProxySettings(enabled, currentProxyInfo.address, currentProxyInfo.port, currentProxyInfo.username, currentProxyInfo.password, currentProxyInfo.secret);
                        }
                        editor.commit();
                        awgCacheManager.setActiveConfigId("");
                    }

                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                    finishFragment();
                }
            }
        });

        doneItem = actionBar.createMenu().addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56));
        doneItem.setContentDescription(LocaleController.getString(R.string.Done));

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        AndroidUtilities.setScrollViewEdgeEffectColor(scrollView, Theme.getColor(Theme.key_actionBarDefault));
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout2, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final View.OnClickListener typeCellClickListener = view -> setProxyType((Integer) view.getTag(), true);

        for (int a = 0; a < 3; a++) {
            typeCell[a] = new RadioCell(context);
            typeCell[a].setBackground(Theme.getSelectorDrawable(true));
            typeCell[a].setTag(a);
            if (a == TYPE_SOCKS5) {
                typeCell[a].setText(LocaleController.getString(R.string.UseProxySocks5), a == currentType, true);
            } else if (a == TYPE_MTPROTO) {
                typeCell[a].setText(LocaleController.getString(R.string.UseProxyTelegram), a == currentType, true);
            } else if (a == TYPE_AMNEZIA) {
                typeCell[a].setText("AmneziaWG Tunnel", a == currentType, false);
            }
            linearLayout2.addView(typeCell[a], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
            typeCell[a].setOnClickListener(typeCellClickListener);
        }

        sectionCell[0] = new ShadowSectionCell(context);
        linearLayout2.addView(sectionCell[0], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        inputFieldsContainer = new LinearLayout(context);
        inputFieldsContainer.setOrientation(LinearLayout.VERTICAL);
        inputFieldsContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            inputFieldsContainer.setElevation(AndroidUtilities.dp(1f));
            inputFieldsContainer.setOutlineProvider(null);
        }
        linearLayout2.addView(inputFieldsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        inputFields = new EditTextBoldCursor[27];
        for (int a = 0; a < 27; a++) {
            FrameLayout container = new FrameLayout(context);
            inputFieldsContainer.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

            inputFields[a] = new EditTextBoldCursor(context);
            inputFields[a].setTag(a);
            inputFields[a].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            inputFields[a].setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
            inputFields[a].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            inputFields[a].setBackground(null);
            inputFields[a].setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            inputFields[a].setCursorSize(AndroidUtilities.dp(20));
            inputFields[a].setCursorWidth(1.5f);
            inputFields[a].setSingleLine(true);
            inputFields[a].setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            inputFields[a].setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
            inputFields[a].setTransformHintToHeader(true);
            inputFields[a].setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));

            if (a == FIELD_IP) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_URI);
                inputFields[a].addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) { checkShareDone(true); }
                });
            } else if (a == FIELD_PORT || a == FIELD_AWG_JC || a == FIELD_AWG_JMIN || a == FIELD_AWG_JMAX
                    || a == FIELD_AWG_S1 || a == FIELD_AWG_S2 || a == FIELD_AWG_S3 || a == FIELD_AWG_S4 || a == FIELD_AWG_KEEPALIVE) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_NUMBER);
                if (a == FIELD_PORT) {
                    inputFields[a].addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override public void afterTextChanged(Editable s) {
                            if (ignoreOnTextChange) return;
                            EditTextBoldCursor phoneField = inputFields[FIELD_PORT];
                            int startSel = phoneField.getSelectionStart();
                            String chars = "0123456789";
                            String str = phoneField.getText().toString();
                            StringBuilder builder = new StringBuilder(str.length());
                            for (int i = 0; i < str.length(); i++) {
                                String ch = str.substring(i, i + 1);
                                if (chars.contains(ch)) builder.append(ch);
                            }
                            ignoreOnTextChange = true;
                            int port = Utilities.parseInt(builder.toString());
                            if (port < 0 || port > 65535 || !str.equals(builder.toString())) {
                                if (port < 0) phoneField.setText("0");
                                else if (port > 65535) phoneField.setText("65535");
                                else phoneField.setText(builder.toString());
                            } else {
                                if (startSel >= 0) phoneField.setSelection(Math.min(startSel, phoneField.length()));
                            }
                            ignoreOnTextChange = false;
                            checkShareDone(true);
                        }
                    });
                }
            } else if (a == FIELD_PASSWORD) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                inputFields[a].setTypeface(Typeface.DEFAULT);
                inputFields[a].setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            }

            inputFields[a].setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

            switch (a) {
                case FIELD_IP:
                    inputFields[a].setHintText(LocaleController.getString(R.string.UseProxyAddress));
                    inputFields[a].setText(currentProxyInfo.address);
                    break;
                case FIELD_PORT:
                    inputFields[a].setHintText(LocaleController.getString(R.string.UseProxyPort));
                    inputFields[a].setText("" + currentProxyInfo.port);
                    break;
                case FIELD_USER:
                    inputFields[a].setHintText(LocaleController.getString(R.string.UseProxyUsername));
                    inputFields[a].setText(currentProxyInfo.username);
                    break;
                case FIELD_PASSWORD:
                    inputFields[a].setHintText(LocaleController.getString(R.string.UseProxyPassword));
                    inputFields[a].setText(currentProxyInfo.password);
                    break;
                case FIELD_SECRET:
                    inputFields[a].setHintText(LocaleController.getString(R.string.UseProxySecret));
                    inputFields[a].setText(currentProxyInfo.secret);
                    break;
                case FIELD_AWG_PRIVKEY: inputFields[a].setHintText("Приватный ключ (Private Key)"); break;
                case FIELD_AWG_PUBKEY: inputFields[a].setHintText("Публичный ключ сервера (Public Key)"); break;
                case FIELD_AWG_PRESHAREDKEY: inputFields[a].setHintText("Preshared Key (Опционально)"); break;
                case FIELD_AWG_ADDRESS: inputFields[a].setHintText("Address (Напр. 10.8.1.2/32)"); break;
                case FIELD_AWG_DNS: inputFields[a].setHintText("DNS (Напр. 1.1.1.1, 8.8.8.8)"); break;
                case FIELD_AWG_JC: inputFields[a].setHintText("Jc (Количество мусорных пакетов)"); break;
                case FIELD_AWG_JMIN: inputFields[a].setHintText("Jmin"); break;
                case FIELD_AWG_JMAX: inputFields[a].setHintText("Jmax"); break;
                case FIELD_AWG_S1: inputFields[a].setHintText("S1"); break;
                case FIELD_AWG_S2: inputFields[a].setHintText("S2"); break;
                case FIELD_AWG_S3: inputFields[a].setHintText("S3"); break;
                case FIELD_AWG_S4: inputFields[a].setHintText("S4"); break;
                case FIELD_AWG_H1: inputFields[a].setHintText("H1"); break;
                case FIELD_AWG_H2: inputFields[a].setHintText("H2"); break;
                case FIELD_AWG_H3: inputFields[a].setHintText("H3"); break;
                case FIELD_AWG_H4: inputFields[a].setHintText("H4"); break;
                case FIELD_AWG_I1: inputFields[a].setHintText("I1"); break;
                case FIELD_AWG_I2: inputFields[a].setHintText("I2"); break;
                case FIELD_AWG_I3: inputFields[a].setHintText("I3"); break;
                case FIELD_AWG_I4: inputFields[a].setHintText("I4"); break;
                case FIELD_AWG_I5: inputFields[a].setHintText("I5"); break;
                case FIELD_AWG_KEEPALIVE: inputFields[a].setHintText("PersistentKeepalive (Опционально)"); break;
            }

            inputFields[a].setSelection(inputFields[a].length());
            inputFields[a].setPadding(0, 0, 0, 0);
            container.addView(inputFields[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 17, a == FIELD_IP ? 12 : 0, 17, 0));

            inputFields[a].setOnEditorActionListener((textView, i, keyEvent) -> {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    int num = (Integer) textView.getTag();
                    if (num + 1 < inputFields.length) {
                        num++;
                        inputFields[num].requestFocus();
                    }
                    return true;
                } else if (i == EditorInfo.IME_ACTION_DONE) {
                    finishFragment();
                    return true;
                }
                return false;
            });
        }

        if (initialUri != null) {
            try {
                if (initialUri.getQueryParameter("pc") != null) inputFields[FIELD_AWG_PRIVKEY].setText(initialUri.getQueryParameter("pc"));
                if (initialUri.getQueryParameter("pk") != null) inputFields[FIELD_AWG_PUBKEY].setText(initialUri.getQueryParameter("pk"));
                if (initialUri.getQueryParameter("psk") != null) inputFields[FIELD_AWG_PRESHAREDKEY].setText(initialUri.getQueryParameter("psk"));
                if (initialUri.getQueryParameter("addr") != null) inputFields[FIELD_AWG_ADDRESS].setText(initialUri.getQueryParameter("addr"));
                if (initialUri.getQueryParameter("dns") != null) inputFields[FIELD_AWG_DNS].setText(initialUri.getQueryParameter("dns"));
                if (initialUri.getQueryParameter("jc") != null) inputFields[FIELD_AWG_JC].setText(initialUri.getQueryParameter("jc"));
                if (initialUri.getQueryParameter("jmin") != null) inputFields[FIELD_AWG_JMIN].setText(initialUri.getQueryParameter("jmin"));
                if (initialUri.getQueryParameter("jmax") != null) inputFields[FIELD_AWG_JMAX].setText(initialUri.getQueryParameter("jmax"));
                if (initialUri.getQueryParameter("s1") != null) inputFields[FIELD_AWG_S1].setText(initialUri.getQueryParameter("s1"));
                if (initialUri.getQueryParameter("s2") != null) inputFields[FIELD_AWG_S2].setText(initialUri.getQueryParameter("s2"));
                if (initialUri.getQueryParameter("s3") != null) inputFields[FIELD_AWG_S3].setText(initialUri.getQueryParameter("s3"));
                if (initialUri.getQueryParameter("s4") != null) inputFields[FIELD_AWG_S4].setText(initialUri.getQueryParameter("s4"));
                if (initialUri.getQueryParameter("h1") != null) inputFields[FIELD_AWG_H1].setText(initialUri.getQueryParameter("h1"));
                if (initialUri.getQueryParameter("h2") != null) inputFields[FIELD_AWG_H2].setText(initialUri.getQueryParameter("h2"));
                if (initialUri.getQueryParameter("h3") != null) inputFields[FIELD_AWG_H3].setText(initialUri.getQueryParameter("h3"));
                if (initialUri.getQueryParameter("h4") != null) inputFields[FIELD_AWG_H4].setText(initialUri.getQueryParameter("h4"));
                if (initialUri.getQueryParameter("i1") != null) inputFields[FIELD_AWG_I1].setText(initialUri.getQueryParameter("i1"));
                if (initialUri.getQueryParameter("i2") != null) inputFields[FIELD_AWG_I2].setText(initialUri.getQueryParameter("i2"));
                if (initialUri.getQueryParameter("i3") != null) inputFields[FIELD_AWG_I3].setText(initialUri.getQueryParameter("i3"));
                if (initialUri.getQueryParameter("i4") != null) inputFields[FIELD_AWG_I4].setText(initialUri.getQueryParameter("i4"));
                if (initialUri.getQueryParameter("i5") != null) inputFields[FIELD_AWG_I5].setText(initialUri.getQueryParameter("i5"));
                if (initialUri.getQueryParameter("ka") != null) inputFields[FIELD_AWG_KEEPALIVE].setText(initialUri.getQueryParameter("ka"));
            } catch (Exception ignore) {}
        }

        for (int i = 0; i < 3; i++) {
            bottomCells[i] = new TextInfoPrivacyCell(context);
            bottomCells[i].setBackground(Theme.getThemedDrawableByKey(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
            if (i == 0) {
                bottomCells[i].setText(LocaleController.getString(R.string.UseProxyInfo));
            } else if (i == 1) {
                bottomCells[i].setText(LocaleController.getString(R.string.UseProxyTelegramInfo) + "\n\n" + LocaleController.getString(R.string.UseProxyTelegramInfo2));
                bottomCells[i].setVisibility(View.GONE);
            } else {
                bottomCells[i].setText("Настройки защищенного туннеля AmneziaWG. Позволяет обходить блокировки благодаря продвинутой обфускации трафика WireGuard.");
                bottomCells[i].setVisibility(View.GONE);
            }
            linearLayout2.addView(bottomCells[i], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        pasteCell = new TextSettingsCell(fragmentView.getContext());
        pasteCell.setBackground(Theme.getSelectorDrawable(true));
        pasteCell.setText(LocaleController.getString(R.string.PasteFromClipboard), false);
        pasteCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        pasteCell.setOnClickListener(v -> {
            if (pasteType != -1) {
                for (int i = 0; i < 27; i++) {
                    if (pasteType == TYPE_SOCKS5 && i == FIELD_SECRET) continue;
                    if (pasteType == TYPE_MTPROTO && (i == FIELD_USER || i == FIELD_PASSWORD)) continue;
                    if (pasteType == TYPE_MTPROTO && i >= FIELD_AWG_PRIVKEY) continue;
                    if (pasteType == TYPE_SOCKS5 && i >= FIELD_AWG_PRIVKEY) continue;
                    if (pasteFields[i] != null) {
                        try {
                            inputFields[i].setText(URLDecoder.decode(pasteFields[i], "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            inputFields[i].setText(pasteFields[i]);
                        }
                    } else {
                        inputFields[i].setText(null);
                    }
                }
                inputFields[0].setSelection(inputFields[0].length());
                setProxyType(pasteType, true, () -> {
                    AndroidUtilities.hideKeyboard(inputFieldsContainer.findFocus());
                    for (int i = 0; i < 27; i++) {
                        if (pasteType == TYPE_SOCKS5 && (i == FIELD_IP || i == FIELD_PORT || i == FIELD_USER || i == FIELD_PASSWORD)) continue;
                        if (pasteType == TYPE_MTPROTO && (i == FIELD_IP || i == FIELD_PORT || i == FIELD_SECRET)) continue;
                        if (pasteType == TYPE_AMNEZIA && (i == FIELD_IP || i == FIELD_PORT || (i >= FIELD_AWG_PRIVKEY && i <= FIELD_AWG_KEEPALIVE))) continue;
                        inputFields[i].setText(null);
                    }
                });
            }
        });
        linearLayout2.addView(pasteCell, 0, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        pasteCell.setVisibility(View.GONE);

        sectionCell[2] = new ShadowSectionCell(fragmentView.getContext());
        sectionCell[2].setBackground(Theme.getThemedDrawableByKey(fragmentView.getContext(), R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        linearLayout2.addView(sectionCell[2], 1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        sectionCell[2].setVisibility(View.GONE);

        TextSettingsCell importCell = new TextSettingsCell(context);
        importCell.setBackground(Theme.getSelectorDrawable(true));
        importCell.setText("Импортировать .conf файл", false);
        importCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        linearLayout2.addView(importCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        importCell.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, 500);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });

        shareCell = new TextSettingsCell(context);
        shareCell.setBackground(Theme.getSelectorDrawable(true));
        shareCell.setText(LocaleController.getString(R.string.ShareFile), false);
        shareCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        linearLayout2.addView(shareCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        shareCell.setOnClickListener(v -> {
            StringBuilder params = new StringBuilder();
            String address = inputFields[FIELD_IP].getText().toString();
            String password = inputFields[FIELD_PASSWORD].getText().toString();
            String user = inputFields[FIELD_USER].getText().toString();
            String port = inputFields[FIELD_PORT].getText().toString();
            String secret = inputFields[FIELD_SECRET].getText().toString();
            String url;
            String link;
            try {
                if (!TextUtils.isEmpty(address)) params.append("server=").append(URLEncoder.encode(address, "UTF-8"));
                if (!TextUtils.isEmpty(port)) {
                    if (params.length() != 0) params.append("&");
                    params.append("port=").append(URLEncoder.encode(port, "UTF-8"));
                }
                
                if (currentType == TYPE_AMNEZIA) {
                    url = "https://t.me/proxy?awg=1";
                    if (params.length() != 0) url += "&" + params.toString();
                    
                    StringBuilder awgParams = new StringBuilder();
                    if (inputFields[FIELD_AWG_PRIVKEY].length() != 0) awgParams.append("&pc=").append(URLEncoder.encode(inputFields[FIELD_AWG_PRIVKEY].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_PUBKEY].length() != 0) awgParams.append("&pk=").append(URLEncoder.encode(inputFields[FIELD_AWG_PUBKEY].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_PRESHAREDKEY].length() != 0) awgParams.append("&psk=").append(URLEncoder.encode(inputFields[FIELD_AWG_PRESHAREDKEY].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_ADDRESS].length() != 0) awgParams.append("&addr=").append(URLEncoder.encode(inputFields[FIELD_AWG_ADDRESS].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_DNS].length() != 0) awgParams.append("&dns=").append(URLEncoder.encode(inputFields[FIELD_AWG_DNS].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_JC].length() != 0) awgParams.append("&jc=").append(URLEncoder.encode(inputFields[FIELD_AWG_JC].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_JMIN].length() != 0) awgParams.append("&jmin=").append(URLEncoder.encode(inputFields[FIELD_AWG_JMIN].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_JMAX].length() != 0) awgParams.append("&jmax=").append(URLEncoder.encode(inputFields[FIELD_AWG_JMAX].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_S1].length() != 0) awgParams.append("&s1=").append(URLEncoder.encode(inputFields[FIELD_AWG_S1].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_S2].length() != 0) awgParams.append("&s2=").append(URLEncoder.encode(inputFields[FIELD_AWG_S2].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_S3].length() != 0) awgParams.append("&s3=").append(URLEncoder.encode(inputFields[FIELD_AWG_S3].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_S4].length() != 0) awgParams.append("&s4=").append(URLEncoder.encode(inputFields[FIELD_AWG_S4].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_H1].length() != 0) awgParams.append("&h1=").append(URLEncoder.encode(inputFields[FIELD_AWG_H1].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_H2].length() != 0) awgParams.append("&h2=").append(URLEncoder.encode(inputFields[FIELD_AWG_H2].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_H3].length() != 0) awgParams.append("&h3=").append(URLEncoder.encode(inputFields[FIELD_AWG_H3].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_H4].length() != 0) awgParams.append("&h4=").append(URLEncoder.encode(inputFields[FIELD_AWG_H4].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_I1].length() != 0) awgParams.append("&i1=").append(URLEncoder.encode(inputFields[FIELD_AWG_I1].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_I2].length() != 0) awgParams.append("&i2=").append(URLEncoder.encode(inputFields[FIELD_AWG_I2].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_I3].length() != 0) awgParams.append("&i3=").append(URLEncoder.encode(inputFields[FIELD_AWG_I3].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_I4].length() != 0) awgParams.append("&i4=").append(URLEncoder.encode(inputFields[FIELD_AWG_I4].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_I5].length() != 0) awgParams.append("&i5=").append(URLEncoder.encode(inputFields[FIELD_AWG_I5].getText().toString(), "UTF-8"));
                    if (inputFields[FIELD_AWG_KEEPALIVE].length() != 0) awgParams.append("&ka=").append(URLEncoder.encode(inputFields[FIELD_AWG_KEEPALIVE].getText().toString(), "UTF-8"));
                    
                    link = url + awgParams.toString();
                } else if (currentType == TYPE_MTPROTO) {
                    url = "https://t.me/proxy?";
                    if (params.length() != 0) params.append("&");
                    params.append("secret=").append(URLEncoder.encode(secret, "UTF-8"));
                    link = url + params.toString();
                } else {
                    url = "https://t.me/socks?";
                    if (!TextUtils.isEmpty(user)) {
                        if (params.length() != 0) params.append("&");
                        params.append("user=").append(URLEncoder.encode(user, "UTF-8"));
                    }
                    if (!TextUtils.isEmpty(password)) {
                        if (params.length() != 0) params.append("&");
                        params.append("pass=").append(URLEncoder.encode(password, "UTF-8"));
                    }
                    link = url + params.toString();
                }
            } catch (Exception ignore) { return; }
            if (params.length() == 0) return;

            QRCodeBottomSheet alert = new QRCodeBottomSheet(context, LocaleController.getString(R.string.ShareQrCode), link, LocaleController.getString(R.string.QRCodeLinkHelpProxy), true);
            Bitmap icon = SvgHelper.getBitmap(AndroidUtilities.readRes(R.raw.qr_dog), AndroidUtilities.dp(60), AndroidUtilities.dp(60), false);
            alert.setCenterImage(icon);
            showDialog(alert);
        });

        sectionCell[1] = new ShadowSectionCell(context);
        sectionCell[1].setBackground(Theme.getThemedDrawableByKey(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        linearLayout2.addView(sectionCell[1], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        shareDoneEnabled = true;
        shareDoneProgress = 1f;
        checkShareDone(false);

        currentType = -1;
        if (currentProxyInfo.isAmneziaWG) {
            setProxyType(TYPE_AMNEZIA, false);
            if (currentProxyInfo.awgConfigId != null) {
                AwgConfig config = awgCacheManager.getAllConfigs().stream()
                        .filter(c -> c.getId().equals(currentProxyInfo.awgConfigId))
                        .findFirst().orElse(null);
                if (config != null) {
                    parseAwgConfig(config.getConfigText());
                }
            }
        } else {
            setProxyType(TextUtils.isEmpty(currentProxyInfo.secret) ? TYPE_SOCKS5 : TYPE_MTPROTO, false);
        }

        pasteType = -1;
        pasteString = null;
        updatePasteCell();

        return fragmentView;
    }

    private void updatePasteCell() {
        final ClipData clip = clipboardManager.getPrimaryClip();
        String clipText;
        if (clip != null && clip.getItemCount() > 0) {
            try { clipText = clip.getItemAt(0).coerceToText(fragmentView.getContext()).toString(); }
            catch (Exception e) { clipText = null; }
        } else { clipText = null; }

        if (TextUtils.equals(clipText, pasteString)) return;

        pasteType = -1;
        pasteString = clipText;
        pasteFields = new String[27];
        if (clipText != null) {
            String[] params = null;
            final String[] socksStrings = {"t.me/socks?", "tg://socks?"};
            for (int i = 0; i < socksStrings.length; i++) {
                final int index = clipText.indexOf(socksStrings[i]);
                if (index >= 0) {
                    pasteType = TYPE_SOCKS5;
                    params = clipText.substring(index + socksStrings[i].length()).split("&");
                    break;
                }
            }

            if (params == null) {
                final String[] proxyStrings = {"t.me/proxy?", "tg://proxy?", "t.me/amnezia?", "tg://amnezia?"};
                for (int i = 0; i < proxyStrings.length; i++) {
                    final int index = clipText.indexOf(proxyStrings[i]);
                    if (index >= 0) {
                        params = clipText.substring(index + proxyStrings[i].length()).split("&");
                        boolean isAwg = false;
                        for (String param : params) {
                            if (param.toLowerCase().startsWith("awg=1")) {
                                isAwg = true;
                                break;
                            }
                        }
                        if (isAwg || proxyStrings[i].contains("amnezia")) {
                            pasteType = TYPE_AMNEZIA;
                        } else {
                            pasteType = TYPE_MTPROTO;
                        }
                        break;
                    }
                }
            }

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    final String[] pair = params[i].split("=");
                    if (pair.length != 2) continue;
                    String key = pair[0].toLowerCase();
                    String val = pair[1];
                    switch (key) {
                        case "server": pasteFields[FIELD_IP] = val; break;
                        case "port": pasteFields[FIELD_PORT] = val; break;
                        case "user": if (pasteType == TYPE_SOCKS5) pasteFields[FIELD_USER] = val; break;
                        case "pass": if (pasteType == TYPE_SOCKS5) pasteFields[FIELD_PASSWORD] = val; break;
                        case "secret": if (pasteType == TYPE_MTPROTO) pasteFields[FIELD_SECRET] = val; break;
                        case "pc": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_PRIVKEY] = val; break;
                        case "pk": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_PUBKEY] = val; break;
                        case "psk": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_PRESHAREDKEY] = val; break;
                        case "addr": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_ADDRESS] = val; break;
                        case "dns": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_DNS] = val; break;
                        case "jc": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_JC] = val; break;
                        case "jmin": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_JMIN] = val; break;
                        case "jmax": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_JMAX] = val; break;
                        case "s1": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_S1] = val; break;
                        case "s2": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_S2] = val; break;
                        case "s3": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_S3] = val; break;
                        case "s4": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_S4] = val; break;
                        case "h1": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_H1] = val; break;
                        case "h2": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_H2] = val; break;
                        case "h3": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_H3] = val; break;
                        case "h4": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_H4] = val; break;
                        case "i1": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_I1] = val; break;
                        case "i2": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_I2] = val; break;
                        case "i3": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_I3] = val; break;
                        case "i4": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_I4] = val; break;
                        case "i5": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_I5] = val; break;
                        case "ka": if (pasteType == TYPE_AMNEZIA) pasteFields[FIELD_AWG_KEEPALIVE] = val; break;
                    }
                }
            }
        }

        if (pasteType != -1) {
            if (pasteCell.getVisibility() != View.VISIBLE) {
                pasteCell.setVisibility(View.VISIBLE);
                sectionCell[2].setVisibility(View.VISIBLE);
            }
        } else {
            if (pasteCell.getVisibility() != View.GONE) {
                pasteCell.setVisibility(View.GONE);
                sectionCell[2].setVisibility(View.GONE);
            }
        }
    }

    private void setShareDoneEnabled(boolean enabled, boolean animated) {
        if (shareDoneEnabled != enabled) {
            if (shareDoneAnimator != null) {
                shareDoneAnimator.cancel();
            } else if (animated) {
                shareDoneAnimator = ValueAnimator.ofFloat(0f, 1f);
                shareDoneAnimator.setDuration(200);
                shareDoneAnimator.addUpdateListener(a -> {
                    shareDoneProgress = AndroidUtilities.lerp(shareDoneProgressAnimValues, a.getAnimatedFraction());
                    shareCell.setTextColor(ColorUtils.blendARGB(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4), shareDoneProgress));
                    doneItem.setAlpha(shareDoneProgress / 2f + 0.5f);
                });
            }
            if (animated) {
                shareDoneProgressAnimValues[0] = shareDoneProgress;
                shareDoneProgressAnimValues[1] = enabled ? 1f : 0f;
                shareDoneAnimator.start();
            } else {
                shareDoneProgress = enabled ? 1f : 0f;
                shareCell.setTextColor(enabled ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4) : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                doneItem.setAlpha(enabled ? 1f : .5f);
            }
            shareCell.setEnabled(enabled);
            doneItem.setEnabled(enabled);
            shareDoneEnabled = enabled;
        }
    }

    private void checkShareDone(boolean animated) {
        if (shareCell == null || doneItem == null || inputFields[FIELD_IP] == null || inputFields[FIELD_PORT] == null) {
            return;
        }
        setShareDoneEnabled(inputFields[FIELD_IP].length() != 0 && Utilities.parseInt(inputFields[FIELD_PORT].getText().toString()) != 0, animated);
    }

    private void setProxyType(int type, boolean animated) {
        setProxyType(type, animated, null);
    }

    private void setProxyType(int type, boolean animated, Runnable onTransitionEnd) {
        if (currentType != type) {
            currentType = type;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                TransitionManager.endTransitions(linearLayout2);
            }
            if (animated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final TransitionSet transitionSet = new TransitionSet()
                        .addTransition(new Fade(Fade.OUT))
                        .addTransition(new ChangeBounds())
                        .addTransition(new Fade(Fade.IN))
                        .setInterpolator(CubicBezierInterpolator.DEFAULT)
                        .setDuration(250);

                if (onTransitionEnd != null) {
                    transitionSet.addListener(new Transition.TransitionListener() {
                        @Override public void onTransitionStart(Transition transition) {}
                        @Override public void onTransitionEnd(Transition transition) { onTransitionEnd.run(); }
                        @Override public void onTransitionCancel(Transition transition) {}
                        @Override public void onTransitionPause(Transition transition) {}
                        @Override public void onTransitionResume(Transition transition) {}
                    });
                }
                TransitionManager.beginDelayedTransition(linearLayout2, transitionSet);
            }

            if (currentType == TYPE_SOCKS5) {
                bottomCells[0].setVisibility(View.VISIBLE);
                bottomCells[1].setVisibility(View.GONE);
                bottomCells[2].setVisibility(View.GONE);
                shareCell.setVisibility(View.VISIBLE);
                sectionCell[1].setVisibility(View.VISIBLE);

                ((View) inputFields[FIELD_SECRET].getParent()).setVisibility(View.GONE);
                ((View) inputFields[FIELD_PASSWORD].getParent()).setVisibility(View.VISIBLE);
                ((View) inputFields[FIELD_USER].getParent()).setVisibility(View.VISIBLE);

                for (int i = FIELD_AWG_PRIVKEY; i <= FIELD_AWG_KEEPALIVE; i++) {
                    ((View) inputFields[i].getParent()).setVisibility(View.GONE);
                }
            } else if (currentType == TYPE_MTPROTO) {
                bottomCells[0].setVisibility(View.GONE);
                bottomCells[1].setVisibility(View.VISIBLE);
                bottomCells[2].setVisibility(View.GONE);
                shareCell.setVisibility(View.VISIBLE);
                sectionCell[1].setVisibility(View.VISIBLE);

                ((View) inputFields[FIELD_SECRET].getParent()).setVisibility(View.VISIBLE);
                ((View) inputFields[FIELD_PASSWORD].getParent()).setVisibility(View.GONE);
                ((View) inputFields[FIELD_USER].getParent()).setVisibility(View.GONE);

                for (int i = FIELD_AWG_PRIVKEY; i <= FIELD_AWG_KEEPALIVE; i++) {
                    ((View) inputFields[i].getParent()).setVisibility(View.GONE);
                }
            } else if (currentType == TYPE_AMNEZIA) {
                bottomCells[0].setVisibility(View.GONE);
                bottomCells[1].setVisibility(View.GONE);
                bottomCells[2].setVisibility(View.VISIBLE);
                shareCell.setVisibility(View.GONE);
                sectionCell[1].setVisibility(View.GONE);

                ((View) inputFields[FIELD_SECRET].getParent()).setVisibility(View.GONE);
                ((View) inputFields[FIELD_PASSWORD].getParent()).setVisibility(View.GONE);
                ((View) inputFields[FIELD_USER].getParent()).setVisibility(View.GONE);

                for (int i = FIELD_AWG_PRIVKEY; i <= FIELD_AWG_KEEPALIVE; i++) {
                    ((View) inputFields[i].getParent()).setVisibility(View.VISIBLE);
                }
            }

            typeCell[0].setChecked(currentType == TYPE_SOCKS5, animated);
            typeCell[1].setChecked(currentType == TYPE_MTPROTO, animated);
            typeCell[2].setChecked(currentType == TYPE_AMNEZIA, animated);
        }
    }

    private void parseAwgConfig(String configText) {
        if (TextUtils.isEmpty(configText)) return;
        String[] lines = configText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("PrivateKey")) inputFields[FIELD_AWG_PRIVKEY].setText(extractValue(line));
            else if (line.startsWith("PublicKey")) inputFields[FIELD_AWG_PUBKEY].setText(extractValue(line));
            else if (line.startsWith("PresharedKey")) inputFields[FIELD_AWG_PRESHAREDKEY].setText(extractValue(line));
            else if (line.startsWith("Address")) inputFields[FIELD_AWG_ADDRESS].setText(extractValue(line));
            else if (line.startsWith("DNS")) inputFields[FIELD_AWG_DNS].setText(extractValue(line));
            else if (line.startsWith("Jc")) inputFields[FIELD_AWG_JC].setText(extractValue(line));
            else if (line.startsWith("Jmin")) inputFields[FIELD_AWG_JMIN].setText(extractValue(line));
            else if (line.startsWith("Jmax")) inputFields[FIELD_AWG_JMAX].setText(extractValue(line));
            else if (line.startsWith("S1")) inputFields[FIELD_AWG_S1].setText(extractValue(line));
            else if (line.startsWith("S2")) inputFields[FIELD_AWG_S2].setText(extractValue(line));
            else if (line.startsWith("S3")) inputFields[FIELD_AWG_S3].setText(extractValue(line));
            else if (line.startsWith("S4")) inputFields[FIELD_AWG_S4].setText(extractValue(line));
            else if (line.startsWith("H1")) inputFields[FIELD_AWG_H1].setText(extractValue(line));
            else if (line.startsWith("H2")) inputFields[FIELD_AWG_H2].setText(extractValue(line));
            else if (line.startsWith("H3")) inputFields[FIELD_AWG_H3].setText(extractValue(line));
            else if (line.startsWith("H4")) inputFields[FIELD_AWG_H4].setText(extractValue(line));
            else if (line.startsWith("I1")) inputFields[FIELD_AWG_I1].setText(extractValue(line));
            else if (line.startsWith("I2")) inputFields[FIELD_AWG_I2].setText(extractValue(line));
            else if (line.startsWith("I3")) inputFields[FIELD_AWG_I3].setText(extractValue(line));
            else if (line.startsWith("I4")) inputFields[FIELD_AWG_I4].setText(extractValue(line));
            else if (line.startsWith("I5")) inputFields[FIELD_AWG_I5].setText(extractValue(line));
            else if (line.startsWith("PersistentKeepalive")) inputFields[FIELD_AWG_KEEPALIVE].setText(extractValue(line));
            else if (line.startsWith("Endpoint")) {
                String val = extractValue(line);
                if (val.contains(":")) {
                    int lastColon = val.lastIndexOf(":");
                    inputFields[FIELD_IP].setText(val.substring(0, lastColon));
                    inputFields[FIELD_PORT].setText(val.substring(lastColon + 1));
                } else {
                    inputFields[FIELD_IP].setText(val);
                }
            }
        }
    }

    private String extractValue(String line) {
        int idx = line.indexOf("=");
        if (idx != -1) return line.substring(idx + 1).trim();
        return "";
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == 500 && resultCode == -1 && data != null && data.getData() != null) {
            try {
                InputStream inputStream = getParentActivity().getContentResolver().openInputStream(data.getData());
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                inputStream.close();
                parseAwgConfig(sb.toString());
                setProxyType(TYPE_AMNEZIA, true);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && !backward && addingNewProxy) {
            inputFields[FIELD_IP].requestFocus();
            AndroidUtilities.showKeyboard(inputFields[FIELD_IP]);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        final ThemeDescription.ThemeDescriptionDelegate delegate = () -> {
            if (shareCell != null && (shareDoneAnimator == null || !shareDoneAnimator.isRunning())) {
                shareCell.setTextColor(shareDoneEnabled ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4) : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            }
            if (inputFields != null) {
                for (int i = 0; i < inputFields.length; i++) {
                    inputFields[i].setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField),
                            Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated),
                            Theme.getColor(Theme.key_text_RedRegular));
                }
            }
        };
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(scrollView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));
        arrayList.add(new ThemeDescription(inputFieldsContainer, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(linearLayout2, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        arrayList.add(new ThemeDescription(shareCell, ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(shareCell, ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_listSelector));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, delegate, Theme.key_windowBackgroundWhiteBlueText4));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, delegate, Theme.key_windowBackgroundWhiteGrayText2));

        arrayList.add(new ThemeDescription(pasteCell, ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(pasteCell, ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_listSelector));
        arrayList.add(new ThemeDescription(pasteCell, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText4));

        for (int a = 0; a < typeCell.length; a++) {
            arrayList.add(new ThemeDescription(typeCell[a], ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_windowBackgroundWhite));
            arrayList.add(new ThemeDescription(typeCell[a], ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_listSelector));
            arrayList.add(new ThemeDescription(typeCell[a], 0, new Class[]{RadioCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(typeCell[a], ThemeDescription.FLAG_CHECKBOX, new Class[]{RadioCell.class}, new String[]{"radioButton"}, null, null, null, Theme.key_radioBackground));
            arrayList.add(new ThemeDescription(typeCell[a], ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{RadioCell.class}, new String[]{"radioButton"}, null, null, null, Theme.key_radioBackgroundChecked));
        }

        if (inputFields != null) {
            for (int a = 0; a < inputFields.length; a++) {
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_HINTTEXTCOLOR | ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_CURSORCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
                arrayList.add(new ThemeDescription(null, 0, null, null, null, delegate, Theme.key_windowBackgroundWhiteInputField));
                arrayList.add(new ThemeDescription(null, 0, null, null, null, delegate, Theme.key_windowBackgroundWhiteInputFieldActivated));
                arrayList.add(new ThemeDescription(null, 0, null, null, null, delegate, Theme.key_text_RedRegular));
            }
        }
        arrayList.add(new ThemeDescription(headerCell, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(headerCell, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        for (int a = 0; a < sectionCell.length; a++) {
            if (sectionCell[a] != null) {
                arrayList.add(new ThemeDescription(sectionCell[a], ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
            }
        }
        for (int i = 0; i < bottomCells.length; i++) {
            if (bottomCells[i] != null) {
                arrayList.add(new ThemeDescription(bottomCells[i], ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
                arrayList.add(new ThemeDescription(bottomCells[i], 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
                arrayList.add(new ThemeDescription(bottomCells[i], ThemeDescription.FLAG_LINKCOLOR, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));
            }
        }

        return arrayList;
    }
}