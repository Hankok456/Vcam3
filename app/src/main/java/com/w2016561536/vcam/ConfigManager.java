package com.w2016561536.vcam;

import android.content.Context;
import android.os.Environment;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigManager {
    private static final String TAG = "VCAM_Config";
    private static final String CONFIG_FILE = "vcam_config.properties";
    
    private static ConfigManager instance;
    private final String configDir;
    private Properties properties;
    private Map<String, String> perAppVideoMap;
    
    public static final String KEY_FPS_OVERRIDE = "fps_override";
    public static final String KEY_SHOW_FPS = "show_fps";
    public static final String KEY_AUDIO_VOLUME = "audio_volume";
    public static final String KEY_MUTE_AUDIO = "mute_audio";
    public static final String KEY_SHOW_INFO_OVERLAY = "show_info_overlay";
    public static final String KEY_VIDEO_INDEX = "video_index";
    public static final String KEY_LOOP_DELAY = "loop_delay";
    
    private ConfigManager(Context context) {
        String baseDir = getBaseDir(context);
        configDir = baseDir + "/";
        properties = new Properties();
        perAppVideoMap = new HashMap<>();
        loadConfig();
    }
    
    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigManager(context);
        }
        return instance;
    }
    
    private String getBaseDir(Context context) {
        if (context != null) {
            File externalDir = context.getExternalFilesDir(null);
            if (externalDir != null && externalDir.canWrite()) {
                return externalDir.getAbsolutePath() + "/VCAM";
            }
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1";
    }
    
    public String getConfigDir() {
        return configDir;
    }
    
    public void loadConfig() {
        File configFile = new File(configDir, CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                loadPerAppVideoMap();
                Log.d(TAG, "Config loaded from " + configFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Failed to load config: " + e.getMessage());
            }
        }
        
        createControlFiles();
    }
    
    public void saveConfig() {
        File configFile = new File(configDir, CONFIG_FILE);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "VCAM Configuration");
            savePerAppVideoMap();
            Log.d(TAG, "Config saved to " + configFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save config: " + e.getMessage());
        }
    }
    
    private void createControlFiles() {
        File configFolder = new File(configDir);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
    }
    
    private void loadPerAppVideoMap() {
        perAppVideoMap.clear();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("app_video_")) {
                String packageName = key.substring("app_video_".length());
                perAppVideoMap.put(packageName, properties.getProperty(key));
            }
        }
    }
    
    private void savePerAppVideoMap() {
        for (Map.Entry<String, String> entry : perAppVideoMap.entrySet()) {
            properties.setProperty("app_video_" + entry.getKey(), entry.getValue());
        }
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getBoolProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }
    
    public void setProperty(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
        saveConfig();
    }
    
    public void setProperty(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
        saveConfig();
    }
    
    public int getFpsOverride() {
        return getIntProperty(KEY_FPS_OVERRIDE, 0);
    }
    
    public void setFpsOverride(int fps) {
        setProperty(KEY_FPS_OVERRIDE, fps);
    }
    
    public boolean isShowFpsEnabled() {
        return getBoolProperty(KEY_SHOW_FPS, false);
    }
    
    public void setShowFpsEnabled(boolean enabled) {
        setProperty(KEY_SHOW_FPS, enabled);
    }
    
    public float getAudioVolume() {
        return getIntProperty(KEY_AUDIO_VOLUME, 100) / 100f;
    }
    
    public void setAudioVolume(int volume) {
        setProperty(KEY_AUDIO_VOLUME, Math.max(0, Math.min(100, volume)));
    }
    
    public boolean isMuted() {
        return getBoolProperty(KEY_MUTE_AUDIO, false);
    }
    
    public void setMuted(boolean muted) {
        setProperty(KEY_MUTE_AUDIO, muted);
    }
    
    public boolean isShowInfoOverlay() {
        return getBoolProperty(KEY_SHOW_INFO_OVERLAY, false);
    }
    
    public void setShowInfoOverlay(boolean show) {
        setProperty(KEY_SHOW_INFO_OVERLAY, show);
    }
    
    public int getVideoIndex() {
        return getIntProperty(KEY_VIDEO_INDEX, 0);
    }
    
    public void setVideoIndex(int index) {
        setProperty(KEY_VIDEO_INDEX, index);
    }
    
    public int getLoopDelay() {
        return getIntProperty(KEY_LOOP_DELAY, 0);
    }
    
    public void setLoopDelay(int delayMs) {
        setProperty(KEY_LOOP_DELAY, delayMs);
    }
    
    public String getPerAppVideo(String packageName) {
        return perAppVideoMap.get(packageName);
    }
    
    public void setPerAppVideo(String packageName, String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            perAppVideoMap.remove(packageName);
            properties.remove("app_video_" + packageName);
        } else {
            perAppVideoMap.put(packageName, videoPath);
            properties.setProperty("app_video_" + packageName, videoPath);
        }
        saveConfig();
    }
    
    public boolean hasPerAppVideo(String packageName) {
        return perAppVideoMap.containsKey(packageName);
    }
    
    public String getVideoPathForApp(String packageName, String defaultPath) {
        if (perAppVideoMap.containsKey(packageName)) {
            String videoPath = perAppVideoMap.get(packageName);
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                return videoPath;
            }
        }
        return defaultPath;
    }
    
    public String[] getAvailableVideos() {
        File cameraDir = new File(configDir);
        if (!cameraDir.exists()) {
            return new String[0];
        }
        
        File[] mp4Files = cameraDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".mp4") && !name.startsWith("."));
        
        if (mp4Files == null || mp4Files.length == 0) {
            return new String[0];
        }
        
        String[] videos = new String[mp4Files.length];
        for (int i = 0; i < mp4Files.length; i++) {
            videos[i] = mp4Files[i].getName();
        }
        java.util.Arrays.sort(videos);
        return videos;
    }
    
    public static String getDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("VCAM - Virtual Camera\n");
        info.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        info.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        info.append("VCAM Version: 4.4");
        return info.toString();
    }
    
    public static String getCameraInfo(int width, int height, int fps) {
        StringBuilder info = new StringBuilder();
        info.append("Resolution: ").append(width).append("x").append(height).append("\n");
        info.append("FPS: ").append(fps).append("\n");
        info.append("Format: YUV_420_888");
        return info.toString();
    }
}
