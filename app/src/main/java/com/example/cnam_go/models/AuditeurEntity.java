package com.example.cnam_go.models;

import java.io.Serializable;

public class AuditeurEntity implements Serializable {
    public String Code;

    public String Name;

    public int BaseHp;

    public int BaseAtk;

    public int BaseDef;

    public int BaseSpd;

    public boolean IsChad;

    public AuditeurEntity(String code, String name, int baseHp, int baseAtk, int baseDef, int baseSpd) {
        this.Code = code;
        this.Name = name;
        this.BaseHp = baseHp;
        this.BaseAtk = baseAtk;
        this.BaseDef = baseDef;
        this.BaseSpd = baseSpd;
        this.IsChad = false;
    }
}
