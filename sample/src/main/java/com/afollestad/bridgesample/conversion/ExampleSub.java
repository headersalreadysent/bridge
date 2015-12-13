package com.afollestad.bridgesample.conversion;

import com.afollestad.bridge.annotations.Body;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ExampleSub {

    public ExampleSub() {
    }

    @Body
    public String name;
    @Body
    public int age;
}
