package com.w2016561536.vcam;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private Switch force_show_switch;
    private Switch disable_switch;
    private Switch play_sound_switch;
    private Switch force_private_dir;
    private Switch disable_toast_switch;
    private Switch show_fps_switch;
    private Switch show_info_switch;
    private Switch mute_audio_switch;
    private Button repo_button;
    private Button btn_select_video;
    private TextView tv_current_video;
    private EditText et_fps_override;
    private EditText et_loop_delay;
    private SeekBar seekbar_volume;
    private TextView tv_volume;
    
    private ConfigManager configManager;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show();
            } else {
                File camera_dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/");
                if (!camera_dir.exists()) {
                    camera_dir.mkdir();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sync_statue_with_files();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configManager = ConfigManager.getInstance(getApplicationContext());

        initViews();
        setupListeners();
        sync_statue_with_files();
        updateVideoDisplay();
    }

    private void initViews() {
        repo_button = findViewById(R.id.button);
        force_show_switch = findViewById(R.id.switch1);
        disable_switch = findViewById(R.id.switch2);
        play_sound_switch = findViewById(R.id.switch3);
        force_private_dir = findViewById(R.id.switch4);
        disable_toast_switch = findViewById(R.id.switch5);
        show_fps_switch = findViewById(R.id.switch6);
        show_info_switch = findViewById(R.id.switch7);
        mute_audio_switch = findViewById(R.id.switch8);
        btn_select_video = findViewById(R.id.btn_select_video);
        tv_current_video = findViewById(R.id.tv_current_video);
        et_fps_override = findViewById(R.id.et_fps_override);
        et_loop_delay = findViewById(R.id.et_loop_delay);
        seekbar_volume = findViewById(R.id.seekbar_volume);
        tv_volume = findViewById(R.id.tv_volume);
    }

    private void setupListeners() {
        repo_button.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://github.com/w2016561536/android_virtual_cam");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        Button repo_button_chinamainland = findViewById(R.id.button2);
        repo_button_chinamainland.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://gitee.com/w2016561536/android_virtual_cam");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        btn_select_video.setOnClickListener(v -> showVideoSelectionDialog());

        et_fps_override.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int fps = Integer.parseInt(et_fps_override.getText().toString());
                    configManager.setFpsOverride(fps);
                } catch (NumberFormatException e) {
                    configManager.setFpsOverride(0);
                }
            }
        });

        et_loop_delay.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int delay = Integer.parseInt(et_loop_delay.getText().toString());
                    configManager.setLoopDelay(delay);
                } catch (NumberFormatException e) {
                    configManager.setLoopDelay(0);
                }
            }
        });

        seekbar_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_volume.setText("Volume: " + progress + "%");
                if (fromUser) {
                    configManager.setAudioVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        setupSwitchListener(force_show_switch, "force_show.jpg");
        setupSwitchListener(disable_switch, "disable.jpg");
        setupSwitchListener(play_sound_switch, "no-silent.jpg");
        setupSwitchListener(force_private_dir, "private_dir.jpg");
        setupSwitchListener(disable_toast_switch, "no_toast.jpg");

        show_fps_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                configManager.setShowFpsEnabled(b);
            }
        });

        show_info_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                configManager.setShowInfoOverlay(b);
            }
        });

        mute_audio_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                configManager.setMuted(b);
            }
        });
    }

    private void setupSwitchListener(Switch switchControl, String fileName) {
        switchControl.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File controlFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/" + fileName);
                    if (controlFile.exists() != b) {
                        if (b) {
                            try {
                                controlFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            controlFile.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });
    }

    private void showVideoSelectionDialog() {
        String[] videos = configManager.getAvailableVideos();
        
        if (videos == null || videos.length == 0) {
            Toast.makeText(this, R.string.no_videos_found, Toast.LENGTH_SHORT).show();
            return;
        }

        int currentIndex = configManager.getVideoIndex();
        if (currentIndex >= videos.length) {
            currentIndex = 0;
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.video_selection)
            .setSingleChoiceItems(videos, currentIndex, (dialog, which) -> {
                configManager.setVideoIndex(which);
                updateVideoDisplay();
                dialog.dismiss();
            })
            .setNegativeButton(R.string.negative, null)
            .show();
    }

    private void updateVideoDisplay() {
        String[] videos = configManager.getAvailableVideos();
        int currentIndex = configManager.getVideoIndex();
        
        if (videos != null && videos.length > 0 && currentIndex < videos.length) {
            tv_current_video.setText(getString(R.string.current_video, videos[currentIndex]));
        } else {
            tv_current_video.setText("Current: virtual.mp4");
        }
    }

    private void request_permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                    || this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.permission_lack_warn);
                builder.setMessage(R.string.permission_description);

                builder.setNegativeButton(R.string.negative, (dialogInterface, i) -> Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show());

                builder.setPositiveButton(R.string.positive, (dialogInterface, i) -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1));
                builder.show();
            }
        }
    }

    private boolean has_permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED
                    && this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED;
        }
        return true;
    }


    private void sync_statue_with_files() {
        Log.d(this.getApplication().getPackageName(), "【VCAM】[sync]同步开关状态");

        if (!has_permission()) {
            request_permission();
        } else {
            File camera_dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1");
            if (!camera_dir.exists()) {
                camera_dir.mkdir();
            }
        }

        File disable_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/disable.jpg");
        disable_switch.setChecked(disable_file.exists());

        File force_show_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/force_show.jpg");
        force_show_switch.setChecked(force_show_file.exists());

        File play_sound_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/no-silent.jpg");
        play_sound_switch.setChecked(play_sound_file.exists());

        File force_private_dir_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/private_dir.jpg");
        force_private_dir.setChecked(force_private_dir_file.exists());

        File disable_toast_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/no_toast.jpg");
        disable_toast_switch.setChecked(disable_toast_file.exists());

        show_fps_switch.setChecked(configManager.isShowFpsEnabled());
        show_info_switch.setChecked(configManager.isShowInfoOverlay());
        mute_audio_switch.setChecked(configManager.isMuted());

        et_fps_override.setText(String.valueOf(configManager.getFpsOverride()));
        et_loop_delay.setText(String.valueOf(configManager.getLoopDelay()));
        seekbar_volume.setProgress(configManager.getIntProperty(ConfigManager.KEY_AUDIO_VOLUME, 100));
        tv_volume.setText("Volume: " + configManager.getIntProperty(ConfigManager.KEY_AUDIO_VOLUME, 100) + "%");
    }


}
