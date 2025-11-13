package com.sybyl.trace.web;

public class SearchForm {
    private String q;
    private Integer size = 10;

    public SearchForm() {}
    public SearchForm(String q, Integer size){ this.q = q; this.size = size; }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
