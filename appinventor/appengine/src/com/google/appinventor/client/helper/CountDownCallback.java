package com.google.appinventor.client.helper;


/*
 * A callback that waits until it's called n times before it runs (runs on the nth time)
 * TODO (evan): remove this file. CollectorCallback can always be used instead
 */
public class CountDownCallback<E> implements Callback<E> {
  private int n;
  private Callback<E> callback;
  public CountDownCallback(int n, Callback<E> callback) {
    this.n = n;
    this.callback = callback;
  }

  @Override
  public void call(E param) {
    n -=1;
    if (n == 0) {
      callback.call(param);
    } else if (n < 0) {
      throw new RuntimeException("CountDownCallback called more times than expected");
    }
  }
}

