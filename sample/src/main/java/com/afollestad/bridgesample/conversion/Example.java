package com.afollestad.bridgesample.conversion;

import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.annotations.Header;
import com.afollestad.bridge.annotations.Json;

import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
@Json(name = "data")
public class Example {

    public Example() {
    }

    @Body
    public int one;
    @Body
    public boolean two;
    @Body
    public String three;
    @Body
    public float four;
    @Body
    public double five;
    @Body
    public double six;
    @Body
    public short seven;
    @Body
    public ExampleSub eight;
    @Body
    public ExampleSub[] nine;
    @Body
    public int[] ten;
    @Body(name = "nine")
    public List<ExampleSub> eleven;
    @Body(name = "ten")
    public List<Integer> twelve;

    @Header(name = "Content-Type")
    public String contentType;
    @Header(name = "Content-Length")
    public long contentLength;
}