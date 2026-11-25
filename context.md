
# DBA · Práctica 2 — Contexto compartido y guía de continuidad

**Asignatura:** Diseño Basado en Agentes (UGR) · Curso 2026/2027  
**Última actualización del contexto:** 24/11/2026
**Uso previsto:** incorporar este documento al repositorio y proporcionarlo como contexto a los asistentes de IA de los cuatro integrantes. **No sustituye los guiones oficiales ni el código fuente.**

> **Regla de trabajo para cualquier asistente:** distinguir entre **requisito del profesor**, **decisión del grupo**, **hecho verificado** y **pendiente de verificar**. Nunca tratar una compilación como prueba de funcionamiento, ni una ejecución correcta como validación definitiva del diseño. Revisar primero la versión más reciente de la rama correspondiente y los documentos docentes antes de proponer cambios.

## 1. Objetivo y material de referencia

**Guion:** *Práctica 2. Percepción y acción en entornos virtuales: del termostato a la navegación en 2D*, Manuel Jesús Cobo Martín. 

**Documentación docente entregada:**
- `Pr2.pdf`: enunciado general, calendario, mapas, evaluación.
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

**Entrega de código y resolución de retos:** prevista en el guion para el miércoles **18/11/2026**; **[DECISIÓN GRUPO / según el grupo, 24/11]** la entrega del código es el **miércoles 25/11/2026**. La defensa del termostato estaba prevista para la semana del 26/10 (30/10/2026); el grupo considera el bloque cerrado. **[DOCENTE]** Según el calendario del guion hay **una única defensa de Grid**, que abarca las dos tandas, en la **semana del 16/11/2026**, junto con la presentación del vídeo; no hay defensa la semana del 09/11. **[POR VALIDAR]** El calendario es orientativo y el guion no fija el día exacto: confirmar en clase o en PRADO. Las defensas son grupales, pero la comprensión individual del código puede afectar la nota personal. El vídeo de presentación se evalúa por separado.

## 2. Equipo y organización

- **Equipo de cuatro personas.** Rodrigo propuso y desarrolló inicialmente el paquete `base` reutilizable.
- **[DECISIÓN GRUPO] Reparto de Grid (propuesto el 09/11/2026):**

