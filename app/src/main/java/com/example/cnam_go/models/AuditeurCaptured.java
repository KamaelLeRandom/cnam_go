package com.example.cnam_go.models;

import java.util.Objects;

public class AuditeurCaptured {
    public String Id;

    public String Code;

    public String Name;

    public Long Hp;

    public Long Atk;

    public Long Def;

    public Long Spd;

    public boolean IsChad;

    public String CatchDate;

    public String Nickname;

    public AuditeurCaptured(
            String id,
            String code,
            String name,
            Long baseHp,
            Long baseAtk,
            Long baseDef,
            Long baseSpd,
            String nickname,
            String catchDate
    ) {
        this.Id = id;
        this.Code = code;
        this.Name = name;
        this.Hp = baseHp;
        this.Atk = baseAtk;
        this.Def = baseDef;
        this.Spd = baseSpd;
        this.CatchDate = catchDate;
        this.Nickname = nickname;
        this.IsChad = false;
    }

    public String getFullName() {
        if (!Objects.equals(this.Nickname, ""))
            return this.Nickname + " (" + this.Name + ")";
        return this.Name;
    }

    public String getImageName() {
        return this.Name.toLowerCase();
    }
}
