package kr.ac.mjc.capstar;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SpaceLog implements Parcelable {

    public String code;
    public String date;
    public boolean authCheck;
    public String authMethod;
    public String authUrl;
    public String key;
    public String name_phoneNo;

    public SpaceLog(){}

    public SpaceLog(String code, Date date, boolean authCheck, String authMethod, String authUrl, String name_phoneNo){
        this.code = code;
        SimpleDateFormat sdfNow = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
        String formatDate = sdfNow.format(date);
        this.date = formatDate;
        this.authCheck = authCheck;
        this.authMethod = authMethod;
        this.authUrl = authUrl;
        this.name_phoneNo = name_phoneNo;
    }

    protected SpaceLog(Parcel in) {
        code = in.readString();
        date = in.readString();
        authCheck = in.readByte() != 0;
        authMethod = in.readString();
        authUrl = in.readString();
        key = in.readString();
        name_phoneNo = in.readString();
    }

    public static final Creator<SpaceLog> CREATOR = new Creator<SpaceLog>() {
        @Override
        public SpaceLog createFromParcel(Parcel in) {
            return new SpaceLog(in);
        }

        @Override
        public SpaceLog[] newArray(int size) {
            return new SpaceLog[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(code);
        dest.writeString(date);
        dest.writeByte((byte) (authCheck ? 1 : 0));
        dest.writeString(authMethod);
        dest.writeString(authUrl);
        dest.writeString(key);
        dest.writeString(name_phoneNo);
    }
}
