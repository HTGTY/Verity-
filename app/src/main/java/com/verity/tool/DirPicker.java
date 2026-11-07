package com.verity.tool;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.verity.tool.util.McPaths;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描 versions 下的全部游戏目录并逐一列出，不限制版本号。
 * 快捷配置与幻觉修复共用。
 */
public final class DirPicker {

    /** 列表超过该数量时折叠余下条目。 */
    public static final int COLLAPSE_LIMIT = 5;

    private DirPicker() {
    }

    public interface Callback {
        void onPick(McPaths.Entry entry);
    }

    /** 全部展开。 */
    public static void render(Activity activity, LinearLayout container, TextView hint,
                              List<McPaths.Entry> entries, Callback callback) {
        render(activity, container, hint, entries, callback, 0);
    }

    /**
     * @param collapsedLimit 超过该数量时其余条目折叠，0 或负数表示全部展开。
     */
    public static void render(Activity activity, LinearLayout container, TextView hint,
                              List<McPaths.Entry> entries, Callback callback, int collapsedLimit) {
        container.removeAllViews();
        if (entries.isEmpty()) {
            hint.setText(McPaths.rootReadable() ? R.string.dir_empty : R.string.dir_denied);
        } else {
            hint.setText(activity.getString(R.string.choose_game_dir_desc, entries.size()));
        }
        hint.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(activity);
        int shown = collapsedLimit > 0 ? Math.min(collapsedLimit, entries.size()) : entries.size();
        List<View> folded = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            View item = build(activity, inflater, entries.get(i), callback);
            if (i >= shown) {
                item.setVisibility(View.GONE);
                folded.add(item);
            }
            container.addView(item);
        }
        if (!folded.isEmpty()) {
            addToggle(activity, inflater, container, folded);
        }
    }

    /** 折叠区下方的展开/收起按钮。 */
    private static void addToggle(Activity activity, LayoutInflater inflater,
                                  LinearLayout container, List<View> folded) {
        MaterialButton button = (MaterialButton) inflater.inflate(
                R.layout.view_dir_more, container, false);
        button.setText(activity.getString(R.string.dir_expand, folded.size()));
        button.setOnClickListener(v -> {
            boolean expand = folded.get(0).getVisibility() != View.VISIBLE;
            for (View item : folded) {
                item.setVisibility(expand ? View.VISIBLE : View.GONE);
            }
            if (expand) {
                button.setText(R.string.dir_collapse);
            } else {
                button.setText(activity.getString(R.string.dir_expand, folded.size()));
            }
        });
        container.addView(button);
    }

    /** 任意目录都可以点，是否能配置交由下一步校验。 */
    private static View build(Activity activity, LayoutInflater inflater,
                              McPaths.Entry entry, Callback callback) {
        View view = inflater.inflate(R.layout.item_game_dir, null, false);
        ((TextView) view.findViewById(R.id.dir_name)).setText(entry.name);
        TextView desc = view.findViewById(R.id.dir_desc);
        desc.setText(entry.configurable ? R.string.dir_configurable : R.string.dir_not_configurable);
        view.setOnClickListener(v -> callback.onPick(entry));
        return view;
    }
}
