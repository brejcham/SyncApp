package eu.brejcha.syncapp1

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.icu.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Path(val id: String,
                val path: String,
                var timestamp: Long
) {
    var progress: Int = 100
}
fun loadPathsFromDb(context: Context): List<Path> {
    val app = context.applicationContext as Application
    val dbHelper = DatabaseHelper(app)
    val db = dbHelper.readableDatabase
    val paths = mutableListOf<Path>()

    try {
        val cursor = db.query(
            "paths",
            arrayOf("id", "path", "timestamp"),
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val path = Path(cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("path")),
                cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
            paths.add(path)
        }
        cursor.close()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db.close()
    }
    return paths
}

fun timestampToString(timestamp: Long): String {
    val date = Date(timestamp)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(date)
}

fun savePathToDb(context: Context, path: Path) {
    val app = context.applicationContext as Application
    val dbHelper = DatabaseHelper(app)
    val db = dbHelper.writableDatabase
    val values = ContentValues().apply {
        put("id", path.id)
        put("path", path.path)
        put("timestamp", path.timestamp)
    }
    try {
        db.insert("paths", null, values)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db.close()
    }
}

fun removePathFromDb(context: Context, path: Path) {
    val app = context.applicationContext as Application
    val dbHelper = DatabaseHelper(app)
    val db = dbHelper.writableDatabase
    try {
        //db.execSQL("DELETE FROM paths WHERE path = ?", arrayOf(path.id))
        db.delete("paths", "id = ?", arrayOf(path.id))
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db.close()
    }
}

fun updatePathInDb(context: Context, path: Path) {
    val app = context.applicationContext as Application
    val dbHelper = DatabaseHelper(app)
    val db = dbHelper.writableDatabase

    val values = ContentValues().apply {
        put("timestamp", path.timestamp)
    }
    try {
        db.update("paths", values, "id = ?", arrayOf(path.id))
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db.close()
    }
}

data class Settings(var id: Int, val serverIp: String, val serverPort: Int,
                    val username: String, val password: String, val domain: String,
                    val shareName: String, val sharePath: String = "")

fun loadSettingsFromDb(context: Context): Settings {
    val app = context.applicationContext as Application
    val dbHelper = DatabaseHelper(app)
    val db = dbHelper.readableDatabase
    var settings = Settings(0, "1.2.3.4", 445,
                            "", "", "WORKGROUP", "")

    try {
        val cursor = db.query(
            "settings",
            arrayOf("id", "server_ip", "server_port", "username", "password", "domain", "share_name", "share_path"),
            null,
            null,
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            settings = Settings(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("server_ip")),
                cursor.getInt(cursor.getColumnIndexOrThrow("server_port")),
                cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cursor.getString(cursor.getColumnIndexOrThrow("password")),
                cursor.getString(cursor.getColumnIndexOrThrow("domain")),
                cursor.getString(cursor.getColumnIndexOrThrow("share_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("share_path"))
            )
        }
        cursor.close()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db.close()
    }

    return settings
}

fun saveSettingsToDb(context: Context, settings: Settings) {
    if (settings.id == 1) {
        return updateSettingsInDb(context, settings)
    }

    settings.id = 1

    val app = context.applicationContext as Application
    val dbHelper = DatabaseHelper(app)
    val db = dbHelper.writableDatabase

    val values = ContentValues().apply {
        put("id", settings.id)
        put("server_ip", settings.serverIp)
        put("server_port", settings.serverPort)
        put("username", settings.username)
        put("password", settings.password)
        put("domain", settings.domain)
        put("share_name", settings.shareName)
        put("share_path", settings.sharePath)
    }

    try {
        db.insert("settings", null, values)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db.close()
    }
}

fun updateSettingsInDb(context: Context, settings: Settings) {
    val app = context.applicationContext as Application
    val dbHelper = DatabaseHelper(app)
    val db = dbHelper.writableDatabase

    val values = ContentValues().apply {
        put("server_ip", settings.serverIp)
        put("server_port", settings.serverPort)
        put("username", settings.username)
        put("password", settings.password)
        put("domain", settings.domain)
        put("share_name", settings.shareName)
        put("share_path", settings.sharePath)
    }

    try {
        db.update("settings", values, "id = ?", arrayOf(settings.id.toString()))
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db.close()
    }
}

class DatabaseHelper(application: Application) :
    SQLiteOpenHelper(application, "syncAppDb", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE paths (id TEXT PRIMARY KEY, path TEXT, timestamp INTEGER)")
        db.execSQL("CREATE TABLE settings (id INTEGER PRIMARY KEY, server_ip TEXT, server_port INTEGER," +
                        "username TEXT, password TEXT, domain TEXT, share_name TEXT, share_path TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // TODO
    }
}