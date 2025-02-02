package com.oznurkutlu.yemekkitabi.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.oznurkutlu.yemekkitabi.model.Tarif


@Database(entities = [Tarif::class], version = 1)
abstract class TarifDatabase : RoomDatabase() {
    abstract fun tarifDao(): TarifDAO
}