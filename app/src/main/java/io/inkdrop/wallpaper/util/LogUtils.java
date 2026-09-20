package io.inkdrop.wallpaper.util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class LogUtils {

    private static Context appContext;
    private static boolean isDebug = false;
    private static int keepDays = 7;
    private static final String PREFS_NAME = "log_prefs";
    private static final String KEY_KEEP_DAYS = "keep_days";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat LOG_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final List<Pattern> sensitivePatterns = new ArrayList<>();

    static {
        sensitivePatterns.add(Pattern.compile("token[\"\\s]*[:=][\"\\s]*([^\"\\s,}]+)"));
        sensitivePatterns.add(Pattern.compile("password[\"\\s]*[:=][\"\\s]*([^\"\\s,}]+)"));
        sensitivePatterns.add(Pattern.compile("api[_-]?key[\"\\s]*[:=][\"\\s]*([^\"\\s,}]+)"));
    }

    private LogUtils() {}

    public static void init(Application app) {
        appContext = app.getApplicationContext();
        isDebug = (app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        keepDays = prefs.getInt(KEY_KEEP_DAYS, 7);
        clearOldLogs();
    }

    public static void setDebug(boolean debug) {
        isDebug = debug;
    }

    // ========== 文件写入 ==========

    private static void writeToFile(String level, String tag, String message, Throwable throwable) {
        if (appContext == null) return;
        if (!isDebug && !"w".equals(level) && !"e".equals(level)) return;

        try {
            File logDir = new File(appContext.getFilesDir(), "logs");
            if (!logDir.exists()) logDir.mkdirs();

            String dateStr = DATE_FORMAT.format(new Date());
            File logFile = new File(logDir, "app_" + dateStr + ".log");

            String timestamp = LOG_FORMAT.format(new Date());
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(level.toUpperCase()).append("] ");
            sb.append("[").append(timestamp).append("] ");
            sb.append("[").append(tag).append("] ");
            sb.append(message);
            if (throwable != null) {
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                sb.append("\n").append(sw.toString());
            }
            sb.append("\n");

            FileWriter writer = new FileWriter(logFile, true);
            writer.write(sb.toString());
            writer.close();
        } catch (IOException ignored) {
        }
    }

    // ========== d 调试日志（Release 不写入文件）==========

    public static void d(String message) {
        String tag = getTag();
        String sanitized = sanitize(message);
        if (isDebug) Log.d(tag, sanitized);
        writeToFile("d", tag, sanitized, null);
    }

    public static void d(String format, Object... args) {
        String tag = getTag();
        String sanitized = sanitize(String.format(format, args));
        if (isDebug) Log.d(tag, sanitized);
        writeToFile("d", tag, sanitized, null);
    }

    public static void d(String tag, String message) {
        String sanitized = sanitize(message);
        if (isDebug) Log.d(tag, sanitized);
        writeToFile("d", tag, sanitized, null);
    }

    // ========== i 信息日志（Release 不写入文件）==========

    public static void i(String message) {
        String tag = getTag();
        String sanitized = sanitize(message);
        if (isDebug) Log.i(tag, sanitized);
        writeToFile("i", tag, sanitized, null);
    }

    public static void i(String format, Object... args) {
        String tag = getTag();
        String sanitized = sanitize(String.format(format, args));
        if (isDebug) Log.i(tag, sanitized);
        writeToFile("i", tag, sanitized, null);
    }

    public static void i(String tag, String message) {
        String sanitized = sanitize(message);
        if (isDebug) Log.i(tag, sanitized);
        writeToFile("i", tag, sanitized, null);
    }

    // ========== w 警告日志（始终写入文件）==========

    public static void w(String message) {
        String tag = getTag();
        String sanitized = sanitize(message);
        Log.w(tag, sanitized);
        writeToFile("w", tag, sanitized, null);
    }

    public static void w(String format, Object... args) {
        String tag = getTag();
        String sanitized = sanitize(String.format(format, args));
        Log.w(tag, sanitized);
        writeToFile("w", tag, sanitized, null);
    }

    public static void w(String tag, String message) {
        String sanitized = sanitize(message);
        Log.w(tag, sanitized);
        writeToFile("w", tag, sanitized, null);
    }

    public static void w(String message, Throwable throwable) {
        String tag = getTag();
        String sanitized = sanitize(message);
        Log.w(tag, sanitized, throwable);
        writeToFile("w", tag, sanitized, throwable);
    }

    // ========== e 错误日志（始终写入文件）==========

    public static void e(String message) {
        String tag = getTag();
        String sanitized = sanitize(message);
        Log.e(tag, sanitized);
        writeToFile("e", tag, sanitized, null);
    }

    public static void e(String format, Object... args) {
        String tag = getTag();
        String sanitized = sanitize(String.format(format, args));
        Log.e(tag, sanitized);
        writeToFile("e", tag, sanitized, null);
    }

    public static void e(String tag, String message) {
        String sanitized = sanitize(message);
        Log.e(tag, sanitized);
        writeToFile("e", tag, sanitized, null);
    }

    public static void e(String message, Throwable throwable) {
        String tag = getTag();
        String sanitized = sanitize(message);
        Log.e(tag, sanitized, throwable);
        writeToFile("e", tag, sanitized, throwable);
    }

    public static void e(String tag, String message, Throwable throwable) {
        String sanitized = sanitize(message);
        Log.e(tag, sanitized, throwable);
        writeToFile("e", tag, sanitized, throwable);
    }

    // ========== 敏感信息处理 ==========

    public static void setSensitivePatterns(List<String> patterns) {
        sensitivePatterns.clear();
        for (String p : patterns) {
            sensitivePatterns.add(Pattern.compile(p));
        }
    }

    public static String sanitize(String message) {
        if (message == null) return null;
        String result = message;
        for (Pattern pattern : sensitivePatterns) {
            result = pattern.matcher(result).replaceAll("$1:***");
        }
        return result;
    }

    private static String getTag() {
        StackTraceElement element = Thread.currentThread().getStackTrace()[4];
        String className = element.getClassName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    // ========== 日志读取 ==========

    public static List<File> getLogFiles(Date startDate, Date endDate) {
        List<File> result = new ArrayList<>();
        if (appContext == null) return result;

        File logDir = new File(appContext.getFilesDir(), "logs");
        if (!logDir.exists()) return result;

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        File[] files = logDir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith("app_") || !name.endsWith(".log")) continue;
            try {
                String dateStr = name.substring(4, 14);
                Date fileDate = DATE_FORMAT.parse(dateStr);
                if (fileDate != null && !fileDate.before(startCal.getTime()) && !fileDate.after(endCal.getTime())) {
                    result.add(file);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public static String getLogContent(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    public static String getAllLogContent(Date startDate, Date endDate) {
        List<File> files = getLogFiles(startDate, endDate);
        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            sb.append(getLogContent(file));
        }
        return sb.toString();
    }

    // ========== 日志清理 ==========

    public static void clearOldLogs() {
        if (appContext == null) return;
        File logDir = new File(appContext.getFilesDir(), "logs");
        if (!logDir.exists()) return;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -keepDays);
        Date cutoff = cal.getTime();

        File[] files = logDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith("app_") || !name.endsWith(".log")) continue;
            try {
                String dateStr = name.substring(4, 14);
                Date fileDate = DATE_FORMAT.parse(dateStr);
                if (fileDate != null && fileDate.before(cutoff)) {
                    file.delete();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static long getLogDirSize() {
        if (appContext == null) return 0;
        File logDir = new File(appContext.getFilesDir(), "logs");
        if (!logDir.exists()) return 0;
        long size = 0;
        File[] files = logDir.listFiles();
        if (files != null) {
            for (File file : files) {
                size += file.length();
            }
        }
        return size;
    }

    public static int getKeepDays() {
        return keepDays;
    }

    public static void setKeepDays(int days) {
        keepDays = days;
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_KEEP_DAYS, days).apply();
        }
        clearOldLogs();
    }
}
