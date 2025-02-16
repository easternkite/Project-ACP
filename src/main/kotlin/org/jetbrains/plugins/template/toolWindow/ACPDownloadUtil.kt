package org.jetbrains.plugins.template.toolWindow

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ACPDownloadUtil {
    // GitHub에서 리포지토리 ZIP 다운로드
    suspend fun downloadAndExtractRepo(gitUrl: String, project: Project) {
        withContext(Dispatchers.IO) {
            try {
                val repoUrl = gitUrl.replace("\"", "")
                println("Downloading repo: $repoUrl")
                val zipFilePath = "${project.basePath}/repo.zip"
                val outputDir = File("${project.basePath}")
                if (!outputDir.exists()) {
                    outputDir.mkdir()
                }

                // 현재 프로젝트 디렉토리 클리어
                clearDirectory(outputDir)

                // GitHub 리포지토리 ZIP 파일 다운로드
                downloadZipFile(repoUrl, zipFilePath)

                // 다운로드한 ZIP 파일을 지정된 디렉토리에 압축 해제
                unzip(zipFilePath, outputDir)

                println("리포지토리 다운로드 및 압축 해제 완료")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ZIP 파일 다운로드
    private suspend fun downloadZipFile(url: String, savePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(url).openStream()
                val outputStream = Files.newOutputStream(Paths.get(savePath))
                inputStream.copyTo(outputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    // zip 압축 해제. (차상위 디렉토리부터 포함됨)
    suspend fun unzip(zipFilePath: String, outputDir: File) {
        withContext(Dispatchers.IO) {
            try {
                val buffer = ByteArray(1024)
                val zipInputStream = ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))
                var zipEntry: ZipEntry? = zipInputStream.nextEntry

                // 최상위 디렉토리 찾기
                var rootDir: String? = null

                while (zipEntry != null) {
                    val entryName = zipEntry.name
                    val firstDir = entryName.substringBefore("/")

                    if (rootDir == null) {
                        rootDir = firstDir // 첫 번째 발견된 디렉토리를 루트 디렉토리로 설정
                    }

                    zipEntry = zipInputStream.nextEntry
                }

                zipInputStream.close()

                // 다시 ZIP 파일을 열어서 압축 해제 진행
                val zipStream = ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))
                var entry: ZipEntry? = zipStream.nextEntry

                while (entry != null) {
                    val entryName = entry.name

                    // 최상위 디렉토리를 제외한 상대 경로 계산
                    val relativePath = if (rootDir != null && entryName.startsWith("$rootDir/")) {
                        entryName.removePrefix("$rootDir/")
                    } else {
                        entryName
                    }

                    if (relativePath.isNotEmpty()) {
                        val newFile = File(outputDir, relativePath)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs() // 상위 디렉토리 생성
                            val outputStream = Files.newOutputStream(newFile.toPath())
                            var len: Int
                            while (zipStream.read(buffer).also { len = it } > 0) {
                                outputStream.write(buffer, 0, len)
                            }
                            outputStream.close()
                        }
                    }

                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
                zipStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    // 프로젝트 디렉토리 클리어
    private suspend fun clearDirectory(directory: File) {
        withContext(Dispatchers.IO) {
            if (directory.exists()) {
                val files = directory.listFiles()
                files?.forEach {
                    if (it.isDirectory) {
                        clearDirectory(it) // 재귀적으로 하위 디렉토리도 클리어
                    }
                    it.delete()
                }
            }
        }
    }
}