package com.afollestad.bridgesample.conversion;

import com.afollestad.bridge.annotations.Body;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SimplePerson {

    public SimplePerson() {
    }

    public SimplePerson(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Body
    public String name;
    @Body
    public int age;
}