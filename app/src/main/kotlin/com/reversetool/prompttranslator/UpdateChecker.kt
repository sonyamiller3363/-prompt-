package com.reversetool.prompttranslator

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用内更新检查器
 * 从 GitHub Releases 检查更新并下载安装
 */
class UpdateChecker(
    private val activity: Activity,
    private val owner: String,
    private val repo: String
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()

    companion object {
        private const val TAG = "UpdateChecker"
        private const val GITHUB_API_URL = "https://api.github.com/repos/%s/releases/latest"
        private const val PREF_NAME = "app_update"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24小时
    }

    /**
     * 检查是否有更新（后台线程）
     */
    fun checkForUpdate(showNoUpdate: Boolean) {
        // 检查是否在检查间隔内
        if (!showNoUpdate && !shouldCheck()) {
            return
        }

        CheckUpdateTask(showNoUpdate).execute()
    }

    /**
     * 判断是否应该检查更新
     */
    private fun shouldCheck(): Boolean {
        val lastCheck = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_CHECK, 0)
        return System.currentTimeMillis() - lastCheck > CHECK_INTERVAL
    }

    /**
     * 更新最后检查时间
     */
    private fun updateLastCheckTime() {
        activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取当前应用版本号
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = activity.packageManager
                .getPackageInfo(activity.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    /**
     * 获取当前应用版本名
     */
    private fun getCurrentVersionName(): String {
        return try {
            val pInfo = activity.packageManager
                .getPackageInfo(activity.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(versionName: String, downloadUrl: String, body: String?) {
        mainHandler.post {
            try {
                val builder = AlertDialog.Builder(activity)

                // 创建自定义视图
                val layout = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 20)
                }

                // 标题
                val title = TextView(activity).apply {
                    text = "发现新版本 v$versionName"
                    textSize = 20f
                    setTextColor(Color.parseColor("#2c8a4a"))
                    gravity = Gravity.CENTER
                }
                layout.addView(title)

                // 当前版本
                val currentVersion = TextView(activity).apply {
                    text = "当前版本: v${getCurrentVersionName()}"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                    gravity = Gravity.CENTER
                    setPadding(0, 20, 0, 10)
                }
                layout.addView(currentVersion)

                // 更新内容
                if (!body.isNullOrEmpty()) {
                    val content = TextView(activity).apply {
                        // 截取前500字符
                        val displayBody = if (body.length > 500) {
                            body.substring(0, 500) + "..."
                        } else {
                            body
                        }
                        text = "更新内容:\n$displayBody"
                        textSize = 14f
                        setTextColor(Color.DKGRAY)
                        setPadding(0, 10, 0, 20)
                    }
                    layout.addView(content)
                }

                builder.setView(layout)

                // 按钮
                builder.setPositiveButton("立即更新") { _, _ ->
                    startDownload(downloadUrl, versionName)
                }

                builder.setNegativeButton("稍后更新") { dialog, _ ->
                    dialog.dismiss()
                }

                builder.setCancelable(false)

                val dialog = builder.create()
                dialog.show()

                // 设置按钮颜色
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.parseColor("#2c8a4a"))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.GRAY)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 显示没有更新的提示
     */
    private fun showNoUpdateToast() {
        mainHandler.post {
            Toast.makeText(activity, "当前已是最新版本", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 开始下载APK
     */
    private fun startDownload(downloadUrl: String, versionName: String) {
        // 显示下载进度对话框
        showDownloadDialog(downloadUrl, versionName)
    }

    /**
     * 显示下载进度对话框
     */
    private fun showDownloadDialog(downloadUrl: String, versionName: String) {
        val builder = AlertDialog.Builder(activity)

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val title = TextView(activity).apply {
            text = "正在下载 v$versionName"
            textSize = 18f
            setTextColor(Color.parseColor("#2c8a4a"))
            gravity = Gravity.CENTER
        }
        layout.addView(title)

        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            setPadding(0, 30, 0, 0)
        }
        layout.addView(progressBar)

        val statusText = TextView(activity).apply {
            text = "准备下载..."
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }
        layout.addView(statusText)

        builder.setView(layout)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog.show()

        // 开始下载
        DownloadTask(dialog, progressBar, statusText).execute(downloadUrl, versionName)
    }

    /**
     * 启动安装
     */
    private fun startInstall(apkPath: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.fromFile(File(apkPath)),
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            mainHandler.post {
                Toast.makeText(activity, "安装失败，请手动安装", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 检查更新的异步任务
     */
    private inner class CheckUpdateTask(private val showNoUpdate: Boolean) :
        AsyncTask<Void, Void, JsonObject?>() {

        override fun doInBackground(vararg voids: Void?): JsonObject? {
            try {
                val apiUrl = String.format(GITHUB_API_URL, owner, repo)
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == 200) {
                    val inputStream = BufferedInputStream(connection.inputStream)
                    val buffer = ByteArray(1024)
                    val response = StringBuilder()
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        response.append(String(buffer, 0, bytesRead))
                    }
                    inputStream.close()

                    return gson.fromJson(response.toString(), JsonObject::class.java)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(release: JsonObject?) {
            updateLastCheckTime()

            if (release == null) {
                if (showNoUpdate) {
                    showNoUpdateToast()
                }
                return
            }

            try {
                val tagName = release.get("tag_name").asString
                val latestVersion = parseVersion(tagName)
                val currentVersion = getCurrentVersionCode()

                if (latestVersion > currentVersion) {
                    val body = release.get("body")?.asString
                    val downloadUrl = getApkDownloadUrl(release)

                    if (downloadUrl != null) {
                        showUpdateDialog(tagName.replace("v", ""), downloadUrl, body)
                    }
                } else if (showNoUpdate) {
                    showNoUpdateToast()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (showNoUpdate) {
                    showNoUpdateToast()
                }
            }
        }

        private fun parseVersion(tagName: String): Int {
            return try {
                val version = tagName.replace("v", "").replace(".", "")
                version.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        }

        private fun getApkDownloadUrl(release: JsonObject): String? {
            try {
                if (release.has("assets")) {
                    val assets = release.getAsJsonArray("assets")
                    for (i in 0 until assets.size()) {
                        val asset = assets[i].asJsonObject
                        val name = asset.get("name").asString
                        if (name.endsWith(".apk")) {
                            return asset.get("browser_download_url").asString
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    /**
     * 下载APK的异步任务
     */
    private inner class DownloadTask(
        private val dialog: AlertDialog,
        private val progressBar: ProgressBar,
        private val statusText: TextView
    ) : AsyncTask<String, Int, String?>() {

        private var versionName: String = ""

        override fun doInBackground(vararg strings: String?): String? {
            val downloadUrl = strings[0]
            versionName = strings[1] ?: ""

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            var connection: HttpURLConnection? = null

            try {
                val url = URL(downloadUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode != 200) {
                    return null
                }

                val fileLength = connection.contentLength
                inputStream = BufferedInputStream(connection.inputStream)

                // 保存到外部存储
                val outputDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                val outputFile = File(
                    outputDir,
                    "reverse-prompt-translator-v$versionName.apk"
                )
                outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(4096)
                var total = 0L
                var count: Int

                while (inputStream.read(buffer).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        publishProgress((total * 100 / fileLength).toInt())
                    }
                    outputStream.write(buffer, 0, count)
                }

                outputStream.flush()
                return outputFile.absolutePath

            } catch (e: Exception) {
                e.printStackTrace()
                return null
            } finally {
                try {
                    outputStream?.close()
                    inputStream?.close()
                    connection?.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            values[0]?.let { progress ->
                progressBar.progress = progress
                statusText.text = "下载进度: $progress%"
            }
        }

        override fun onPostExecute(apkPath: String?) {
            dialog.dismiss()

            if (apkPath != null) {
                Toast.makeText(activity, "下载完成，开始安装", Toast.LENGTH_SHORT).show()
                startInstall(apkPath)
            } else {
                Toast.makeText(activity, "下载失败，请稍后重试", Toast.LENGTH_LONG).show()
            }
        }
    }
}
