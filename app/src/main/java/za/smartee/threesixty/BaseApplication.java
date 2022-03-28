package za.smartee.threesixty;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.flattener.PatternFlattener;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.naming.ChangelessFileNameGenerator;
import za.smartee.threesixty.utils.IOUtils;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.moko.ble.lib.log.ClearLogBackStrategy;
import za.smartee.support.MokoSupport;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class BaseApplication extends Application {

    private static final String TAG = "smarteeThreeSixty";
    private static final String LOG_FILE = "smarteeThreeSixty.txt";
    private static final String LOG_FOLDER = "smarteeThreeSixty";
    public static String PATH_LOGCAT;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(getApplicationContext());

            Log.i("Tutorial", "Initialized Amplify");
        } catch (AmplifyException e) {
            Log.e("Tutorial", "Could not initialize Amplify", e);
        }
        initXLog();
        MokoSupport.getInstance().init(getApplicationContext());
        Thread.setDefaultUncaughtExceptionHandler(new BTUncaughtExceptionHandler());
        AppUpdater appUpdater = new AppUpdater(this)
                .setDisplay(Display.DIALOG)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON("https://github.com/IconixES/Smartee360Prod/update-changelog.json");
        appUpdater.start();
    }

    private void initXLog() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + LOG_FOLDER;
        } else {
            PATH_LOGCAT = getFilesDir().getAbsolutePath() + File.separator + LOG_FOLDER;
        }
        Printer filePrinter = new FilePrinter.Builder(PATH_LOGCAT)
                .fileNameGenerator(new ChangelessFileNameGenerator(LOG_FILE))
                .backupStrategy(new ClearLogBackStrategy())
                .flattener(new PatternFlattener("{d yyyy-MM-dd HH:mm:ss} {l}/{t}: {m}"))
                .build();
        LogConfiguration config = new LogConfiguration.Builder()
                .tag(TAG)
                .logLevel(LogLevel.ALL)
                .build();
        XLog.init(config, new AndroidPrinter(), filePrinter);
    }

    public class BTUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private static final String LOGTAG = "BTUncaughtExceptionHandler";

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // 读取stacktrace信息
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            ex.printStackTrace(printWriter);
            StringBuffer errorReport = new StringBuffer();
            // 获取packagemanager的实例
            PackageManager packageManager = getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = null;
            try {
                packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (packInfo != null) {
                String version = packInfo.versionName;
                errorReport.append(version);
                errorReport.append("\r\n");
            }
            errorReport.append(result.toString());
            IOUtils.setCrashLog(errorReport.toString());
            XLog.e("uncaughtException errorReport=" + errorReport);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}
