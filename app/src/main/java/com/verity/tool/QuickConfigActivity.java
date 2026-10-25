package com.verity.tool;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import com.verity.tool.util.McPaths;
import com.verity.tool.util.ThemeUtils;
import com.verity.tool.util.TomlEditor;
import com.verity.tool.util.Validator;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** 快捷配置：AI 接口置顶，下方是模组主设置与外观设置。 */
public class QuickConfigActivity extends BaseActivity {

    private static final String[] MODEL_PRESETS = {
            "deepseek-v4-flash", "deepseek-v4-pro", "glm-5.2", "glm-4-Flash"
    };
    private static final String[] URL_PRESETS = {
            "https://api.deepseek.com", "https://open.bigmodel.cn/api/paas/v4"
    };

    /** 旧版本可设置的主设置项，顺序与配置文件一致。 */
    private static final String[] LEGACY_TOGGLE_KEYS = {
            "canCrash", "playVideo", "requireVerity", "useNativeTts",
            "trueDarkness", "killVillagers", "showKarma"
    };
    /** 新版本可设置的主设置项。 */
    private static final String[] MODERN_TOGGLE_KEYS = {
            "canCrash", "playVideo", "requireVerity", "trueDarkness",
            "killWildlife", "killEntities", "killVillagers", "showKarma"
    };
    private static final String KEY_DAY_COUNT = "dayCount";
    private static final String KEY_CUSTOM_NAME = "customName";
    private static final String KEY_COLOR_HUE = "colorHue";
    private static final int[] DAY_PRESETS = {1, 3, 5, 7, 10};

    private String version;
    private TomlEditor.Format format;
    private View layoutVersions;
    private View titleVersion;
    private MaterialButton btnNext;
    private MaterialButton btnRedetect;

