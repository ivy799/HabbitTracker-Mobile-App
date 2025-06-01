package com.example.habbittracker.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class HabitLog implements Parcelable {
    private int id, habit_id;
    private Date log_date;
    private boolean status;

    public HabitLog(int id, int habit_id, Date log_date, boolean status) {
        this.id = id;
        this.habit_id = habit_id;
        this.log_date = log_date;
        this.status = status;
    }

    protected HabitLog(Parcel in) {
        id = in.readInt();
        habit_id = in.readInt();
        status = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(habit_id);
        dest.writeByte((byte) (status ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HabitLog> CREATOR = new Creator<HabitLog>() {
        @Override
        public HabitLog createFromParcel(Parcel in) {
            return new HabitLog(in);
        }

        @Override
        public HabitLog[] newArray(int size) {
            return new HabitLog[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHabit_id() {
        return habit_id;
    }

    public void setHabit_id(int habit_id) {
        this.habit_id = habit_id;
    }

    public Date getLog_date() {
        return log_date;
    }

    public void setLog_date(Date log_date) {
        this.log_date = log_date;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
