package com.example.habbittracker.Models;

public class Quotes {
    private int id;
    private String q;
    private String a;

    public Quotes(int id, String quote, String author) {
        this.id = id;
        this.q = quote;
        this.a = author;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }
}
