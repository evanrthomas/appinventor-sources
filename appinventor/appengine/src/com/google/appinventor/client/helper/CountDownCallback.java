package com.google.appinventor.client.helper;


import java.util.ArrayList;
import java.util.Collection;

/*
 * A callback that waits until it's called n times before it runs (runs on the nth time)
 * TODO (evan): remove this file. CollectorCallback can always be used instead
 */
public class CountDownCallback<E> implements Callback<E> {
  private int n;
  private Callback<Collection<E>> callback;
  private Collection<E> collected;
  public CountDownCallback(int n, Callback<Collection<E>> callback) {
    this.n = n;
    collected = new ArrayList<E>();
    this.callback = callback;
  }

  @Override
  public void call(E param) {
    n -=1;
    collected.add(param);
    if (n == 0) {
      callback.call(collected);
    } else if (n < 0) {
      throw new RuntimeException("CountDownCallback called more times than expected");
    }
  }
}

