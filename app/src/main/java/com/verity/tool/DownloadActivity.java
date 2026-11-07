package com.verity.tool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/** 下载页：进度条 + 日志，可终止或转后台。 */
public class DownloadActivity extends BaseActivity implements DownloadService.Listener {

    public static final String EXTRA_VERSION = "version";

    private LinearProgressIndicator progress;
    private TextView percent;
    private TextView logText;
    private TextView versionText;
    private android.widget.ScrollView logScroll;
    private MaterialButton btnStop;
    private MaterialButton btnBackground;

    private DownloadService service;
    private boolean bound;
    private String version;
    private boolean finished;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DownloadService.LocalBinder) binder).service();
            bound = true;
            service.addListener(DownloadActivity.this);
            if (!service.isRunning() && !finished) {
                // 服务已结束（例如进程内重进页面），重新开始一次
                DownloadService.start(DownloadActivity.this, version);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        version = getIntent().getStringExtra(EXTRA_VERSION);
        if (version == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.title_bar)).setText(R.string.dl_title);
        findViewById(R.id.btn_back).setOnClickListener(v -> background());
        versionText = findViewById(R.id.dl_version);
        progress = findViewById(R.id.dl_progress);
        percent = findViewById(R.id.dl_percent);
        logText = findViewById(R.id.dl_log_text);
        logScroll = findViewById(R.id.dl_log_scroll);
        btnStop = findViewById(R.id.btn_stop);
        btnBackground = findViewById(R.id.btn_background);

        versionText.setText(version);

        btnStop.setOnClickListener(v -> confirmStop());
        btnBackground.setOnClickListener(v -> background());

        bindService(new Intent(this, DownloadService.class), connection, Context.BIND_AUTO_CREATE);
        DownloadService.start(this, version);
    }

    private void confirmStop() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dl_title)
                .setMessage(R.string.dl_stop)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.dl_stop, (d, w) -> {
                    if (service != null) {
                        service.cancel();
                    }
                })
                .show();
    }

    /** 返回键 / 后台按钮：页面关掉，服务继续跑。 */
    private void background() {
        if (!finished && service != null && service.isRunning()) {
            Toast.makeText(this, R.string.dl_background, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        background();
    }

    @Override
    protected void onDestroy() {
        if (bound) {
            if (service != null) {
                service.removeListener(this);
            }
            unbindService(connection);
            bound = false;
        }
        super.onDestroy();
    }

    // ---- DownloadService.Listener ----

    @Override
    public void onLog(String line) {
        runOnUiThread(() -> {
            logText.append(line);
            logText.append("\n");
            logScroll.post(() -> logScroll.fullScroll(android.view.View.FOCUS_DOWN));
        });
    }

    @Override
    public void onProgress(int done, int total) {
        runOnUiThread(() -> {
            if (total > 0) {
                int pct = (int) (done * 100.0 / total);
                progress.setIndeterminate(false);
                progress.setProgress(pct);
                percent.setText(getString(R.string.dl_progress, mb(done), mb(total), pct));
            } else {
                progress.setIndeterminate(true);
                percent.setText(getString(R.string.dl_progress_unknown, mb(done)));
            }
        });
    }

    @Override
    public void onDone(String dirName) {
        finished = true;
        runOnUiThread(() -> {
            progress.setIndeterminate(false);
            progress.setProgress(100);
            btnStop.setEnabled(false);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_done)
                    .setMessage(getString(R.string.install_launch, dirName))
                    .setPositiveButton(R.string.ok, (d, w) -> finish())
                    .setOnDismissListener(d -> finish())
                    .show();
        });
    }

    @Override
    public void onFailed(String message) {
        finished = true;
        runOnUiThread(() -> {
            progress.setIndeterminate(false);
            btnStop.setEnabled(false);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_error)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (d, w) -> finish())
                    .show();
        });
    }

    private static String mb(long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(java.util.Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0);
    }
}