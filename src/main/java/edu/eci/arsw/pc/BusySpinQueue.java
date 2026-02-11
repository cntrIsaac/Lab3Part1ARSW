package edu.eci.arsw.pc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.LockSupport;

/** Intencionalmente incorrecta: usa busy-wait (alto CPU). */
public final class BusySpinQueue<T> {
  private final Deque<T> q = new ArrayDeque<>();
  private final int capacity;

  public BusySpinQueue(int capacity) {
    this.capacity = capacity;
  }

  public void put(T item) {
    // intenta insertar; si está lleno, usa backoff adaptativo
    int spins = 0;
    while (true) {
      synchronized (this) {
        if (q.size() < capacity) {
          q.addLast(item);
          return;
        }
      }
      // backoff: spin -> yield -> parkNanos
      if (spins < 100) {
        Thread.onSpinWait();
      } else if (spins < 1000) {
        Thread.yield();
      } else {
        LockSupport.parkNanos(1_000_000L); // 1 ms
      }
      spins++;
    }
  }

  public T take() {
    // intenta extraer; si está vacío, usa backoff adaptativo
    int spins = 0;
    while (true) {
      synchronized (this) {
        T v = q.pollFirst();
        if (v != null)
          return v;
      }
      if (spins < 100) {
        Thread.onSpinWait();
      } else if (spins < 1000) {
        Thread.yield();
      } else {
        LockSupport.parkNanos(1_000_000L);
      }
      spins++;
    }
  }

  public int size() {
    synchronized (this) {
      return q.size();
    }
  }
}
