package com.example.cnam_go.models;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.InputStream;
import java.util.Random;

public class AuditeurMap {
    public GeoPoint position;
    public Marker marker;
    public long creationTime;
    public AuditeurEntity entity;

    private static final Random random = new Random();

    public AuditeurMap(Context context, GeoPoint pos, Marker mark) {
        this.position = pos;
        this.marker = mark;
        this.creationTime = System.currentTimeMillis();

        try {
            InputStream is = context.getAssets().open("auditeurs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer);

            JSONArray array = new JSONArray(jsonStr);

            int index = random.nextInt(array.length());
            JSONObject obj = array.getJSONObject(index);

            String codeBall = obj.getString("codeBall");
            String code = obj.getString("code");
            String name = obj.getString("name");
            int baseHp = obj.getInt("baseHp");
            int baseAtk = obj.getInt("baseAtk");
            int baseDef = obj.getInt("baseDef");
            int baseSpd = obj.getInt("baseSpd");

            this.entity = new AuditeurEntity(code, name,
                    randomStat(baseHp),
                    randomStat(baseAtk),
                    randomStat(baseDef),
                    randomStat(baseSpd),
                    codeBall
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int randomStat(int baseValue) {
        // Alteration de 10% des stats (0.9 à 1.1)
        double factor = 0.9 + (0.2 * random.nextDouble());

        return (int) Math.round(baseValue * factor);
    }
}