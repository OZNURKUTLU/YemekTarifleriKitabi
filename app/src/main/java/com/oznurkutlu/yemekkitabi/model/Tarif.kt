package com.oznurkutlu.yemekkitabi.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Tarif (
    @ColumnInfo(name="isim")
    var isim:String,

    @ColumnInfo(name="gorsel")
    var malzeme:String,

    @ColumnInfo(name="malzeme")
    var gorsel: ByteArray

){
    @PrimaryKey(autoGenerate=true)
    var id=0
}