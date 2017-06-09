package com.afollestad.bridge;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public class CancelCriteria {

  private final Object LOCK;
  private final HandlerCompat handler;
  private Bridge client;
  private int method = Method.UNSPECIFIED;
  private String urlRegex = null;
  private Object tag = null;
  private boolean force = false;

  CancelCriteria(Bridge client, Object lock) {
    this.client = client;
    LOCK = lock;
    handler = new HandlerCompat();
  }

  private boolean passesMethod(Map.Entry<String, CallbackStack> entry) {
    if (method == Method.UNSPECIFIED) return true;
    final String[] splitKey = entry.getKey().split("\0");
    final int keyMethod = Integer.parseInt(splitKey[0]);
    //noinspection ResourceType
    return keyMethod == method;
  }

  private boolean passesUrlRegex(Map.Entry<String, CallbackStack> entry) {
    if (urlRegex == null) return true;
    final Pattern pattern = Pattern.compile(urlRegex);
    final String[] splitKey = entry.getKey().split("\0");
    final String keyUrl = splitKey[1];
    return pattern.matcher(keyUrl).find();
  }

  public CancelCriteria method(int method) {
    this.method = method;
    return this;
  }

  public CancelCriteria url(@Nullable String urlRegex) {
    this.urlRegex = urlRegex;
    return this;
  }

  public CancelCriteria tag(@Nullable String tag) {
    this.tag = tag;
    return this;
  }

  public CancelCriteria force() {
    force = true;
    return this;
  }

  public int commit() {
    synchronized (LOCK) {
      if (client.requestMap == null) return 0;
      int cancelledCount = 0;
      final Iterator<Map.Entry<String, CallbackStack>> iter =
          client.requestMap.entrySet().iterator();
      while (iter.hasNext()) {
        final Map.Entry<String, CallbackStack> entry = iter.next();
        if (passesMethod(entry) && passesUrlRegex(entry)) {
          if (client.requestMap.get(entry.getKey()).cancelAll(tag, force)) {
            iter.remove();
            cancelledCount++;
          }
        }
      }
      if (client.requestMap.size() == 0) client.requestMap = null;
      return cancelledCount;
    }
  }

  public void commitAsync(@Nullable final CancelCallback callback) {
    new Thread(
            new TimerTask() {
              @Override
              public void run() {
                final int count = commit();
                if (callback != null) {
                  handler.post(
                      new TimerTask() {
                        @Override
                        public void run() {
                          callback.onRequestsCancelled(count);
                        }
                      });
                }
              }
            })
        .start();
  }

  @SuppressWarnings("WeakerAccess")
  public interface CancelCallback {
    void onRequestsCancelled(int count);
  }
}