    private MaterialCardView card1201;
    private MaterialCardView card1211;
    private TextView envStatus;
    private View layoutInputs;
    private TextInputEditText etModel;
    private TextInputEditText etUrl;
    private TextInputEditText etKey;
    private TextInputEditText etDayCount;
    private TextInputEditText etCustomName;
    private Slider sliderHue;
    private TextView hueValue;
    private LinearLayout toggleContainer;
    private final Map<String, MaterialSwitch> toggleSwitches = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_config);

        card1201 = findViewById(R.id.card_v1201);
        card1211 = findViewById(R.id.card_v1211);
        envStatus = findViewById(R.id.env_status);
        layoutInputs = findViewById(R.id.layout_inputs);
        layoutVersions = findViewById(R.id.layout_versions);
        titleVersion = findViewById(R.id.title_version);
        btnNext = findViewById(R.id.btn_next);
        etModel = findViewById(R.id.et_model);
        etUrl = findViewById(R.id.et_url);
        etKey = findViewById(R.id.et_key);
        etDayCount = findViewById(R.id.et_day_count);
        etCustomName = findViewById(R.id.et_custom_name);
        sliderHue = findViewById(R.id.slider_hue);
        hueValue = findViewById(R.id.hue_value);
        toggleContainer = findViewById(R.id.container_toggles);

        card1201.setOnClickListener(v -> selectVersion("1.20.1"));
        card1211.setOnClickListener(v -> selectVersion("1.21.1"));

        btnNext.setOnClickListener(v -> checkEnvironment());

        MaterialButton modelPreset = findViewById(R.id.btn_model_preset);
        modelPreset.setOnClickListener(v -> showPresets(v, MODEL_PRESETS, etModel));
        MaterialButton urlPreset = findViewById(R.id.btn_url_preset);
        urlPreset.setOnClickListener(v -> showPresets(v, URL_PRESETS, etUrl));

        int[] dayButtons = {
                R.id.btn_day_1, R.id.btn_day_3, R.id.btn_day_5,
                R.id.btn_day_7, R.id.btn_day_10
        };
        for (int i = 0; i < dayButtons.length; i++) {
            int days = DAY_PRESETS[i];
            findViewById(dayButtons[i]).setOnClickListener(v ->
                    etDayCount.setText(String.valueOf(days)));
        }

        sliderHue.addOnChangeListener((slider, value, fromUser) ->
                hueValue.setText(getString(R.string.color_hue_value, (int) value)));
        hueValue.setText(getString(R.string.color_hue_value, (int) sliderHue.getValue()));

        btnRedetect = findViewById(R.id.btn_redetect);
        btnRedetect.setOnClickListener(v -> resetToDetectStep());

        setupCollapsible(R.id.header_game, R.id.section_game, R.id.arrow_game);
        setupCollapsible(R.id.header_appearance, R.id.section_appearance, R.id.arrow_appearance);

        MaterialButton confirm = findViewById(R.id.btn_confirm);
        confirm.setOnClickListener(v -> confirmAndWrite());

        refreshVersions();
    }

    private void refreshVersions() {
        boolean has1201 = McPaths.versionInstalled("1.20.1");
        boolean has1211 = McPaths.versionInstalled("1.21.1");

        TextView status1201 = findViewById(R.id.status_v1201);
        TextView status1211 = findViewById(R.id.status_v1211);
        status1201.setText(has1201 ? R.string.version_available : R.string.version_missing);
        status1211.setText(has1211 ? R.string.version_available : R.string.version_missing);

        if (!has1201 && !has1211) {
            envStatus.setText(R.string.version_missing_all);
        } else {
            selectVersion(has1201 ? "1.20.1" : "1.21.1");
        }
    }

    private void selectVersion(String target) {
        if (!McPaths.versionInstalled(target)) {
            Toast.makeText(this, getString(R.string.toast_version_not_installed, target),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        version = target;
        updateCard(card1201, target.equals("1.20.1"));
        updateCard(card1211, target.equals("1.21.1"));
        hideWithAnimation(layoutInputs);
        showWithAnimation(btnNext);
        envStatus.setText(getString(R.string.env_selected, target));
    }

    private void updateCard(MaterialCardView card, boolean selected) {
        card.setStrokeWidth(selected ? dp(2) : dp(1));
        card.setStrokeColor(selected
                ? ThemeUtils.color(this, com.verity.tool.R.attr.colorPrimary)
                : ThemeUtils.color(this, com.verity.tool.R.attr.colorOutline));
        card.setCardBackgroundColor(selected
                ? ThemeUtils.color(this, com.verity.tool.R.attr.colorPrimaryContainer)
                : ThemeUtils.color(this, com.verity.tool.R.attr.colorSurface));
    }

    private void checkEnvironment() {
        if (version == null) {
            Toast.makeText(this, R.string.toast_choose_version_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!McPaths.versionInstalled(version)) {
            envStatus.setText(getString(R.string.env_version_not_installed, version));
            return;
        }
        File configDir = McPaths.configDir(version);
        if (!configDir.isDirectory()) {
            envStatus.setText(R.string.env_no_config_dir);
            layoutInputs.setVisibility(View.GONE);
            return;
        }
        File toml = McPaths.commonToml(version);
        if (!toml.isFile()) {
            envStatus.setText(R.string.env_no_toml);
            layoutInputs.setVisibility(View.GONE);
            return;
        }

        format = TomlEditor.detectFormat(toml);
        String label = getString(format == TomlEditor.Format.LEGACY
                ? R.string.config_format_legacy : R.string.config_format_modern);
        envStatus.setText(getString(R.string.env_found, label, toml.getParentFile().getName()));
        hideWithAnimation(titleVersion);
        hideWithAnimation(layoutVersions);
        hideWithAnimation(btnNext);
        showWithAnimation(btnRedetect);
        showWithAnimation(layoutInputs);
        prefill(toml, format);
    }

    private void prefill(File toml, TomlEditor.Format detected) {
        TomlEditor.AiConfig config = TomlEditor.readAi(toml, detected);
        if (config.model != null && !config.model.isEmpty()) {
            etModel.setText(config.model);
        }
        if (config.url != null && !config.url.isEmpty()) {
            etUrl.setText(config.url);
        }
        if (config.key != null && !config.key.isEmpty() && !"这里是密钥".equals(config.key)) {
            etKey.setText(config.key);
        }

        String togglesSection = TomlEditor.togglesSection(detected);
        Map<String, String> values =
                TomlEditor.readValues(toml, detected, togglesSection, toggleKeys(detected));
        String days = TomlEditor.readValue(toml, detected, togglesSection, KEY_DAY_COUNT);
        if (days != null && !days.isEmpty()) {
            etDayCount.setText(days);
        }
        buildToggles(detected, values);

        String customSection = TomlEditor.customSection(detected);
        String name = TomlEditor.readValue(toml, detected, customSection, KEY_CUSTOM_NAME);
        if (name != null && !name.isEmpty()) {
            etCustomName.setText(name);
        }
        String hue = TomlEditor.readValue(toml, detected, customSection, KEY_COLOR_HUE);
        if (hue != null && !hue.trim().isEmpty()) {
            try {
                sliderHue.setValue(Float.parseFloat(hue.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String[] toggleKeys(TomlEditor.Format detected) {
        return detected == TomlEditor.Format.LEGACY ? LEGACY_TOGGLE_KEYS : MODERN_TOGGLE_KEYS;
    }

    private void buildToggles(TomlEditor.Format detected, Map<String, String> values) {
        toggleContainer.removeAllViews();
        toggleSwitches.clear();
        for (String key : toggleKeys(detected)) {
            View view = getLayoutInflater().inflate(R.layout.item_toggle, toggleContainer, false);
            ((TextView) view.findViewById(R.id.toggle_title)).setText(titleOf(key));
            ((TextView) view.findViewById(R.id.toggle_desc)).setText(descOf(key));
            MaterialSwitch item = view.findViewById(R.id.toggle_switch);
            item.setChecked(!"false".equals(values.get(key)));
            toggleSwitches.put(key, item);
            toggleContainer.addView(view);
        }
    }

    private int titleOf(String key) {
        switch (key) {
            case "canCrash":
                return R.string.toggle_can_crash;
            case "playVideo":
                return R.string.toggle_play_video;
            case "requireVerity":
                return R.string.toggle_require_verity;
            case "useNativeTts":
                return R.string.toggle_use_native_tts;
            case "trueDarkness":
                return R.string.toggle_true_darkness;
            case "killWildlife":
                return R.string.toggle_kill_wildlife;
            case "killEntities":
                return R.string.toggle_kill_entities;
            case "killVillagers":
                return R.string.toggle_kill_villagers;
            default:
                return R.string.toggle_show_karma;
        }
    }

    private int descOf(String key) {
        switch (key) {
            case "canCrash":
                return R.string.key_can_crash;
            case "playVideo":
                return R.string.key_play_video;
            case "requireVerity":
                return R.string.key_require_verity;
            case "useNativeTts":
                return R.string.key_use_native_tts;
            case "trueDarkness":
                return R.string.key_true_darkness;
            case "killWildlife":
                return R.string.key_kill_wildlife;
            case "killEntities":
                return R.string.key_kill_entities;
            case "killVillagers":
                return R.string.key_kill_villagers;
            default:
                return R.string.key_show_karma;
        }
    }

    private void showPresets(View anchor, String[] items, TextInputEditText target) {
        PopupMenu menu = new PopupMenu(this, anchor);
        for (String item : items) {
            menu.getMenu().add(item);
        }
        menu.setOnMenuItemClickListener(item -> {
            target.setText(item.getTitle());
            return true;
        });
        menu.show();
    }

    private void confirmAndWrite() {
        if (version == null) {
            Toast.makeText(this, R.string.toast_choose_version_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (format == null) {
            Toast.makeText(this, R.string.toast_detect_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String model = text(etModel);
        String url = text(etUrl);
        String key = text(etKey);
        String days = text(etDayCount);
        String name = text(etCustomName);

        String error = Validator.validate(model, url, key);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isDaysValid(days)) {
            Toast.makeText(this, R.string.day_count_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        File toml = McPaths.commonToml(version);
        if (!TomlEditor.writeAi(toml, format, model, url, key)) {
            showWriteFailure();
            return;
        }

        Map<String, String> togglesValues = new LinkedHashMap<>();
        for (Map.Entry<String, MaterialSwitch> entry : toggleSwitches.entrySet()) {
            togglesValues.put(entry.getKey(), entry.getValue().isChecked() ? "true" : "false");
        }
        togglesValues.put(KEY_DAY_COUNT, days);
        if (!TomlEditor.writeValues(toml, format, TomlEditor.togglesSection(format),
                togglesValues)) {
            showWriteFailure();
            return;
        }

        Map<String, String> customValues = new LinkedHashMap<>();
        customValues.put(KEY_CUSTOM_NAME, TomlEditor.quote(name));
        customValues.put(KEY_COLOR_HUE, String.valueOf((int) sliderHue.getValue()));
        if (!TomlEditor.writeValues(toml, format, TomlEditor.customSection(format),
                customValues)) {
            showWriteFailure();
            return;
        }

        TomlEditor.AiConfig written = TomlEditor.readAi(toml, format);
        if (!model.equals(written.model) || !url.equals(written.url)) {
            dialog(getString(R.string.title_verify_failed), getString(R.string.verify_failed));
            return;
        }
        String writtenName = TomlEditor.readValue(toml, format,
                TomlEditor.customSection(format), KEY_CUSTOM_NAME);
        if (!name.equals(writtenName)) {
            dialog(getString(R.string.title_verify_failed), getString(R.string.verify_name_failed));
            return;
        }

        String detail = getString(format == TomlEditor.Format.LEGACY
                ? R.string.config_done_legacy : R.string.config_done_modern);
        dialog(getString(R.string.title_done), getString(R.string.done_restart, detail));
    }

    private boolean isDaysValid(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        try {
            int days = Integer.parseInt(value);
            return days >= 0 && days <= 999;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showWriteFailure() {
        dialog(getString(R.string.title_write_failed), getString(R.string.write_failed));
    }

    /** 回到版本选择步骤，方便更换游戏版本后重新检测。 */
    private void resetToDetectStep() {
        hideWithAnimation(layoutInputs);
        hideWithAnimation(btnRedetect);
        showWithAnimation(titleVersion);
        showWithAnimation(layoutVersions);
        showWithAnimation(btnNext);
        format = null;
        envStatus.setText(getString(R.string.env_selected, version));
    }

    private void setupCollapsible(int headerId, int sectionId, int arrowId) {
        View section = findViewById(sectionId);
        ImageView arrow = findViewById(arrowId);
        findViewById(headerId).setOnClickListener(v -> {
            boolean expand = section.getVisibility() != View.VISIBLE;
            if (expand) {
                showWithAnimation(section);
            } else {
                hideWithAnimation(section);
            }
            arrow.animate().rotation(expand ? 90f : 0f).setDuration(200).start();
        });
    }

    private void showWithAnimation(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
    }

    private void hideWithAnimation(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_out_up);
        anim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
        });
        view.startAnimation(anim);
    }

    private static class AnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    private void dialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
