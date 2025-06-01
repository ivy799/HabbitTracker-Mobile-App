package com.example.habbittracker.Models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;

public class Habit implements Parcelable {
    private int id, target_count, current_count;
    private String name, description, category, frequency;
    private Date start_date;
    private  Boolean is_active;

    public Habit(int id, int target_count, int current_count, String name, String description, String category, String frequency, Date start_date, Boolean is_active) {
        this.id = id;
        this.target_count = target_count;
        this.current_count = current_count;
        this.name = name;
        this.description = description;
        this.category = category;
        this.frequency = frequency;
        this.start_date = start_date;
        this.is_active = is_active;
    }

    protected Habit(Parcel in) {
        id = in.readInt();
        target_count = in.readInt();
        current_count = in.readInt();
        name = in.readString();
        description = in.readString();
        category = in.readString();
        frequency = in.readString();
        byte tmpIs_active = in.readByte();
        is_active = tmpIs_active == 0 ? null : tmpIs_active == 1;
    }

    public static final Creator<Habit> CREATOR = new Creator<Habit>() {
        @Override
        public Habit createFromParcel(Parcel in) {
            return new Habit(in);
        }

        @Override
        public Habit[] newArray(int size) {
            return new Habit[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTarget_count() {
        return target_count;
    }

    public void setTarget_count(int target_count) {
        this.target_count = target_count;
    }

    public int getCurrent_count() {
        return current_count;
    }

    public void setCurrent_count(int current_count) {
        this.current_count = current_count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Date getStart_date() {
        return start_date;
    }

    public void setStart_date(Date start_date) {
        this.start_date = start_date;
    }

    public Boolean getIs_active() {
        return is_active;
    }

    public void setIs_active(Boolean is_active) {
        this.is_active = is_active;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(target_count);
        parcel.writeInt(current_count);
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeString(category);
        parcel.writeString(frequency);
        parcel.writeByte((byte) (is_active == null ? 0 : is_active ? 1 : 2));
    }
}
