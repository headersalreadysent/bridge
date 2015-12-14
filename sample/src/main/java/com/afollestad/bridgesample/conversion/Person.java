package com.afollestad.bridgesample.conversion;

import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.annotations.Header;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Person {

    public Person() {
    }

    @Body
    public String name;
    @Body
    public short age;
    @Body
    public int year;
    @Body
    public long skill;
    @Body
    public double rank;
    @Body(name = "f_age")
    public float fAge;
    @Body(name = "is_programmer")
    public boolean isProgrammer;
    @Body
    public Person girlfriend;

    @Header(name = "Content-Type")
    public String contentType;
    @Header(name = "Content-Length")
    public long contentLength;
}