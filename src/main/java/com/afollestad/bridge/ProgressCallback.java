package com.afollestad.bridge;

import java.io.Serializable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
public abstract class ProgressCallback implements Serializable {

  Request request;
  private int lastPercent = -1;

  public abstract void progress(Request request, int current, int total, int percent);

  public final void publishProgress(int current, int total) {
    final int percent = (int) (((double) current / (double) total) * 100d);
    if (percent != lastPercent) {
      progress(request, current, total, percent);
      lastPercent = percent;
    }
  }
}
