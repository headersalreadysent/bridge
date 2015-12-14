package com.afollestad.bridgesample.conversion;

import com.afollestad.bridge.annotations.Body;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SimplePerson {

    public SimplePerson() {
    }

    @Body
    public String name;
    @Body
    public int age;
    @Body
    public float rank;
}