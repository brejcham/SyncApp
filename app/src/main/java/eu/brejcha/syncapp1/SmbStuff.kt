package eu.brejcha.syncapp1


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import java.util.concurrent.TimeUnit
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import java.util.EnumSet


class SyncAppException(message: String) : Exception(message)
class FileInfo(val filePath: String, val fileObject: DocumentFile)

fun listDirectory(share: DiskShare, path: String, out: StringBuilder) {
    val files = share.list(path)
    for (file in files) {
        if (file.fileAttributes and 16L == 16L) {
            if (file.fileName != "." && file.fileName != "..") {
                out.append(file.fileName)
                out.append("\n")
                listDirectory(share, "$path/${file.fileName}", out)
            }
        } else {
            out.append(file.fileName)
            out.append("\n")
        }
    }
}

fun listFilesAndSubdirectories(docFile: DocumentFile?, lastSync: Long, prefix: String, fileNames: MutableList<FileInfo>) {
    if (docFile != null && docFile.isDirectory) {
        for (file in docFile.listFiles()) {
            val filePath = "$prefix/${file.name?: ""}"
            if (lastSync == -1L || lastSync < file.lastModified()) {
                //fileNames.add(file.name ?: "")
                fileNames.add(FileInfo(filePath, file))
            }
            if (file.isDirectory) {
                listFilesAndSubdirectories(file, lastSync, filePath, fileNames) // Recursive call for subdirectories
            }
        }
    }
}

fun listLocalDirectory(context: Context, path: Path, prefix: String): List<FileInfo> {

    val pathUri = Uri.parse(path.id)
    val contentResolver = context.contentResolver

    val hasPermission = contentResolver.persistedUriPermissions.any { it.uri == pathUri }
    if (!hasPermission) {
        Log.e("SyncApp", "No permission to access path: $pathUri")
    }

    val fileNames = mutableListOf<FileInfo>()
    val docFile = DocumentFile.fromTreeUri(context, pathUri)

    Log.d("SyncApp", "docFile: $docFile")
    listFilesAndSubdirectories(docFile, path.timestamp, prefix, fileNames)
    return fileNames
}

fun syncPathWithSmb(path: Path, context: Context, onProcessUpdate: (Int) -> Unit) {

    val settings = loadSettingsFromDb(context)

    val localFiles = listLocalDirectory(context, path, settings.sharePath)

    val config = SmbConfig.builder()
        .withTimeout(3000, TimeUnit.MILLISECONDS)
        .build()
    val smbClient = SMBClient(config)

    onProcessUpdate(0)

    try {
        val connection = smbClient.connect(settings.serverIp, settings.serverPort)
        val session = connection.authenticate(
            AuthenticationContext(
                settings.username, settings.password.toCharArray(), settings.domain
            )
        )

        val share = session.connectShare(settings.shareName) as DiskShare
        //listDirectory(share, "", out)
        localFiles.forEachIndexed { fileIdx, localFile ->
            Log.d("SyncApp", "Syncing file ${localFile.filePath}")
            if (localFile.fileObject.isDirectory) {
                if (!share.folderExists(localFile.filePath)) {
                    Log.d("SyncApp", "Creating directory ${localFile.filePath}")
                    share.mkdir(localFile.filePath)
                } else {
                    Log.d("SyncApp", "Directory ${localFile.filePath} already exists")
                }
            } else {
                val fileIs = context.contentResolver.openInputStream(localFile.fileObject.uri)
                if (fileIs != null) {
                    val sambaFile = share.openFile(
                        localFile.filePath,
                        EnumSet.of(AccessMask.GENERIC_WRITE),
                        null,
                        EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                        SMB2CreateDisposition.FILE_OVERWRITE_IF, null
                    )
                    val smbFileOs = sambaFile.outputStream
                    fileIs.copyTo(smbFileOs)
                    fileIs.close()
                    smbFileOs.close()
                }
            }
            Log.d("SyncApp", "File ${localFile.filePath} synced")
            onProcessUpdate((fileIdx.toDouble() / localFiles.size * 100).toInt())
        }
        share.close()
        session.close()
        connection.close()

    } catch (e: Exception) {
        throw SyncAppException(e.toString())
    } finally {
        smbClient.close()
        onProcessUpdate(100)
    }
}