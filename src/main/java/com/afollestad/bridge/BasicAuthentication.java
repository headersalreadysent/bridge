package com.afollestad.bridge;

import java.util.Base64;
import java.util.Locale;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public class BasicAuthentication implements Authentication {

  private static final String HEADER_NAME = "Authorization";

  private final String user;
  private final String pass;

  private BasicAuthentication(String user, String pass) {
    this.user = user;
    this.pass = pass;
  }

  public static BasicAuthentication create(String username, String password) {
    return new BasicAuthentication(username, password);
  }

  @Override
  public void apply(RequestBuilder request) throws Exception {
    final byte[] token =
        String.format(Locale.getDefault(), "%s:%s", this.user, this.pass).getBytes("UTF-8");
    final String encodedToken = Base64.getEncoder().encodeToString(token);
    request.header(HEADER_NAME, "Basic " + encodedToken);
  }
}
