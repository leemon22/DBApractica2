
# DBA · Práctica 2 — Contexto compartido y guía de continuidad

**Asignatura:** Diseño Basado en Agentes (UGR) · Curso 2026/2027  
**Última actualización del contexto:** 10/11/2026  
**Uso previsto:** incorporar este documento al repositorio y proporcionarlo como contexto a los asistentes de IA de los cuatro integrantes. **No sustituye los guiones oficiales ni el código fuente.**

> **Regla de trabajo para cualquier asistente:** distinguir entre **requisito del profesor**, **decisión del grupo**, **hecho verificado** y **pendiente de verificar**. Nunca tratar una compilación como prueba de funcionamiento, ni una ejecución correcta como validación definitiva del diseño. Revisar primero la versión más reciente de la rama correspondiente y los documentos docentes antes de proponer cambios.

## 1. Objetivo y material de referencia

**Guion:** *Práctica 2. Percepción y acción en entornos virtuales: del termostato a la navegación en 2D*, Manuel Jesús Cobo Martín.  
**Documentación docente entregada:**

- `Ingenieria.pdf`: enunciado general, calendario, mapas, evaluación.
- `guia-api-dba-lab.md`: acceso, ciclo de ejecución, rutas, autenticación, respuestas y errores.
- `guia-api-termostato.md`: sensores, acciones y objetivo del termostato.
- `guia-api-grid.md`: percepciones, desplazamiento, obstáculos y energía de Grid.
- `Tema1-Introduccion.pdf`: diapositivas teóricas del Tema 1; el tema define agentes situados, autonomía, percepción, interacción y sistemas multiagente. No confundir conceptos teóricos con requisitos de clases Java.
- `Tema2-Autonomia.pdf`: diapositivas del Tema 2. Su sección «Ejemplos de resolución de laberintos» (greedy, memoria con penalización, seguimiento de muros/Bug0-Bug1-Bug2, DFS, BFS con fronteras, RTA*/LRTA*) es la base del algoritmo de Grid (sección 7).

**Nota documental:** las copias recibidas de las guías específicas de Termostato y Grid están encabezadas por «Borrador para revisión docente. No publicar todavía». Confirmar con el profesor si existen revisiones posteriores; no asumir que las guías son versiones finales.

La práctica utiliza **Java y JADE** para desarrollar agentes que perciben, deciden y actúan en un entorno cuyo estado oficial mantiene un servicio remoto llamado **DBA Lab**. Se evalúan por separado:

| Bloque | Objetivo | Evaluación sobre 10 | Peso en prácticas |
|---|---|---|---|
| Termostato | Mantener temperatura dentro de límites hasta resultado oficial `SUCCEEDED` | 5 resolución + 5 diseño/implementación | 20 % |
| Grid | Alcanzar el objetivo sin colisiones y antes de agotar energía, con mapas iniciales y después desconocidos | 5 primera tanda + 4 segunda tanda + 1 diseño | 35 % |

**Entrega de código y resolución de retos:** miércoles **18/11/2026**. La defensa del termostato estaba prevista para la semana del 26/10 (30/10/2026); el grupo considera el bloque cerrado. **[DOCENTE]** Según el calendario del guion hay **una única defensa de Grid**, que abarca las dos tandas, en la **semana del 16/11/2026**, junto con la presentación del vídeo; no hay defensa la semana del 09/11. **[POR VALIDAR]** El calendario es orientativo y el guion no fija el día exacto: confirmar en clase o en PRADO. Las defensas son grupales, pero la comprensión individual del código puede afectar la nota personal. El vídeo de presentación se evalúa por separado.

## 2. Equipo y organización

- **Equipo de cuatro personas.** Un compañero propuso y desarrolló inicialmente el paquete `base` reutilizable.
- **[DECISIÓN GRUPO] Reparto de Grid (propuesto el 09/11/2026):**

