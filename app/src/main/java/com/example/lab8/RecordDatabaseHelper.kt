package com.example.lab8
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RecordDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game_records.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "records"
        private const val COLUMN_ID = "id"
        private const val COLUMN_SCORE = "score"
        private const val COLUMN_NAME = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_SCORE INTEGER," +
                "$COLUMN_NAME TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertRecord(score: Int, name: String) {
        val db = this.writableDatabase
        val query = "INSERT INTO $TABLE_NAME ($COLUMN_SCORE, $COLUMN_NAME) VALUES ($score, '$name')"
        db.execSQL(query)
        db.close()
    }

    fun getTop5Records(): List<Pair<Int, String>> {
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_SCORE, $COLUMN_NAME FROM $TABLE_NAME ORDER BY $COLUMN_SCORE DESC LIMIT 5"
        val cursor = db.rawQuery(query, null)
        val records = mutableListOf<Pair<Int, String>>()

        if (cursor.moveToFirst()) {
            do {
                val score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                records.add(Pair(score, name))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return records
    }

    fun removeLowestRecord() {
        val db = this.writableDatabase
        val query = "DELETE FROM $TABLE_NAME WHERE $COLUMN_ID = (SELECT $COLUMN_ID FROM $TABLE_NAME ORDER BY $COLUMN_SCORE ASC LIMIT 1)"
        db.execSQL(query)
        db.close()
    }
}
