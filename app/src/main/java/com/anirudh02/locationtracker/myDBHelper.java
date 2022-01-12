package com.anirudh02.locationtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class myDBHelper extends SQLiteOpenHelper {
    public myDBHelper(Context context) { //constructor method for Database helper class. Database name declared here.
        super(context, "trackDB", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) { //Database table is created in this method.
        db.execSQL("Create Table trackTBL(lastLatitude DOUBLE," +
                "lastLongitude DOUBULE, latitude DOUBLE, longitude DOUBLE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) { //In this method, the database table is recreated if the database is upgraded.
        db.execSQL("Drop table if exists trackTBL");
        onCreate(db);
    }

    public boolean addPoints(Double lastLatitude, Double lastLongitude, Double latitude, Double longitude) { //The latitude/longitude points
        //necessary to draw Polylines are added to the database in this method
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("lastLatitude", lastLatitude);
        values.put("lastLongitude", lastLongitude);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        db.insert("trackTBL", null, values);
        return true;
    }
    public ArrayList getAllPoints() { //All PolylineOption objects are created with the latitude/longitude values from the DB in this method.
        //The objects are then stored in an ArrayList and returned.
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<PolylineOptions> lines = new ArrayList<PolylineOptions>();
        Cursor cursor = db.rawQuery("Select * from trackTBL", null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            Double lLa = cursor.getDouble(cursor.getColumnIndex("lastLatitude"));
            Double lLo = cursor.getDouble(cursor.getColumnIndex("lastLongitude"));
            Double la = cursor.getDouble(cursor.getColumnIndex("latitude"));
            Double lo = cursor.getDouble(cursor.getColumnIndex("longitude"));
            PolylineOptions line =  new PolylineOptions().add(new LatLng(lLa, lLo),
                    new LatLng(la, lo)).width(5).color(Color.RED);
            lines.add(line);
            cursor.moveToNext();
        }
        return lines;
    }
}