| Integrante | Responsabilidad | Tareas |
|---|---|---|
| **Rodrigo** (Coordinación, algo más de carga) | Algoritmo de navegación e integración | `PoliticaGrid` con Bug0 con progreso y elección dinámica de mano (sección 7); integración final; estrategia para la segunda tanda. |
| **Pedro** | Comunicación con DBA Lab | Run exploratorio para medir el consumo de energía; extraer la lógica común de `EntornoTermostato` a `base.EntornoDba` y comprobar que el termostato sigue dando `SUCCEEDED`; implementar `EntornoGrid`. |
| **Martín** | Tipos, orientación y salvaguardas | ~~`Posicion`, `SensoresGrid`, `AccionGrid` y enum `Direccion`~~ (los implementó Rodrigo el 10/11, ver 7.3); detección de ciclos; control de energía. |
| **Cristina** | Simulador, pruebas y vídeo | Simulador local (`Entorno` falso) con los 7 mapas del guion; trazas paso a paso; tabla de resultados por reto; guion del vídeo. |

- Hitos propuestos: **10/11** run exploratorio hecho y `EntornoDba` integrado; **11/11** tipos de Grid y simulador funcionando; **12/11** política resolviendo los 7 mapas en el simulador y después en DBA Lab; **13/11–16/11** segunda tanda; **17/11** código congelado y revisión cruzada (cada integrante explica un módulo ajeno); **18/11** entrega.
- Evitar cambios no consensuados en `base`, porque debe servir para los dos entornos. Antes de integrar cambios, explicarlos al autor del paquete común.
- Todo integrante debe poder explicar el ciclo JADE, el cliente HTTP, la política, la interpretación de resultados y las decisiones tomadas en la defensa.

## 3. Repositorio y entorno local

- **Repositorio compartido:** https://github.com/leemon22/DBApractica2.git
- Rama predeterminada: `main`.
- Flujo de trabajo **[DECISIÓN GRUPO]**: cada cambio se hace en una rama propia, se sube a GitHub y entra en `main` mediante un PR.
- **Estado del repositorio [VERIFICADO el 09/11/2026 contra GitHub]:**