| Integrante | Responsabilidad | Tareas |
|---|---|---|
| **Rodrigo** (Coordinación, algo más de carga) | Algoritmo de navegación e integración | `PoliticaGrid` (V1) con Bug0 con progreso y elección dinámica de mano (sección 7). **Hecho.** Banco de pruebas `test/grid/BancoMapasGrid.java` y comparación V1/V2 en simulación y en DBA Lab (sección 7.5). **Hecho (24/11).** Integración final y estrategia para la segunda tanda: se adopta la V2 (7.5). |
| **Pedro** | Comunicación con DBA Lab y componentes de Grid | Generalizar la lógica común a `base.Entorno`; verificar que el termostato sigue dando `SUCCEEDED` (verificado en run 1686); implementar `EntornoGrid`, `AgenteGrid`, `BucleAgenticoGrid`, `VisualizadorGridCli` y `LanzadorGrid`. **Hecho.** `PoliticaGridV2`, política activa (22–23/11). **Hecho.** |
| **Martín** | Tipos, orientación y salvaguardas | ~~`Posicion`, `SensoresGrid`, `AccionGrid` y enum `Direccion`~~ (los implementó Rodrigo el 10/11, ver 7.3); detección de ciclos y control de energía dentro de `PoliticaGrid` (PR #7, 20/11). **Hecho.** |
| **Cristina** | Simulador, pruebas y vídeo | Simulador local (`Entorno` falso) con los 7 mapas del guion; trazas paso a paso; tabla de resultados por reto; guion del vídeo. Vídeo **en curso** (24/11). |

- Hitos propuestos: **10/11** run exploratorio hecho y `base.Entorno` generalizado (**Hecho**); **11/11** tipos de Grid, componentes agénticos y visualizador funcionando (**Hecho**); **12/11** política resolviendo los 7 mapas en el simulador y después en DBA Lab; **13/11–16/11** segunda tanda; **17/11** código congelado y revisión cruzada (cada integrante explica un módulo ajeno); **18/11** entrega.
- Evitar cambios no consensuados en `base`, porque debe servir para los dos entornos. Antes de integrar cambios, explicarlos al autor del paquete común.
- Todo integrante debe poder explicar el ciclo JADE, el cliente HTTP, la política, la interpretación de resultados y las decisiones tomadas en la defensa.

## 3. Repositorio y entorno local

- **Repositorio compartido:** https://github.com/leemon22/DBApractica2.git
- Rama predeterminada: `main`.
- Flujo de trabajo **[DECISIÓN GRUPO]**: cada cambio se hace en una rama propia, se sube a GitHub y entra en `main` mediante un PR.
- **Estado del repositorio [VERIFICADO el 24/11/2026 contra GitHub]:**

| PR | Rama → `main` | Contenido | Estado |
|---|---|---|---|
| #1 y #2 | `Martin` | Agente del termostato y configuración de JADE | Mergeados |
| #3 | `limpieza-termostato` | `runStatus` desconocido → `UNKNOWN`, limpieza de cabeceras, README | Mergeado |
| #4 | `reorganizacion-grid` | Paquete `grid` (solo `package-info.java`), lanzador movido a `lanzadores.LanzadorTermostato`, README dividido en Parte 1 y Parte 2 | Mergeado |
| #5 | `reorganizacion-grid` | Plan de Grid en este `context.md` y estado real del repositorio (commits `bf0c22b`, `494180c` y `2c68caf`) | Mergeado |
| #6 | `politica-grid` | `Direccion`, `Posicion`, `SensoresGrid` y `PoliticaGrid` (Rodrigo) | Mergeado |
| — | (directo a `main`) | Refactor de `base` y termostato; `EntornoGrid`, `AgenteGrid`, `BucleAgenticoGrid`, `VisualizadorGridCli`, `LanzadorGrid` y `makefile` (Pedro, 12–17/11) | En `main` |
| #7 | `Martin` | Detección de ciclos y parada por energía en `PoliticaGrid`; `test/grid/PoliticaGridPrueba.java` (Martín) | Mergeado |
| — | (directo a `main`) | `PoliticaGridV2`, activa en `AgenteGrid`; pruebas de V2 (Pedro, 22–23/11) | En `main` |
| #8 | `cambios-context` | `Ingenieria.pdf` → `Pr2.pdf` y fila de `PoliticaGridV2` en este documento | Mergeado |
| — | `docs-resultados-grid` | Banco `test/grid/BancoMapasGrid.java`, resultados de Grid en el README y este `context.md` al día (Rodrigo, 24/11) | Pendiente de PR |

  `main` = `c0138bd` (merge del PR #8). Las ramas `Martin`, `limpieza-termostato`, `reorganizacion-grid` y `politica-grid` ya están integradas y pueden borrarse. **[VERIFICADO]** GitHub rechaza la contraseña de la cuenta en `git push` por HTTPS: usar `gh auth login` o un token personal (*fine-grained*, permiso *Contents: Read and write*); ese token de GitHub no tiene relación con el de DBA Lab.
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
│   │   ├── Accion.java
│   │   ├── Agente.java
│   │   ├── BucleAgentico.java
│   │   ├── ClienteHttpDba.java
│   │   ├── ComponentesAgente.java
│   │   ├── Configuracion.java
│   │   ├── Entorno.java
│   │   ├── EstadoControl.java
│   │   ├── InfoReto.java
│   │   ├── LecturaSensor.java
│   │   ├── Politica.java
│   │   ├── RespuestaAccion.java
│   │   ├── Sensor.java
│   │   └── Visualizador.java
│   ├── termostato/                 # Bloque cerrado, resuelto con SUCCEEDED
│   │   ├── AccionTermostato.java
│   │   ├── AgenteTermostato.java
│   │   ├── BucleAgenticoTermostato.java
│   │   ├── EntornoTermostato.java
│   │   ├── LimitesTermostato.java
│   │   ├── PoliticaTermostato.java
│   │   ├── SensoresTermostato.java
│   │   └── VisualizadorTermostato.java
│   ├── grid/                       # Bloque Grid
│   │   ├── AgenteGrid.java
│   │   ├── BucleAgenticoGrid.java
│   │   ├── Direccion.java
│   │   ├── EntornoGrid.java
│   │   ├── PoliticaGrid.java
│   │   ├── PoliticaGridV2.java
│   │   ├── Posicion.java
│   │   ├── SensoresGrid.java
│   │   ├── VisualizadorGridCli.java
│   │   └── package-info.java
│   └── lanzadores/
│       ├── LanzadorTermostato.java
│       └── LanzadorGrid.java
├── test/
│   └── grid/                       # Pruebas locales sin JADE ni DBA Lab (solo JDK)
│       ├── PoliticaGridPrueba.java # Casos sintéticos de V1 y V2
│       └── BancoMapasGrid.java     # 7 mapas del guion + mapas aleatorios, V1 contra V2
├── lib/                           # Dependencias JADE/Gson; revisar tracking
├── config.properties              # PRIVADO. Nunca versionar.
├── .gitignore
├── build.xml
├── makefile                       # make termostato / make grid
└── README.md
```

**Contratos del paquete base (`base`):**

```java
public interface Sensor {}
public interface Accion {}

public abstract class Entorno<S extends Sensor, A extends Accion> {
    public abstract void iniciarRun() throws IOException, InterruptedException;
    public abstract S leerSensores() throws IOException, InterruptedException;
    public abstract void ejecutarAccion(A accion) throws IOException, InterruptedException;
    public boolean isActivo();
    public Optional<EstadoControl> getEstadoControl();
    public void cerrarRun() throws IOException, InterruptedException;
}
public interface Politica<S extends Sensor, A extends Accion> {
    A aplicar(S sensores);
}
public interface Visualizador<S extends Sensor, A extends Accion> {
    void mostrarRetoEncontrado(InfoReto reto);
    void mostrarInicio(String runId, EstadoControl estado);
    void mostrarPercepcion(S sensores);
    void mostrarAccion(A accion, String resultado, EstadoControl nuevoEstado);
    void mostrarCierre(String runId);
    void mostrarFin(EstadoControl estadoFinal);
}
public record ComponentesAgente<S extends Sensor, A extends Accion>(
    Entorno<S, A> entorno,
    BucleAgentico<S, A> bucle
) {}
```

`Agente<S extends Sensor, A extends Accion>` es una clase abstracta que extiende `jade.core.Agent`. Su método `setup()` ejecuta `iniciar()` (que devuelve `ComponentesAgente`), inicia el run en el entorno y añade el bucle agéntico con `addBehaviour()`. En `takeDown()` invoca `entorno.cerrarRun()` para no dejar runs zombie. `BucleAgentico<S, A>` ejecuta el ciclo canónico percepción-decisión-acción y delega el control de parada en `antes()` y `despues()`. `ClienteHttpDba` encapsula completamente la serialización y deserialización JSON con Gson, devolviendo modelos Java puros (`InfoReto`, `LecturaSensor`, `RespuestaAccion`, `EstadoControl`).

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

- `SensoresTermostato.java`: `record` con `temperaturaActual`, `limiteInferior`, `limiteSuperior` y `temperaturaObjetivo` (`double`). Implementa `Sensor` y se instancia directamente desde los objetos JSON recibidos de la API.
- `AccionTermostato.java`: enum `ON`/`OFF` que implementa `Accion` con sus códigos exactos de acción.
- `PoliticaTermostato.java`: implementa `Politica<SensoresTermostato, AccionTermostato>`: encender refrigeración (`ON`) si `temperaturaActual >= temperaturaObjetivo`; en caso contrario, apagar (`OFF`). Un único punto de retorno y asignación directa.
- `EntornoTermostato.java`: implementa `Entorno<SensoresTermostato, AccionTermostato>` y usa `ClienteHttpDba` para descubrir el reto `THERMOSTAT`, validar capacidades, iniciar el run, leer percepciones y ejecutar acciones comprobando `actionStatus` y `runStatus`. Se apoya en constructores de `SensoresTermostato` y `EstadoControl` a partir de `JsonObject`. Acota sus excepciones a `IOException` e `InterruptedException`.
- `AgenteTermostato.java`: subclase de `Agente<SensoresTermostato, AccionTermostato>` que inicializa en `iniciar()` el entorno, la política y el bucle agéntico del termostato.
- `BucleAgenticoTermostato.java`: subclase de `BucleAgentico<SensoresTermostato, AccionTermostato>` que valida en `antes()` y `despues()` si el run continúa `ACTIVE`.
- `LanzadorTermostato.java`: punto de entrada que inicia JADE, crea el contenedor principal y lanza `termostato.AgenteTermostato`.

**Estado en rama `refactor-termostato` (noviembre 2026):** refactorización de arquitectura base y termostato según correcciones docentes: interfaces `Sensor`/`Accion`, retardo configurable en `ClienteHttpDba` para no saturar el servidor, clases abstractas `Agente` y `BucleAgentico` fuertemente tipadas y simplificación de `PoliticaTermostato`. Probado en ejecución real manteniendo el comportamiento `SUCCEEDED`.

**Observación técnica a revisar:** la lógica de excepción y de finalización cambió varias veces durante el desarrollo. La última ejecución satisfactoria confirma terminación normal, pero antes de integrar el proyecto hay que inspeccionar el código exacto, evitar clasificar errores de red como finalización normal y comprobar el caso de estado terminal detectado durante la lectura de sensores. No dar por hecha la corrección de todas las rutas excepcionales basándose solo en las pruebas felices.

## 6. Evidencia de ejecución real

Se observaron dos ejecuciones satisfactorias del reto del termostato:

| Run | Estado final | Acciones aceptadas | Observaciones |
|---|---|---:|---|
| `368` | `SUCCEEDED` | 10 de 10 | El agente terminó, pero la plataforma JADE quedó activa. |
| `382` | `SUCCEEDED` | 10 de 10 | Se observó cierre de JADE sin las advertencias previas y `BUILD SUCCESSFUL (total time: 4 seconds)`. |
| `1686` | `SUCCEEDED` | 10 de 10 | Tras la refactorización de `base` y del termostato (Pedro). |

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

**[VERIFICADO el 24/11/2026 con `GET /challenges`]** Hay 19 retos `GRID_2D`, todos con `agentTypeCode` `dba-2026-2027-grid-finite-energy`. **Los IDs son un dato observado, no deben codificarse como constantes de selección.**

| Tanda | ID | Nombre en DBA Lab |
|---|---|---|
| 1 | 2–8 | Fase 1 - 01 Campo abierto, 02 Muro vertical, 03 Muro horizontal, 04 Muro diagonal, 05 Barrera en U, 06 Barrera en arco, 07 Barrera en C |
| 2 | 9 | Fase 2 - 01 - Corredores horizontales |
| 2 | 10 | Fase 2 - 02 - Corredores verticales |
| 2 | 11 | Fase 2 - 03 - Barrera triangular |
| 2 | 12 | Fase 2 - 04 - Recintos conectados |
| 2 | 13 | Fase 2 - 05 - Barreras en U y L |
| 2 | 14 | Fase 2 - 06 - Laberinto central |
| 2 | 15 | Fase 2 - 07 - Bloques conectados |
| 2 | 16 | Fase 2 - 08 - Disco con hendidura |
| 2 | 17 | Fase 2 - 09 - Pasillos estrechos |
| 2 | 18 | Fase 2 - 10 - Obstáculos dispersos A |
| 2 | 19 | Fase 2 - 11 - Obstáculos dispersos B |

**[VERIFICADO en runs 3141–3146]** Energía y tamaño:

- **Cada movimiento cuesta 1 de energía; leer sensores no cuesta nada.** En los 6 runs, la energía leída en la percepción *k* es exactamente la inicial menos *k − 1*.
- Los mapas de la segunda tanda son mucho mayores que los de la primera (al menos 50 filas; en los retos 9, 13 y 18 el inicio está en la fila 49 y el objetivo en las filas 0–21).
- Presupuesto observado: reto 9 → 210 (el profesor lo subió a **220** el 24/11), reto 13 → 160, reto 18 → 200. La distancia Manhattan inicio–objetivo es 49, 59 y 57: el presupuesto es de 2,7 a 4,5 veces la Manhattan.

### 7.2 Algoritmo elegido: seguimiento de muros (Bug0 con progreso y elección dinámica de mano)

**[DOCENTE, indicación en clase]** El profesor desaconsejó A* porque el agente solo percibe las celdas vecinas y sugirió el **seguimiento de muro «con ambas manos»**, explicado en `Tema2-Autonomia.pdf` (diapositivas «Bug0 con progreso» y «Elección dinámica de mano»). **[DECISIÓN GRUPO]** Se adopta este algoritmo; se descarta el A* sobre mapa de creencias propuesto inicialmente. El README y `src/grid/package-info.java` ya describen el algoritmo nuevo (10/11/2026).

- **Notación de las diapositivas:** Wall-e = agente; EVA = objetivo (`grid.target`).
- **Heurística:** distancia Manhattan `h = |x − xEVA| + |y − yEVA|`. Con obstáculos, el recorrido real puede ser mayor.
- **Desempate fijo:** arriba (N), derecha (E), abajo (S), izquierda (O).
- **Estado interno:** modo, orientación (dirección del último movimiento), mano elegida, `d0`, conjunto de estados de seguimiento visitados y si ya se probó el cambio de mano.

Esta sección describe la **V1** (`PoliticaGrid`). La política activa es la **V2** (`PoliticaGridV2`), que parte de este mismo algoritmo; sus diferencias y el motivo de la elección están en 7.5.

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

**Condición de salida (Bug0 con progreso) [DECISIÓN GRUPO, 10/11/2026]: variante «criterio lateral».** Se abandona el SEGUIMIENTO cuando se cumplen las tres condiciones:

1. `h(pos) < d0` (progreso respecto al inicio del rodeo).
2. El mejor movimiento de acercamiento (el primero en orden N, E, S, O que reduce h) está libre.
3. El objetivo queda en el **lado libre**, el contrario al muro: con mano derecha, `orientacion.izquierda()`; con mano izquierda, `orientacion.derecha()`. Se comprueba con el producto escalar `(objetivo − posición) · lado > 0`.

**[POR VALIDAR]** La diapositiva solo dice «el objetivo cumple el criterio lateral de la variante»; la condición 3 es la interpretación del grupo. Confirmar con el profesor si se explicó de otra forma.

**Justificación.** Se simularon las tres variantes (Bug0 básico, pérdida de contacto, criterio lateral) en los 7 mapas del guion, transcritos de sus imágenes, con **todas** las combinaciones de inicio y objetivo sobre celdas libres (~9.000 por mapa). Ninguna variante choca nunca; los fallos son ciclos. Porcentaje de casos resueltos:

| Mapa | Básico | Pérdida de contacto | Criterio lateral |
|---|---|---|---|
| Campo abierto | 100 % | 100 % | 100 % |
| Muro vertical | 81 % | 86 % | 100 % |
| Muro horizontal | 99 % | 96 % | 100 % |
| Muro diagonal | 64 % | 100 % | 100 % |
| Barrera en U | 86 % | 97 % | 100 % |
| Barrera en arco | 72 % | 92 % | 100 % |
| Barrera en C | 84 % | 96 % | 100 % |

Con el criterio lateral, el recorrido supera a la distancia Manhattan en 0,5–7 pasos de media según el mapa, y en 52 como máximo (muro diagonal). La pérdida de contacto falla porque los límites del mapa también cuentan como muro: por ejemplo, en el muro vertical con inicio (0,2) y objetivo (6,3), el agente rodea el muro, en (5,2) ya podría salir, pero sigue en contacto y recorre el perímetro en bucle.

**[VERIFICADO el 24/11/2026]** El banco original (modelo en Python) se perdió. Se rehízo en Java como `test/grid/BancoMapasGrid.java`, con los 7 mapas transcritos automáticamente de las imágenes de `Pr2.pdf`. La V1 actual (con la detección de ciclos de Martín) resuelve el 100 % de los 59.290 casos, con las mismas medias y el mismo máximo de 52. La cifra antigua de 59.462 casos venía de una transcripción con alguna celda distinta. En DBA Lab, según el grupo, la V1 resuelve los 7 retos de la primera tanda.

**Riesgos y salvaguardas [DECISIÓN GRUPO, implementadas por Martín en el PR #7]:**
- **Ciclos.** Bug0 no garantiza llegar en cualquier mapa. Se guarda cada estado de seguimiento (posición, orientación, mano, `d0`, bumpers). La primera repetición cambia de mano, vacía la memoria y desanda el último paso; la segunda lanza `ParadaNavegacion`. En la V1, el cambio de mano se permite una vez por run.
- **Energía.** Si `mode` es `FINITE` y `remaining <= 0`, la política lanza `ParadaNavegacion` en vez de mover. No se anticipa si la energía restante basta para llegar.
- **Seguridad.** Nunca se envía un movimiento hacia una dirección con bumper `true`; las colisiones terminan el run. En el banco no hay ningún choque con ninguna de las dos políticas.
- `BucleAgenticoGrid` captura `ParadaNavegacion` (de V1 y V2), lo muestra y termina; `Agente.takeDown()` cierra el run.

### 7.3 Clases previstas

| Clase | Responsabilidad | Responsable |
|---|---|---|
| `Posicion` | *Record* `x`, `y`; `distanciaManhattan(otra)` y `mover(direccion)`. **Hecho.** | Rodrigo |
| `Direccion` | Enum `NORTE`, `ESTE`, `SUR`, `OESTE` (en ese orden: sentido horario y desempate del Tema 2) con `dx()`, `dy()` (norte = `dy -1`), `izquierda()`, `derecha()` y `retroceso()`. Desacoplada del protocolo de red; la traducción a códigos REST y campos de bumpers reside en `EntornoGrid`. **Hecho.** | Rodrigo |
| `SensoresGrid` | *Record* `posicion`, `objetivo`, `Set<Direccion> bloqueadas` (bumpers a `true`, copia inmutable), `modoEnergia`, `energiaRestante`; `bloqueado(d)`, `libre(d)`, `distanciaAlObjetivo()`, `enObjetivo()`. **Hecho.** | Rodrigo |
| ~~`AccionGrid`~~ | **[DECISIÓN GRUPO, 10/11]** No se crea: la acción es la propia `Direccion` (un movimiento *es* una dirección). Desde el refactor de Pedro (17/11), `Direccion` ya no conoce su código: lo traduce `EntornoGrid`. | — |
| `EntornoGrid` | Extiende `Entorno<SensoresGrid, Direccion>` sobre DBA Lab. Gestiona la selección del mapa/reto, lee el objetivo una sola vez y consulta posición, bumpers y energía. Ejecuta movimientos cardinales y delega la presentación en el visualizador. **Hecho.** | Pedro |
| `VisualizadorGridCli` | Implementa `Visualizador<SensoresGrid, Direccion>`, mostrando métricas, mapa ASCII autoescalable con memoria de recorrido y selección interactiva de mapas por CLI. **Hecho.** | Pedro |
| `AgenteGrid` | Extiende `base.Agente`, instanciando el entorno, política y bucle para Grid sin argumentos de JADE. **Hecho.** | Pedro |
| `BucleAgenticoGrid` | Extiende `base.BucleAgentico`, gestionando la parada por objetivo alcanzado o fin de run. **Hecho.** | Pedro |
| `PoliticaGrid` (V1) | Implementa `Politica<SensoresGrid, Direccion>` con el algoritmo de 7.2 y la salida por criterio lateral. **Hecho (10/11).** Una instancia por run, porque guarda estado. Incluye la detección de ciclos y la parada por energía de Martín (PR #7). Resuelve todos los casos de los 7 mapas en `BancoMapasGrid`. **Ya no es la política activa** (ver 7.5). | Rodrigo |
| `PoliticaGridV2` | Evolución y optimización de `PoliticaGrid` activa en `AgenteGrid`. Incorpora: (1) navegación en acercamiento guiada por **eje dominante** en vez de orden cardinal fijo; (2) **inercia direccional** en empates diagonales a 45° para eliminar oscilaciones en zigzag; (3) **despegue coherente por lado libre** proyectado al semiplano del objetivo mediante producto escalar; (4) **detección de ciclos con memoria inmutable** (`EstadoSeguimiento`) y recuperación por inversión de mano y desandado; y (5) ciclo de vida reutilizable con `reiniciar()` y métodos de observabilidad. **Hecho.** | Rodrigo / Pedro |
| ~~`DetectorCiclos`~~ / control de energía | Salvaguardas de 7.2. No se creó una clase aparte: están dentro de `PoliticaGrid` y `PoliticaGridV2`. **Hecho (PR #7).** | Martín |
| `SimuladorGrid` | `Entorno` local con los 7 mapas transcritos del guion, para pruebas sin VPN ni runs. **Pendiente.** `BancoMapasGrid` cubre la prueba de las políticas sobre esos mapas, aunque no simula el `Entorno` completo. | Cristina |
| `LanzadorGrid` | Punto de entrada del bloque que arranca JADE (sin interfaz gráfica) e inicia `AgenteGrid`. El reto se elige en un menú por consola o con `DBA_CHALLENGE_ID` en `config.properties` o como variable de entorno. **Hecho.** | Pedro |
| `PoliticaGridPrueba` (`test/grid`) | Casos sintéticos de V1 y V2: avance directo, rodeo, recuperación tras cambiar de mano, parada por ciclo y por energía. **Hecho.** | Martín / Pedro |
| `BancoMapasGrid` (`test/grid`) | Banco de V1 contra V2: los 7 mapas del guion con todas las combinaciones de inicio y objetivo, 300 mapas aleatorios de obstáculos sueltos y 300 de muros largos. Clasifica cada caso (llega, ciclo, bucle, choque, error), mide los pasos de más y tiene un modo `traza` que dibuja un recorrido. **Hecho (24/11).** | Rodrigo |

**Observación de diseño [VERIFICADO, 24/11]:** `BucleAgentico` lee los sensores en cada paso, pero leer no consume energía: solo los movimientos cuestan 1. `EntornoGrid` lee `grid.target` una sola vez al iniciar el run.

### 7.4 Pendiente de verificar antes de cerrar el diseño

- **[VERIFICADO, 24/11]** Leer sensores no consume energía; cada movimiento cuesta 1 (ver 7.1).
- **[POR VALIDAR]** Si los retos se resuelven una vez por grupo o con el token de cada integrante (preguntar al profesor).
- **[POR VALIDAR]** La guía `guia-api-grid.md` es un borrador docente; confirmar si hay versión revisada.

### 7.5 Interfaz Swing de Grid — 24/11/2026

La vista usa `MapaExplorado` y `PanelMapa`: memoria de celdas percibidas, escala adaptable, agente/objetivo superpuestos y muros en rojo oscuro (`#8B2635`). Se comunica mediante `Visualizador`, sin referencias al agente concreto ni al cliente HTTP. El render y la delegación de botones se verifican sin acceso al laboratorio.

### 7.6 Reiniciar y Finalizar sin argumentos de agente — 24/11/2026

**Criterio docente transmitido por el usuario:** evitar inyectar visualizador y sesión al crear un agente JADE. La versión inicial de la interfaz (PR #9) lo hacía; esta revisión elimina ese mecanismo.

- `LanzadorGrid` crea `AgenteGrid` con `null`, sin parámetros de aplicación. No construye vistas ni sesiones.
- `AgenteGrid.setup()` pregunta el tipo de interfaz (Swing o consola); `iniciar()` crea la presentación mediante la fábrica y construye su entorno y política. No usa getArguments ni importa componentes Swing.
- `FabricaPresentacionGrid` encapsula la elección en EDT y la creación concreta de la vista y el controlador. El agente entrega esa presentación a EntornoGrid mediante construcción Java ordinaria. La fábrica crea objetos nuevos; no mantiene un registro global ni entrega argumentos ocultos. Cerrar el selector cancela el agente sin crear run; las propiedades dba.grid.ui permiten elegir explícitamente consola/Swing sin pregunta, y el modo headless usa consola.
- `ControladorGrid` coordina el estado local y las solicitudes de reinicio/salida. Se eliminan `SesionGrid` y `MotorJadeGrid`.
- `BucleAplicacionGrid` conserva el agente entre runs en modo gráfico: cierra el anterior antes de iniciar otro y reinicia la política. La vista y los sensores eliminan sus datos anteriores. En CLI se mantiene un único run.
- Finalizar despierta la espera de órdenes y cancela el selector. El agente cierra su run y la presentación; un hilo externo del lanzador detecta la terminación y cierra JADE. Si el cierre falla, se impide otro inicio y se informa al salir.

**Validación local:** compilación Ant/JDK 25; pruebas del controlador/entorno/bucle con cliente simulado; políticas V1/V2 originales; render Swing. Prueba adicional del lanzador real, agente creado sin argumentos y servidor HTTP en loopback, desde directorio temporal con credencial ficticia: run simulado SUCCEEDED y cierre efectivo del proceso JADE (`ARRANQUE_SIN_ARGUMENTOS_OK`). No se crean runs en DBA Lab ni se usa el token personal.

**Alcance:** rama `codex/interfaz-sin-argumentos` desde main con PR #9 fusionada. Base, Termostato y políticas de navegación no cambian. Pendiente comprobar el flujo completo en una ventana interactiva contra DBA Lab.

### 7.7 Limpieza de la interfaz — 24/11/2026

**Petición del usuario:** simplificar, documentar y dejar código legible conservando el comportamiento. Esta revisión se aplica a nuestra aportación de interfaz e integración; no modifica Base, Termostato ni las políticas de navegación de los compañeros.

- Sustituidas la cola de órdenes y varias banderas atómicas del controlador por un estado enumerado y una solicitud de inicio protegidos con `synchronized`. La espera libera el monitor y Finalizar la despierta.
- Eliminado el adaptador de acciones: los dos botones reciben referencias a métodos con `Runnable`.
- Reunida la espera de resultados de Swing en un único método; las actualizaciones normales siguen siendo asíncronas.
- Separada la construcción visual en cabecera, indicadores, mapa y pie. Descomprimidos métodos e instrucciones para facilitar su lectura.
- Centralizado el cierre gráfico en `terminarEjecucion()`, evitando notificaciones de fin repetidas durante el cierre del bucle y del agente. El modo consola conserva el comportamiento base.
- Divididas las pruebas del controlador en escenarios con nombre y añadida la comprobación de una sola notificación final por run, incluso si se pide cerrar varias veces.

**Verificación local:** compilación limpia Ant con JDK 25; pruebas de controlador, políticas V1/V2, mapa Swing y arranque real de JADE contra un servidor HTTP local. El render del panel usa datos simulados. Estas comprobaciones no requieren VPN ni token personal y no equivalen a validar una sesión gráfica interactiva contra DBA Lab.

Los cambios de esta limpieza quedan locales en `codex/interfaz-sin-argumentos`, sin commit ni push.

### 7.8 Limpieza del resto del proyecto — 24/11/2026

**Alcance autorizado por el usuario:** extender la limpieza a todo el proyecto, incluidos Base, Termostato y las políticas. Se continúa desde los cambios locales de la interfaz, ahora en la rama `codex/limpieza-proyecto`. Esta petición amplía el alcance limitado de 7.7.

**Cambios:**

- Base: una sola preparación y validación HTTP para GET/POST; lectura común de códigos y campos opcionales JSON; conversión de valores sin bloques anidados repetidos. Se conservan endpoints, retardo efectivo de 50 ms, timeouts y ausencia de reintentos.
- Configuración: lectura de obligatorios compartida y una carga por entorno. Eliminados el alias `getUrlLaboratorio`, la consulta arbitraria `getPropiedad` y `buscarRetoPorId` del cliente, sin consumidores en el proyecto. Los accesores opcionales sin uso de `LecturaSensor` se sustituyen por accesos obligatorios directos, conservando conversiones y diagnósticos.
- Ciclo común: `BucleAgentico` ofrece las comprobaciones de actividad predeterminadas. Termostato lo instancia directamente; se elimina `BucleAgenticoTermostato`, que solo duplicaba esas comprobaciones. `Agente` conserva el entorno y entrega el bucle a JADE sin mantener otro campo innecesario.
- Entornos y presentación: eliminados constructores y caminos de creación sin uso. El agente sigue eligiendo y creando su presentación; la vista permanece desacoplada y no se pasa por argumentos JADE.
- Grid: V1 mantiene su papel de referencia; `d0` pasa a llamarse `distanciaImpacto`. V2 reutiliza la dirección primaria ya calculada al iniciar el rodeo y simplifica el empate diagonal sin cambiar su resultado. Se elimina una comprobación privada de orientación que ya garantiza `aplicar()`. No se cambian estrategias, desempates ni reinicios de memoria.
- Consola: separada la memoria percibida del render, simplificada la selección del símbolo y reutilizado el borde ASCII.
- Formato uniforme, comentarios centrados en decisiones, retirada de separadores y documentación redundante. README ajustado a clases y métodos actuales.
- Ant: objetivo `pruebas-locales`, también accesible con `make test`, que ejecuta las pruebas en procesos aislados. El arranque JADE utiliza un directorio temporal con configuración ficticia. Se sustituye el texto de ejemplo sin uso de `build.xml` por este objetivo.

**Verificado localmente:** compilación limpia y seis programas de prueba superados con JDK 25: infraestructura/configuración, Termostato, políticas Grid, controlador, mapa Swing y arranque JADE simulado. Las pruebas nuevas comprueban el protocolo HTTP y el ciclo del termostato, además del rechazo de capacidades incompletas antes de crear un run.

**Comparación antes/después:** 2.000 mapas generados con semilla 24, lados entre 4 y 12 y obstáculos con probabilidad 0,25; ambas políticas, máximo 200 movimientos por recorrido. En total 4.000 recorridos. Se comparó la secuencia completa de movimientos y la terminación (llegada o excepción), con la misma huella SHA-256 antes y después: `d9e7c796bd26ace9459d6a45d7cb3c2b9beaebc7dee5c3cc0166ec38413545b8`. Es una comprobación de conservación de comportamiento en esos casos, no una afirmación de que todos los mapas sean resolubles.

**Limitaciones:** sin ejecuciones nuevas en DBA Lab y sin usar el token personal. No se han revalidado los 18 retos oficiales ni la interacción gráfica real. Se conserva también la diferencia preexistente entre V1 (un cambio de mano por ejecución) y V2 (restablecido entre rodeos). La consola sigue calculando su resumen del recorrido a partir del conjunto de celdas visitadas; ese contador no equivale a contar movimientos cuando hay revisitas. Corregirlo supondría un cambio funcional aparte.

**Publicación:** cambios locales, sin commit ni push. Conviene que el responsable de Base revise su diff antes de integrarlo.

### 7.9 Segunda simplificación — 24/11/2026

**Petición del usuario:** reducir el código y hacerlo más sencillo de seguir.

- Se reúnen las responsabilidades de `BucleAplicacionGrid` y `BucleAgenticoGrid` en una sola clase `BucleAgenticoGrid`, que extiende directamente el bucle común. Se elimina un nivel de herencia. Conserva navegación con V2, parada solicitada, reinicio, bloqueo tras cierre fallido y cierre del agente.
- V1 deja `src/grid` y se conserva en `test/grid/PoliticaGrid.java`. La aplicación solo compila V2; las pruebas siguen comparando ambas versiones. No se cambia el algoritmo de ninguna de las dos.
- Comentarios de Base más breves: cada operación conserva su propósito y sus condiciones relevantes, eliminando descripciones repetidas de parámetros cuyo nombre ya las explica.
- README y pruebas actualizados a la nueva organización. La vista continúa desacoplada del agente.

**Comprobación:** objetivo `clean pruebas-locales` con las seis pruebas locales y JDK 25. No se utilizan VPN ni credenciales reales; no se revalidan aquí los 18 mapas oficiales.

Sigue en `codex/limpieza-proyecto`, sin commit ni push.

### 7.10 Revisión de cumplimiento — 24/11/2026

Contraste del código local con `Pr2 (1).pdf`, las tres guías API del Escritorio y las indicaciones de clase transmitidas por el usuario. Esta revisión no cambia la lógica del programa.

**Comprobado en código:** Java/JADE; descubrimiento de retos y validación de capacidades; decisiones por sensores sin mapa previo; cliente HTTP común; agente creado con argumentos JADE `null`; presentación construida por el propio agente mediante fábrica; vista desacoplada; celdas desconocidas, libres y bloqueadas con colores distintos, agente y objetivo superpuestos; escala según mapa y ventana; reinicio y cierre. El guion pide organización clara y reutilizable, sin imponer una lista concreta de clases Java.

**Incumplimiento confirmado de la guía API:** el estado se comprueba antes de percibir y después de actuar, pero falta detenerse si se vuelve terminal durante las lecturas. Además, Grid continúa con los sensores restantes tras recibir un estado terminal y los sensores iniciales de ambos entornos no actualizan siempre el estado del entorno.

Reproducción local con cliente ficticio, sin red ni credenciales: devolver EXPIRED al consultar `grid.position` provoca después dos lecturas (`grid.bumpers`, `grid.energy`) y un intento de acción. Devolver EXPIRED en `thermostat.temperature` provoca un intento de acción. La guía común exige detener percepciones y acciones al recibir estado terminal. Las seis pruebas previas no cubrían este caso. Prioridad: comprobar y propagar control tras cada respuesta y parar antes de decidir/actuar.

**Documentación y decisiones por revisar:** el texto histórico de Base describe `BucleAgentico` como abstracto; la limpieza lo hizo concreto para reutilizar su implementación en Termostato. No se ha encontrado esa exigencia de clases concretas/abstractas en el guion: distinguir una eventual corrección oral del profesor de la descripción histórica del equipo. V2 conserva el cambio de mano por rodeo, diferente de la salvaguarda inicial de un cambio por ejecución. La consola conserva el contador de celdas únicas etiquetado como pasos.

**Pendiente de evidencia:** éxito oficial de los 18 retos con esta versión local, prueba interactiva completa de la GUI y presentación/vídeo de cinco minutos. No se deduce su cumplimiento de la compilación, de mapas generados ni de resultados antiguos. Los plazos históricos del guion deben leerse junto con el aplazamiento de una semana comunicado después por el usuario.

### 7.11 Resultados aportados por el usuario — 24/11/2026

Evidencia: fragmentos de la salida de NetBeans pegados por el usuario. No son ejecuciones nuevas iniciadas por el asistente. Los fragmentos no incluyen runId ni identificador de commit; no permiten atribuir el resultado a una versión exacta.

| Reto | Resultado visible | Observaciones |
| --- | --- | --- |
| 13 | ACTIVE, sin resultado final aportado | Movimiento NORTE confirmado entre (30,47) y (30,46); distancia 57 → 56, energía percibida 158 → 157. La diferencia no aísla el coste de lecturas y movimiento. |
| 14 | SUCCEEDED, TARGET_REACHED | Última percepción en (3,14), objetivo (3,15), energía 629 y ESTE bloqueado. Última acción SUR, APPLIED. La salida muestra 375 celdas descubiertas y 143 «pasos». Terminación del agente y cierre de JADE sin error visible; Ant informa BUILD SUCCESSFUL, duración total aproximada 1 min 10 s. |

La energía 629 corresponde a la percepción anterior al último movimiento; no es una lectura del saldo final. El valor 143 del resumen se obtiene en el código actual de `rastro.size()`: cuenta posiciones distintas registradas, no movimientos aplicados cuando hay revisitas. No presentarlo como longitud exacta del recorrido. El fragmento del reto 14 acredita ese éxito; no acredita el final del reto 13 ni la resolución de los 18 retos.

### 7.12 Corrección del recorrido en consola — 24/11/2026

Petición del usuario tras revisar el resultado del reto 14: corregir la longitud del recorrido, que se calculaba con el tamaño del conjunto de posiciones percibidas.

`VisualizadorGridCli` dispone ahora de un contador independiente `movimientosAplicados`: se pone a cero al iniciar el run y aumenta una vez por respuesta de acción APPLIED, incluidas revisitas y la última acción que alcanza el objetivo. Lecturas repetidas, rechazos y mostrar el resumen no lo incrementan. `rastro` se mantiene para el dibujo. No se modifica la navegación ni se realizan ejecuciones en DBA Lab.

Prueba nueva `test/grid/VisualizadorGridCliPrueba.java`, incluida en `ant pruebas-locales`: recorrido de cuatro movimientos aplicados, una revisita, un rechazo y llegada sin lectura posterior; solo dos posiciones distintas aparecen en las percepciones. El resumen debe indicar cuatro pasos, seguir indicando cuatro al repetirlo y volver a cero en el siguiente run. También comprueba que las percepciones repetidas no sumen pasos. Verificación: compilación limpia y las siete pruebas locales superadas con JDK 25.

Esta corrección sustituye para futuras ejecuciones la limitación del contador indicada en 7.8, 7.10 y 7.11. Los 143 «pasos» del fragmento anterior del reto 14 siguen siendo un dato producido por la versión antigua: el total real solo puede recuperarse contando los APPLIED de su traza completa. El SUCCEEDED de ese reto sigue siendo válido. Permanece pendiente la corrección independiente de las peticiones posteriores a un estado terminal descrita en 7.10.

Cambios locales en `codex/limpieza-proyecto`, sin commit ni push.

### 7.13 Validación real del contador aportada por el usuario — 24/11/2026

Nueva salida de NetBeans pegada por el usuario, con cierre a las 22:54:14: reto 14, resultado SUCCEEDED y motivo TARGET_REACHED. Última acción SUR, APPLIED, desde la percepción (3,14) hacia el objetivo (3,15). Resumen: 375 celdas descubiertas y 162 pasos con el contador de movimientos aplicados corregido. Última energía percibida antes de la acción final: 629. El agente y JADE finalizan sin errores visibles; BUILD SUCCESSFUL, tiempo total aproximado 1 min 11 s.

La nueva salida confirma el uso del resumen corregido en una ejecución real satisfactoria. La navegación no se modificó al corregir el contador. La diferencia 143 → 162 no debe interpretarse como un empeoramiento del algoritmo ni como exactamente 19 revisitas: las métricas anteriores eran distintas. El fragmento no contiene runId ni la traza completa de acciones. No acredita los demás retos ni el saldo de energía posterior al último movimiento.

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

1. **Lee primero** el enunciado (`Pr2.pdf`), las tres guías docentes y la versión actual del repositorio, incluidos los cambios locales/ramas si son accesibles.
2. Pregunta o comprueba qué parte asume el integrante (termostato, Grid primera tanda, Grid exploración, Base/integración); **no sobrescribas código de otros** ni introduzcas cambios innecesarios.
3. Distingue con etiquetas o lenguaje explícito: **[DOCENTE]**, **[DECISIÓN GRUPO]**, **[VERIFICADO]**, **[POR VALIDAR]**. No atribuyas requisitos al profesor si el documento no los contiene.
4. Da instrucciones **paso a paso**, con un cambio acotado y una comprobación clara; no avances diciendo que algo funciona hasta ver el resultado.
5. Usa los códigos y campos exactos de la API. No inventes JSON, nombres de endpoints, identificadores de retos o tipos de sensores. Las lecturas no mueven el entorno; una acción sí puede cambiarlo o terminar el run.
6. **Nunca solicites que el usuario pegue su token**. Si hacen falta pruebas, prepara mecanismos locales que no muestren credenciales y evita incluirlas en logs o documentación.
7. En asuntos de JADE, evita repetir soluciones descartadas de cierre. Inspecciona la versión de JADE y el código real antes de proponer métodos o constantes.
8. Recuerda que **Termostato ya tiene ejecuciones `SUCCEEDED` (368, 382 y 1686)**; prioriza revisión/integración/defensa antes de generar ejecuciones idénticas sin propósito.
9. En Grid, diseña la navegación considerando percepción parcial, obstáculos terminales y energía limitada. El algoritmo acordado es el seguimiento de muros de la sección 7.2 (indicación del profesor), y la política entregada es `PoliticaGridV2` (7.5); no propongas sustituirlo por A* u otro planificador sin que el grupo lo decida. Antes de gastar runs en DBA Lab, prueba cualquier cambio de política con `test/grid/BancoMapasGrid.java`.
10. Documenta qué se cambió, por qué, qué resultado produjo y qué queda por revisar para el resto del grupo.

---

**Resumen de estado al 24/11/2026:** Termostato cerrado y resuelto (runs 368, 382 y 1686). Grid completo: `base` generalizado y todos los componentes de Grid en `main`. La política activa es `PoliticaGridV2`, que resuelve los 19 retos (7 de la primera tanda y 11 de la segunda) tras subir el profesor la energía del reto 9 a 220. La V1 (`PoliticaGrid`) se conserva como referencia: resuelve 17, y falla el 13 y el 18 por energía. Verificado que cada movimiento cuesta 1 de energía y leer sensores no cuesta nada. Banco `BancoMapasGrid` en `test/grid` (rama `docs-resultados-grid`, pendiente de PR). Pendiente: registrar en el README los números de run de los retos que resolvieron otros compañeros, `SimuladorGrid` (opcional), vídeo (en curso) y entrega del código el **25/11/2026**.
