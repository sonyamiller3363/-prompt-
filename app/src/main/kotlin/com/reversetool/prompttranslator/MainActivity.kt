package com.reversetool.prompttranslator

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private var webView: WebView? = null
    private var updateChecker: UpdateChecker? = null

    companion object {
        // GitHub 仓库信息（用于检查更新）
        private const val GITHUB_OWNER = "your-username" // 替换为你的 GitHub 用户名
        private const val GITHUB_REPO = "reverse-prompt-translator" // 替换为仓库名
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // 隐藏状态栏（沉浸式）
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // 创建 WebView
        webView = WebView(this)
        setContentView(webView)

        // 配置 WebView
        webView?.settings?.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            defaultTextEncodingName = "UTF-8"
        }

        // 设置 WebViewClient
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // 所有链接都在 WebView 内打开
                url?.let { view?.loadUrl(it) }
                return true
            }
        }

        // 设置 WebChromeClient
        webView?.webChromeClient = WebChromeClient()

        // 加载本地 HTML
        webView?.loadUrl("file:///android_asset/index.html")

        // 初始化更新检查器
        updateChecker = UpdateChecker(this, GITHUB_OWNER, GITHUB_REPO)

        // 启动时检查更新（后台静默检查）
        updateChecker?.checkForUpdate(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // 创建菜单
        menu?.add(0, 1, 0, "检查更新")
        menu?.add(0, 2, 0, "关于")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                // 检查更新
                updateChecker?.checkForUpdate(true)
                true
            }
            2 -> {
                // 关于
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage(
                "逆向 Prompt 翻译器 v1.0\n\n" +
                "功能：将逆向工程问题转化为结构化的 Claude Skill 提示词\n\n" +
                "特性：\n" +
                "• 158 个内嵌资源\n" +
                "• 6+ 场景支持\n" +
                "• 完全离线可用\n" +
                "• 自动更新"
            )
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 返回键处理
        if (keyCode == KeyEvent.KEYCODE_BACK && webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }
}
