package com.afollestad.bridge;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface Authentication {

    void apply(RequestBuilder request) throws Exception;
}
