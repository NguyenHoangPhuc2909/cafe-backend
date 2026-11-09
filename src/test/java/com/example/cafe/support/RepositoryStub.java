package com.example.cafe.support;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class RepositoryStub<T> {
  public final T value;
  public final Map<String, Integer> calls = new HashMap<>();
  private final Map<String, Function<Object[], Object>> handlers = new HashMap<>();
  public RepositoryStub(Class<T> type) {
    value = type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
        (proxy, method, args) -> {
          calls.merge(method.getName(), 1, Integer::sum);
          var handler = handlers.get(method.getName());
          if (handler == null) throw new AssertionError("Unexpected repository call: " + method.getName());
          return handler.apply(args);
        }));
  }
  public void on(String method, Function<Object[], Object> handler) { handlers.put(method, handler); }
}