| PR | Rama → `main` | Contenido | Estado |
|---|---|---|---|
| #1 y #2 | `Martin` | Agente del termostato y configuración de JADE | Mergeados |
| #3 | `limpieza-termostato` | `runStatus` desconocido → `UNKNOWN`, limpieza de cabeceras, README | Mergeado |
| #4 | `reorganizacion-grid` | Paquete `grid` (solo `package-info.java`), lanzador movido a `lanzadores.LanzadorTermostato`, README dividido en Parte 1 y Parte 2 | Mergeado |
| #5 | `reorganizacion-grid` | Plan de Grid en este `context.md` y estado real del repositorio (commits `bf0c22b`, `494180c` y `2c68caf`) | Mergeado |

  `main` = `2898d80` (merge del PR #5). Las ramas `Martin`, `limpieza-termostato` y `reorganizacion-grid` ya están integradas y pueden borrarse. El trabajo de `PoliticaGrid` se hace en la rama `politica-grid` (Rodrigo). Ningún integrante ha subido todavía código de Grid. **[VERIFICADO]** GitHub rechaza la contraseña de la cuenta en `git push` por HTTPS: usar `gh auth login` o un token personal (*fine-grained*, permiso *Contents: Read and write*); ese token de GitHub no tiene relación con el de DBA Lab.
- Proyecto NetBeans basado en **Ant** (`build.xml`, `nbproject/`).
- JADE detectado en ejecución: **4.6.0**.
- Equipo utilizado durante estas pruebas: macOS ARM, **Temurin Java 25.0.2**. El proyecto originalmente traía `javac.source=27` y `javac.target=27`, lo que producía `invalid target release: 27`; para ese equipo se cambió a 25 y compiló. **Coordinar la versión Java del equipo antes de integrar cambios en `nbproject/project.properties`.**
- Gson se añadió localmente para analizar respuestas JSON. El profesor **no impone Gson**; su guía permite elegir el mecanismo de interpretación. Hay que comprobar que el JAR y la referencia del classpath se comparten de forma reproducible.
- Para JADE se añadió localmente el JAR correspondiente; el README del repositorio pide situarlo en `lib/` y configurar NetBeans.

Estructura del repositorio (`main`): un único proyecto NetBeans; cada bloque es un paquete y ambos reutilizan `base`. **[DECISIÓN GRUPO]** No se separa en dos proyectos para no duplicar `base`; el guion valora el código reutilizable entre bloques.

```text
DBApractica2/
├── src/
│   ├── base/
│   │   ├── Agente.java
│   │   ├── BucleAgentico.java
│   │   ├── ClienteHttpDba.java
│   │   ├── Configuracion.java
│   │   ├── Entorno.java
│   │   ├── EstadoControl.java
│   │   └── Politica.java
│   ├── termostato/                 # Bloque cerrado, resuelto con SUCCEEDED
│   │   ├── SensoresTermostato.java
│   │   ├── AccionTermostato.java
│   │   ├── PoliticaTermostato.java
│   │   └── EntornoTermostato.java
│   ├── grid/                       # Bloque Grid (por ahora solo package-info.java)
│   └── lanzadores/
│       ├── LanzadorTermostato.java # antes practica2/pkg1/Practica21.java; es main.class
│       └── LanzadorGrid.java       # pendiente
├── lib/                           # Dependencias JADE/Gson; revisar tracking
├── config.properties              # PRIVADO. Nunca versionar.
├── .gitignore
├── build.xml
└── README.md
```

**Interfaces del paquete común existentes en el repositorio revisado:**

```java
interface Entorno<S,A> {
    void iniciarRun(Configuracion config) throws Exception;
    S leerSensores() throws Exception;
    EstadoControl ejecutarAccion(A accion) throws Exception;
    EstadoControl getEstadoActual();
}
interface Politica<S,A> { A aplicar(S sensores); }
```

`Agente` extiende `jade.core.Agent`, recibe `Entorno` y `Politica` por argumentos, carga `config.properties`, inicia el entorno y añade `BucleAgentico`. El bucle `SimpleBehaviour` consulta estado, percibe, decide, actúa y termina cuando el estado deja de ser `ACTIVE`. `ClienteHttpDba` centraliza peticiones HTTP con token Bearer.

## 4. Acceso a DBA Lab y reglas de seguridad

- URL conocida del laboratorio: **http://orca.ugr.es:8080**.
- Prefijo de API: `/api/v1`.
- Acceso solo por **red UGR o VPN UGR**.
- Token **personal por estudiante**, recibido del profesor, que debe incluirse en cada petición: `Authorization: Bearer <token>`.
- Un token **NO se comparte**, ni siquiera dentro del grupo; no es el mismo token que GitHub. Cada estudiante solo puede consultar y operar sus propios runs; un único run activo por estudiante.
- `config.properties` en la **raíz del proyecto**, junto a `build.xml`:

```properties
DBA_API_URL=http://orca.ugr.es:8080/api/v1
DBA_TOKEN=<TOKEN_PERSONAL_NO_COMPARTIR>
```

- `config.properties` debe estar en `.gitignore`. Antes de cada commit comprobar que no está staged y que no se filtran credenciales en código, pantallazos ni logs compartidos.
- La URL incluye `/api/v1` **porque el `ClienteHttpDba` revisado concatena directamente** rutas como `/challenges`; comprobar este contrato si se modifica el cliente.
- **No iniciar runs sin motivo**: expiran por inactividad a los cinco minutos desde el inicio o la última acción aplicada. Las lecturas de sensores, consultas de estado y acciones rechazadas **no renuevan** el plazo.

### Contrato común de la API

1. `GET /api/v1/challenges`: devuelve `{ "challenges": [...] }` con `id`, `environmentType`, `sensorCodes`, `actionCodes`, etc. Seleccionar el reto por tipo/capacidades, **no fijar su ID**.
2. `POST /api/v1/runs` con `{"challengeId": <id>}`: devuelve `control.runId` y `control.runStatus`.
3. Mientras `control.runStatus == ACTIVE`: `POST /api/v1/runs/{runId}/perceptions/{sensorId}`; examinar `value` y `control`.
4. Decidir y solicitar acción con `POST /api/v1/runs/{runId}/actions`, cuerpo `{"actionCode":"..."}`. Examinar **ambos** `actionStatus` (`APPLIED`/`REJECTED`) y `control.runStatus`.
5. Estados terminales: `SUCCEEDED`, `FAILED`, `EXPIRED`, `INTERRUPTED`. Detener peticiones sobre runs terminados.
6. `GET /api/v1/runs/{runId}` consulta el estado. `POST /api/v1/runs/{runId}/close` permite abandonar un run como `INTERRUPTED` (no resuelto).
7. Revisar **primero el código HTTP**, después el JSON de respuesta; errores relevantes `401`, `404`, `409`, `422`, `503`. Si se pierde respuesta de un POST con efectos, **no repetirlo automáticamente**. Investigar con endpoints de estado y `GET /api/v1/runs/{runId}/actions/last`.

El servidor devolvió **HTTP 200** a una consulta autenticada de descubrimiento en la red UGR/VPN durante esta sesión. Esto verifica conexión/autenticación para ese momento, no garantiza disponibilidad futura.

## 5. Termostato: especificación y decisiones de diseño

El entorno representa una cámara refrigerada. Los extremos del intervalo de seguridad **están incluidos**. La temperatura orientativa ayuda a decidir, pero alcanzarla no resuelve por sí solo el reto. El agente debe mantenerse dentro de límites durante los pasos establecidos por el servidor, sin deducir localmente cuántos faltan. El termostato **no usa energía**. Cada acción aplicada cambia/fija refrigeración, avanza un paso y puede haber perturbaciones externas. Leer sensores **no** avanza la simulación.

| Sensor | Datos JSON dentro de `value` |
|---|---|
| `thermostat.temperature` | `celsius` |
| `thermostat.limits` | `lower`, `upper` |
| `thermostat.target` | `celsius` |

| Acción | Código |
|---|---|
| Encender | `thermostat.refrigeration.on` |
| Apagar | `thermostat.refrigeration.off` |

**Implementación local realizada:**

- `SensoresTermostato.java`: `record` con `temperatura`, `limiteInferior`, `limiteSuperior`, `orientativa` (`double`). El uso de `record` es decisión del grupo, no exigencia docente.
- `AccionTermostato.java`: enum `ON`/`OFF` con sus códigos exactos. El uso de `enum` tampoco lo prescribe el profesor.
- `PoliticaTermostato.java`: implementa `Politica<SensoresTermostato,AccionTermostato>`; política básica: refrigerar si `temperatura >= superior`, apagar si `temperatura <= inferior`, y dentro de límites comparar con `orientativa` (`ON` por encima, `OFF` en caso contrario). Esta **heurística es elección del grupo y no garantía de seguridad para todos los escenarios**; debe defenderse y probarse contra perturbaciones.
- `EntornoTermostato.java`: implementa `Entorno<SensoresTermostato,AccionTermostato>` y usa `ClienteHttpDba` + Gson para descubrir el reto tipo `THERMOSTAT`, validar sensores/acciones anunciados, iniciar run, leer las tres percepciones, actualizar `EstadoControl`, emitir acción y comprobar `actionStatus`/`runStatus`. Desde el 29/10, un `runStatus` no reconocido se traduce a `RunStatus.UNKNOWN` (método `convertirEstado`) en lugar de lanzar `IllegalArgumentException`; como no es `ACTIVE`, el bucle se detiene. Se hizo en el paquete `termostato` para no modificar `base` sin consenso; al pasar la lógica común a `base` para Grid, debería moverse allí.
- `LanzadorTermostato.java` (antes `Practica21.java`): punto de entrada que inicia JADE, crea el contenedor principal y el agente `base.Agente` con `EntornoTermostato` y `PoliticaTermostato`.

**Estado en `main` (29/10/2026):** este código está integrado en `main` (PR #2). Se eliminaron los comentarios de plantilla de NetBeans y se añadió una descripción a cada clase. La lógica genérica de DBA Lab (descubrimiento, validación de capacidades, lectura de `control`, sensores y acciones) sigue dentro de `EntornoTermostato`; **pendiente** extraerla a `base` antes de Grid.

**Observación técnica a revisar:** la lógica de excepción y de finalización cambió varias veces durante el desarrollo. La última ejecución satisfactoria confirma terminación normal, pero antes de integrar el proyecto hay que inspeccionar el código exacto, evitar clasificar errores de red como finalización normal y comprobar el caso de estado terminal detectado durante la lectura de sensores. No dar por hecha la corrección de todas las rutas excepcionales basándose solo en las pruebas felices.

## 6. Evidencia de ejecución real

Se observaron dos ejecuciones satisfactorias del reto del termostato:

| Run | Estado final | Acciones aceptadas | Observaciones |
|---|---|---:|---|
| `368` | `SUCCEEDED` | 10 de 10 | El agente terminó, pero la plataforma JADE quedó activa. |
| `382` | `SUCCEEDED` | 10 de 10 | Se observó cierre de JADE sin las advertencias previas y `BUILD SUCCESSFUL (total time: 4 seconds)`. |

Secuencia observada del run **382**: `OFF, ON, OFF, ON, OFF, ON, OFF, OFF, ON, OFF`; todas `APPLIED`. Última acción devolvió `control.runStatus = SUCCEEDED`.

**Resultado verificado:** el reto de termostato publicado (tipo `THERMOSTAT`, ID observado `1`) se ha resuelto al menos dos veces. **No hay todavía validación docente del diseño/defensa ni auditoría final del código para todos los errores de red**.

### Incidencias resueltas durante el desarrollo

1. **`invalid target release: 27`**: proyecto configurado para JDK 27, máquina con Java 25. Ajuste local de compilación a 25.
2. **Imports JADE no resueltos**: añadir `jade.jar` al classpath de NetBeans.
3. **JSON**: añadir Gson al classpath; no es una dependencia prescrita por el guion.
4. **JADE no cerraba al acabar el agente**: el agente terminaba pero seguía vivo el contenedor principal.
5. **Intentos descartados**: `AgentController.join()` (no existe), `AgentState.STATE_DELETED` (constante no válida en el entorno usado), `doPlatformExit()` desde `Agent` (no existe), y `getContainerController().kill()` **dentro de** `Agente.takeDown()` (produjo advertencias `Agent did not terminate` y una excepción durante el cierre).
6. **Cierre observado como funcional:** restaurar `takeDown()` a solo registro de salida; mantener `runtime.setCloseVM(true)` en el lanzador y gestionar `contenedor.kill()` desde un hilo externo en el lanzador (entonces `Practica21.java`), que espera hasta que `agente.getState()` falle con `ControllerException`. **Advertencia:** es la estrategia probada en esa ejecución, pero la condición basada en que `getState()` falle no es un contrato de finalización documentado aquí; revisar robustez con el compañero responsable de Base y evitar reutilizarla ciegamente en Grid o entornos multiagente. **[POR VALIDAR] Ese hilo de cierre no está en el lanzador de `main` (`Practica21.java`, hoy `lanzadores/LanzadorTermostato.java`) (revisado el 29/10/2026):** solo hay `runtime.setCloseVM(true)`. Comprobar si la plataforma JADE queda abierta al terminar el agente.

## 7. Grid: especificación, algoritmo y plan

### 7.1 Entorno

La API anuncia los retos tipo `GRID_2D` con sensores:

- `grid.position`: `x`, `y` (x crece a la derecha; y hacia abajo).
- `grid.bumpers`: `north`, `east`, `south`, `west` (`true` = obstáculo/límite).
- `grid.target`: `x`, `y`.
- `grid.energy`: `mode`, `remaining` (energía finita según guía).

Acciones `grid.move.north`, `.east`, `.south`, `.west`; una colisión o salida de límites termina el run. La respuesta de acción **no incluye posición ni energía actualizadas**; consultar después si continúa activo. El agente **no recibe mapa completo** de la API, y para la segunda tanda necesitará exploración y memoria del entorno. La eficiencia de ruta no puntúa adicionalmente, pero el presupuesto energético sí condiciona superar el reto.

En la respuesta real de `challenges` se observaron IDs `2` a `8` para primera tanda: campo abierto, muro vertical, muro horizontal, muro diagonal, barrera en U, barrera en arco y barrera en C. **Los IDs son un dato observado, no deben codificarse como constantes de selección.** En el momento de la observación no se verificó una segunda tanda publicada.

### 7.2 Algoritmo elegido: seguimiento de muros (Bug0 con progreso y elección dinámica de mano)

**[DOCENTE, indicación en clase]** El profesor desaconsejó A* porque el agente solo percibe las celdas vecinas y sugirió el **seguimiento de muro «con ambas manos»**, explicado en `Tema2-Autonomia.pdf` (diapositivas «Bug0 con progreso» y «Elección dinámica de mano»). **[DECISIÓN GRUPO]** Se adopta este algoritmo; se descarta el A* sobre mapa de creencias propuesto inicialmente. El README y `src/grid/package-info.java` ya describen el algoritmo nuevo (10/11/2026).

- **Notación de las diapositivas:** Wall-e = agente; EVA = objetivo (`grid.target`).
- **Heurística:** distancia Manhattan `h = |x − xEVA| + |y − yEVA|`. Con obstáculos, el recorrido real puede ser mayor.
- **Desempate fijo:** arriba (N), derecha (E), abajo (S), izquierda (O).
- **Estado interno:** modo, orientación (dirección del último movimiento), mano elegida, `d0` y contador de decisiones de seguimiento.

| Modo | Comportamiento | Cambio de modo |
|---|---|---|
| ACERCAMIENTO | Mover a la primera dirección **libre** (bumper `false`) que **reduce** h. | Ninguna dirección que reduce h está libre: guardar `d0 = h(pos)`, elegir mano, pasar a SEGUIMIENTO. |
| SEGUIMIENTO | Rodear el obstáculo con la mano elegida. | Se cumple la condición de salida. |

**Elección de mano al chocar:** comparar las dos celdas laterales respecto a la dirección bloqueada; elegir la libre con menor h; en empate, preferencia por defecto (mano derecha en el ejemplo docente). Girar a la izquierda deja el muro a la derecha → **mano derecha**; girar a la derecha → **mano izquierda**. La elección se mantiene durante ese rodeo.

**Giros relativos a la orientación:**

| Orientación | Izquierda | Frente | Derecha | Retroceso |
|---|---|---|---|---|
| ↑ N | ← O | ↑ N | → E | ↓ S |
| → E | ↑ N | → E | ↓ S | ← O |
| ↓ S | → E | ↓ S | ← O | ↑ N |
| ← O | ↓ S | ← O | ↑ N | → E |

- Mano izquierda: prioridad izquierda, frente, derecha, retroceso.
- Mano derecha: prioridad derecha, frente, izquierda, retroceso.

**Condición de salida (Bug0 con progreso):** `h(pos) < d0` **y** el mejor movimiento de acercamiento está libre **y** (variante «pérdida de contacto») el agente ha perdido el contacto lateral con el muro tras **≥ 2 decisiones** de seguimiento. La diapositiva ofrece otra variante («criterio lateral»). **[POR DECIDIR]** Elegir una variante y justificarla en la defensa.

**Riesgos y salvaguardas [DECISIÓN GRUPO, por implementar]:**
- Bug0 no garantiza llegar en cualquier mapa: puede repetir rodeos. Detectar ciclos (repetición de la tupla posición, orientación, modo, mano) y reaccionar cambiando de mano o pasando a otra estrategia del Tema 2 (p. ej., exploración por fronteras con BFS).
- El algoritmo no considera la energía: vigilar `remaining` frente a h y registrar el consumo real.
- Seguridad: no enviar nunca un movimiento hacia una dirección con bumper `true`; las colisiones terminan el run.

### 7.3 Clases previstas

| Clase | Responsabilidad | Responsable |
|---|---|---|
| `Posicion` | *Record* `x`, `y`; `distanciaManhattan(otra)` y `mover(direccion)`. **Hecho.** | Rodrigo |
| `Direccion` | Enum `NORTE`, `ESTE`, `SUR`, `OESTE` (en ese orden: sentido horario y desempate del Tema 2) con `dx()`, `dy()` (norte = `dy -1`), `izquierda()`, `derecha()`, `retroceso()`, `codigoAccion()` (`grid.move.*`) y `campoBumper()` (`north`…). **Hecho.** | Rodrigo |
| `SensoresGrid` | *Record* `posicion`, `objetivo`, `Set<Direccion> bloqueadas` (bumpers a `true`, copia inmutable), `modoEnergia`, `energiaRestante`; `bloqueado(d)`, `libre(d)`, `distanciaAlObjetivo()`, `enObjetivo()`. **Hecho.** | Rodrigo |
| ~~`AccionGrid`~~ | **[DECISIÓN GRUPO, 10/11]** No se crea: la acción es la propia `Direccion` (un movimiento *es* una dirección y ya conoce su código). | — |
| `EntornoDba` (en `base`) | Lógica común de DBA Lab extraída de `EntornoTermostato`. | Pedro |
| `EntornoGrid` | Implementa `Entorno<SensoresGrid, Direccion>`; lee el objetivo una sola vez. Para los bumpers puede recorrer `Direccion.values()` y leer `campoBumper()`. | Pedro |
| `PoliticaGrid` | Implementa `Politica<SensoresGrid, Direccion>` con el algoritmo de 7.2. | Rodrigo |
| `DetectorCiclos` / control de energía | Salvaguardas de 7.2. | Martín |
| `SimuladorGrid` | `Entorno` local con los 7 mapas transcritos del guion, para pruebas sin VPN ni runs. | Cristina |
| `LanzadorGrid` | Punto de entrada del bloque. | Rodrigo (integración) **[POR CONFIRMAR]** |

**Observación de diseño:** `BucleAgentico` llama a `leerSensores()` en cada paso. En Grid, leer los cuatro sensores por movimiento puede desperdiciar energía si las lecturas la consumen; `grid.target` no cambia y basta leerlo al iniciar.

### 7.4 Pendiente de verificar antes de cerrar el diseño

- **[POR VALIDAR]** Si leer sensores consume energía o solo la consumen los movimientos, y cuánto cuesta cada movimiento. Run exploratorio en Campo abierto: leer `grid.energy` dos veces; leer posición, objetivo y bumpers (formato JSON real); un movimiento libre y leer energía; leer posición (confirmar ejes); cerrar el run con `POST /runs/{id}/close`. Registrar los resultados aquí como **[VERIFICADO]**.
- **[POR VALIDAR]** Si los retos se resuelven una vez por grupo o con el token de cada integrante (preguntar al profesor).
- **[POR VALIDAR]** La guía `guia-api-grid.md` es un borrador docente; confirmar si hay versión revisada.

## 8. Antes de un commit / push / merge

1. Comparar la rama local con `main` (`git status`, `git diff`, y revisión de los archivos modificados). **No asumir que el archivo local está en GitHub solo porque compila.**
2. Confirmar que `config.properties`, tokens, secretos y credenciales no están en staging ni en el historial de commits.
3. Verificar que las dependencias JADE y Gson pueden resolverse al clonar en otra máquina y que el classpath **no depende de rutas absolutas del Mac**. `lib/` está en `.gitignore`: cada integrante crea `lib/jade.jar` (de `JADE-bin-4.6.0.zip`) y `lib/gson-2.15.0.jar` (Maven Central); el README lo explica. `DBA_API_URL` debe terminar en `/api/v1`; sin ese prefijo las peticiones fallan (incidencia detectada el 29/10 en un `config.properties` local).
4. Coordinar la versión del JDK (`javac.source`/`javac.target`), evitando forzar unilateralmente Java 25 a compañeros con otra instalación.
5. Acordar con el autor del paquete `Base` cualquier modificación en `Agente.java` o `BucleAgentico.java`.
6. Revisar políticas de errores HTTP, comprobación de capacidades, estados terminales, acciones rechazadas y recuperación ante respuestas perdidas; no reintentar POST de efectos sin determinar su resultado.
7. Compilar (`Clean and Build`); después, si se requiere, ejecutar una prueba controlada en DBA Lab con token propio y VPN. Cada prueba real crea un run y puede fallar o expirar.
8. Subir la rama propia y abrir revisión/PR antes de mezclar en `main` (si el equipo así lo acuerda).

## 9. Instrucciones para un asistente de IA que continúe

Al recibir este archivo, procede así:

1. **Lee primero** el enunciado (`Ingenieria.pdf`), las tres guías docentes y la versión actual del repositorio, incluidos los cambios locales/ramas si son accesibles.
2. Pregunta o comprueba qué parte asume el integrante (termostato, Grid primera tanda, Grid exploración, Base/integración); **no sobrescribas código de otros** ni introduzcas cambios innecesarios.
3. Distingue con etiquetas o lenguaje explícito: **[DOCENTE]**, **[DECISIÓN GRUPO]**, **[VERIFICADO]**, **[POR VALIDAR]**. No atribuyas requisitos al profesor si el documento no los contiene.
4. Da instrucciones **paso a paso**, con un cambio acotado y una comprobación clara; no avances diciendo que algo funciona hasta ver el resultado.
5. Usa los códigos y campos exactos de la API. No inventes JSON, nombres de endpoints, identificadores de retos o tipos de sensores. Las lecturas no mueven el entorno; una acción sí puede cambiarlo o terminar el run.
6. **Nunca solicites que el usuario pegue su token**. Si hacen falta pruebas, prepara mecanismos locales que no muestren credenciales y evita incluirlas en logs o documentación.
7. En asuntos de JADE, evita repetir soluciones descartadas de cierre. Inspecciona la versión de JADE y el código real antes de proponer métodos o constantes.
8. Recuerda que **Termostato ya tiene ejecuciones `SUCCEEDED` (368 y 382)**; prioriza revisión/integración/defensa antes de generar ejecuciones idénticas sin propósito.
9. En Grid, diseña la navegación considerando percepción parcial, obstáculos terminales y energía limitada. El algoritmo acordado es el seguimiento de muros de la sección 7.2 (indicación del profesor); no propongas sustituirlo por A* u otro planificador sin que el grupo lo decida. Para probar, usa primero el simulador local antes de gastar runs en DBA Lab.
10. Documenta qué se cambió, por qué, qué resultado produjo y qué queda por revisar para el resto del grupo.

---

**Resumen de estado al 10/11/2026:** `Base` existente; Termostato integrado en `main`, resuelto (runs 368 y 382) y bloque cerrado; reorganización del repositorio (paquete `grid`, `lanzadores`, README dividido) integrada en `main` (PRs #3 y #4); el plan de Grid de este documento, integrado en `main` (PR #5). Grid: algoritmo acordado (Bug0 con progreso y elección dinámica de mano, Tema 2) y reparto asignado (sección 2). Tipos de Grid (`Direccion`, `Posicion`, `SensoresGrid`) hechos en la rama `politica-grid` (10/11), compilan y pasan una prueba local de giros, desplazamientos y Manhattan. Siguientes pasos: `PoliticaGrid`, run exploratorio de energía, `EntornoDba` en `base` y simulador local. Entrega el 18/11/2026; defensa de Grid la semana del 16/11/2026.
