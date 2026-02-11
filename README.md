# Parte I — Productor/Consumidor con `wait/notify` (y contraste con busy-wait)

## Modo Spin
![](img/mode_spin.png)

## Modo Monitor
 ![](img/mode_monitor.png)


### 1. Productor lento / Consumidor rápido

Cuando el productor genera elementos lentamente y el consumidor procesa muy rápido, el buffer se queda vacío con frecuencia.

- En mode=monitor: el consumidor debe quedar en estado WAITING (wait/notify), sin consumir CPU.
- En mode=spin: el consumidor entra en busy-wait, revisando constantemente el buffer vacío → consume CPU innecesariamente.

  El modo monitor implementa espera bloqueante eficiente, mientras que spin utiliza espera activa, generando potencial desperdicio de CPU cuando el buffer está vacío.

### 2. Productor rápido / Consumidor lento (capacidad pequeña)

Cuando el productor es más rápido y la capacidad del buffer es pequeña (4 u 8), el buffer se llena rápidamente.

- En monitor: el productor entra en WAITING hasta que el consumidor libere espacio → sin consumo de CPU.
- En spin: el productor revisa constantemente si hay espacio → busy-wait → uso innecesario de CPU.

  En mode=monitor, el productor libera el CPU al bloquearse mediante mecanismos de sincronización, mientras que en mode=spin permanece activo evaluando repetidamente la condición de disponibilidad.

### 3. Comparación CPU

Aquí está lo más importante para tu conclusión.

### mode=monitor

- Uso de CPU estable y muy bajo.
- GC activity mínima.
- No se observa comportamiento errático.
- Hilos en estado bloqueado cuando corresponde.
- En monitor, los hilos pasan a estado WAITING o BLOCKED.

### mode=spin

- Aunque en esta ejecución el CPU no parece alto, el modelo de espera activa implica consumo innecesario de ciclos.
- En pruebas más agresivas (más hilos o menos delay), el CPU suele incrementarse notablemente.
- Arquitectónicamente es menos eficiente.
- spin escala peor con más hilos.

## Preguntas Parte 1

1. ¿Porqué el consumo alto? ¿Qué clase lo causa?

- Hay consumo alto porque el productor/consumidor está usando un algoritmo de busy-wait (spin) que mantiene los hilos en RUNNABLE y en bucle, consumiendo CPU en vez de bloquearse

- La clase que lo causa es BusySpinQueue, ya que sus metodos put/take realizan spin loops

2. Implementación para usar CPU eficientemente cuando el productor es lento y el consumidor es rápido-

- Se modificó la implementación de BusySpinQueue con el objetivo de optimizar el uso de CPU sin eliminar completamente la estrategia de espera activa. En los métodos put() y take(), se mantuvieron secciones críticas sincronizadas mínimas para proteger el acceso a la cola; sin embargo, cuando la operación no puede completarse (cola llena o vacía), se implementó un mecanismo de backoff adaptativo en lugar de una espera activa pura.

El esquema adoptado opera en tres niveles progresivos: inicialmente se realiza spin activo mediante Thread.onSpinWait(), posteriormente se cede el procesador con Thread.yield(), y finalmente, si la condición persiste, el hilo entra en espera temporal utilizando LockSupport.parkNanos().

Este enfoque conserva baja latencia cuando la contención es breve (por ejemplo, cuando productor y consumidor están desfasados mínimamente), pero reduce significativamente el consumo de CPU en escenarios donde la espera es prolongada, como en el caso de productor lento y consumidor rápido. La principal contrapartida es una ligera penalización en la latencia de reanudación bajo condiciones de espera extendida.

3. Implementación para usar CPU eficientemente con productor rápido y consumidor lento con límite de stock, garantizando que el límite se respete sin espera activa y válida CPU con un stock pequeño.

- Se actualizó la aplicación incorporando un nuevo modo de ejecución denominado “bounded” o “blocking”, el cual instancia la clase BoundedBuffer en lugar de BusySpinQueue. En este modo, los métodos put() y take() utilizan el mecanismo de sincronización basado en wait()/notify(), garantizando el respeto estricto de la capacidad máxima del buffer sin recurrir a espera activa.

Adicionalmente, al finalizar la ejecución, PCApp consulta el tamaño final de la cola y lo compara con la capacidad configurada. En caso de detectarse una violación del límite, se muestra un mensaje de error, lo que facilita la validación experimental del comportamiento bajo configuraciones de stock reducido (por ejemplo, capacity=4, prodDelayMs=1, consDelayMs=50).

Este enfoque permite comprobar que el límite de capacidad se mantiene correctamente y que el consumo de CPU permanece bajo en comparación con el modo spin, donde la espera activa incrementa innecesariamente el uso del procesador en escenarios de alta contención.