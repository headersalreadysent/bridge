package com.afollestad.bridgesample.conversion;

import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.annotations.ContentType;

/**
 * @author Aidan Follestad (afollestad)
 */
@ContentType("application/json")
public class Person {

    public Person() {
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Body
    public String name;
    @Body
    public int age;
    @Body
    public Person girlfriend;
}