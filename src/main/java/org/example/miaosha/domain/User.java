package org.example.miaosha.domain;

public class User {

    private int id;
    private String name;
    private String url;
    private String alexa;
    private String country;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getAlexa() {
        return alexa;
    }

    public String getCountry() {
        return country;
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAlexa(String alexa) {
        this.alexa = alexa;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
