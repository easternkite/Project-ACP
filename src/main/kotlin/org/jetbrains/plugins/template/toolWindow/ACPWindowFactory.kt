package org.jetbrains.plugins.template.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class ACPWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Codelab URL을 지정
        val codelabUrl = "https://developer.android.com/codelabs/android-navigation?hl=ko#0"
        // 패널 생성 및 추가 (BorderLayout 사용)
        val panel = JBCefPanelWithBridge(codelabUrl, project)

        // ToolWindow에 내용 추가
        toolWindow.contentManager.addContent(toolWindow.contentManager.factory.createContent(panel, "Android Codelab", false))
    }

    override fun shouldBeAvailable(project: Project) = true
}

