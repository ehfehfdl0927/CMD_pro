package com.CMD.CMD_pro.board.domain;

import java.util.Date;

public class BoardVO {
    private int bno;
    private String subject;
    private String content;
    private String writer;
    private Date reg_date;
    private int hit;
    private int notice;
    private String kind;
    private String realm;
    private int recommend;
    private String real_date;

    public int getBno() { return bno; }

    public void setBno(int bno) {
        this.bno = bno;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getWriter() {
        return writer;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public Date getReg_date() {
        return reg_date;
    }

    public void setReg_date(Date reg_date) {
        this.reg_date = reg_date;
    }

    public int getHit() {
        return hit;
    }

    public void setHit(int hit) {
        this.hit = hit;
    }

    public int getNotice() { return notice; }

    public void setNotice(int notice) { this.notice = notice; }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public int getRecommend() {
        return recommend;
    }

    public void setRecommend(int recommend) {
        this.recommend = recommend;
    }

    public String getReal_date() {
        return real_date;
    }

    public void setReal_date(String real_date) {
        this.real_date = real_date;
    }
}
