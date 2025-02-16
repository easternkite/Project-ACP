package com.github.easternkite.projectacp.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest
import javax.swing.JPanel

class JBCefPanelWithBridge(url: String, project: Project) : JPanel() {
    private val browser = JBCefBrowser(url)
    private val jsBridge = JsBridge(browser, project)

    init {
        browser.openDevtools()
        layout = java.awt.BorderLayout()
        add(browser.component, java.awt.BorderLayout.CENTER)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandler {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                p1: Boolean,
                p2: Boolean,
                p3: Boolean
            ) {
            }

            override fun onLoadStart(
                browser: CefBrowser?,
                p1: CefFrame?,
                p2: CefRequest.TransitionType?
            ) {
                if (browser != null) {
                    println("load start : ${browser.url}")
                    jsBridge.injectJSBridge()
                }
            }

            override fun onLoadEnd(browser: CefBrowser?, p1: CefFrame?, p2: Int) {
                println("load end : browser = ${browser?.url}")
                jsBridge.injectJSBridge()

                val jsCode = """
                    // 모든 a 태그를 가져와서 검사
                    let links = document.querySelectorAll('a');
                    let allowedDomain = 'https://developer.android.com/codelabs';
                    let gitUrl = null; 
                    links.forEach(function(link) {
                        if (link.href.includes('github.com')) {
                            gitUrl = link.href;
                            sendToJava(gitUrl);
                        }
                    });
                    sendToJava({ command: "sendData", message: "Hello!" });
                """

                browser?.executeJavaScript(jsCode, browser.url, 0)
            }

            override fun onLoadError(
                p0: CefBrowser?,
                p1: CefFrame?,
                p2: CefLoadHandler.ErrorCode?,
                p3: String?,
                p4: String?
            ) {
                TODO("Not yet implemented")
            }
        }, browser.cefBrowser)
    }

    fun sendDataToJS(data: String) {
        jsBridge.sendToJS(data)
    }
}

class JsBridge(private val browser: JBCefBrowserBase, val project: Project) {
    private val jsQuery = JBCefJSQuery.create(browser)
    private var isDownloading = false
    init {
        jsQuery.addHandler { request ->
            println("JSBROWSER: $request")
            if (request.contains("github.com") && request.contains("zip") && !isDownloading) {
                CoroutineScope(Dispatchers.IO).launch {
                    isDownloading = true
                    ACPDownloadUtil.downloadAndExtractRepo(request, project = project)
                }
            }
            return@addHandler JBCefJSQuery.Response(request)
        }
    }
    fun injectJSBridge() {
        val script = """
            window.sendToJava = function(data) {
                console.log("sendToJava called with:", data);
                return ${jsQuery.inject("JSON.stringify(data)")};
            };
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
    }

    fun sendToJS(data: String) {
        browser.cefBrowser.executeJavaScript("window.receiveFromJava('$data')", browser.cefBrowser.url, 0)
    }
}
