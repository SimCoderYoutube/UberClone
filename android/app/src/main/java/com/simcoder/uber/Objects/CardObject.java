package com.simcoder.uber.Objects;

public class CardObject {

    String brand, id, name;
    int expMonth, expYear, lastDigits;
    Boolean defaultCard = false;

    /**
     * CardObject constructor
     * @param id - id of the card
     */
    public CardObject(String id){
        this.id = id;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setExpMonth(int expMonth) {
        this.expMonth = expMonth;
    }

    public void setExpYear(int expYear) {
        this.expYear = expYear;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastDigits(int lastDigits) {
        this.lastDigits = lastDigits;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDefaultCard(Boolean defaultCard) {
        this.defaultCard = defaultCard;
    }

    public int getExpMonth() {
        return expMonth;
    }

    public int getExpYear() {
        return expYear;
    }

    public int getLastDigits() {
        return lastDigits;
    }

    public String getBrand() {
        return brand;
    }

    public String getName() {
        return name;
    }

    public Boolean getDefaultCard() {
        return defaultCard;
    }

    public String getId() {
        return id;
    }
}
