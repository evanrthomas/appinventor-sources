package com.google.appinventor.client.helper;


/*
 * A callback that has some extra information  that can be accessed by the call function
 */
public abstract class Closure<R, E> implements Callback<E> {
  protected R env;

  public Closure(R env) {
    this.env = env;
  }

}
