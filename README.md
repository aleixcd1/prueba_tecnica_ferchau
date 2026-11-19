# El tiempo en España

Aplicación para buscar municipios españoles y consultar la previsión de temperatura media y probabilidad de precipitación del día siguiente mediante AEMET OpenData. Es una implementación MVP de la prueba técnica entregada.

## Requisitos

- Docker Desktop o Docker Engine con Docker Compose.
- Una clave personal de [AEMET OpenData](https://opendata.aemet.es/centrodedescargas/inicio).
- Para ejecutar y probar el código sin contenedores: Java 25 (LTS) y Maven 3.9+, Node.js 24.15+ y npm.

## Puesta en marcha con Docker

1. Solicita una clave de AEMET y crea el archivo de entorno:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Edita `.env` y sustituye el valor de `AEMET_API_KEY`. No guardes ni publiques tu clave.
3. Enciende Docker Desktop y desde la raíz del proyecto ejecuta:

   ```powershell
   docker compose up --build
   ```

4. Abre <http://localhost:4200>. La API también está disponible en <http://localhost:8080>; su documentación interactiva Swagger está en <http://localhost:8080/swagger-ui.html>.
5. Para detener los contenedores, pulsa `Ctrl+C` y ejecuta `docker compose down`.

Si no se proporciona la clave, los contenedores arrancan, pero las operaciones que consultan AEMET responden con un error explícito.

## API

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/municipalities?query=Madrid` | Busca municipios sin distinguir mayúsculas ni tildes. |
| `GET` | `/api/v1/forecast/{municipalityId}?unit=G_CEL` | Predicción del día siguiente. Unidades: `G_CEL` o `G_FAH`; Celsius es el valor predeterminado. |
| `GET` | `/actuator/health` | Estado del backend. |
| `GET` | `/v3/api-docs` | Especificación OpenAPI. |
| `GET` | `/swagger-ui.html` | Interfaz Swagger UI. |

Ejemplo de predicción:

```json
{
  "mediaTemperatura": 20.0,
  "unidadTemperatura": "G_CEL",
  "probPrecipitacion": [
    { "probabilidad": 5, "periodo": "00-06" },
    { "probabilidad": 10, "periodo": "06-12" },
    { "probabilidad": 10, "periodo": "12-18" },
    { "probabilidad": 5, "periodo": "18-24" }
  ]
}
```

La probabilidad es `null` cuando AEMET no proporciona datos para una franja. La temperatura se calcula promediando los valores disponibles de ese día y se redondea a una cifra decimal al presentarla.

## Diseño

### Backend

Spring Boot 4.1.2 y Java 25 (LTS), organizados en capas hexagonales:

- `domain`: municipio, unidades, predicción diaria y puertos de salida.
- `application`: búsqueda de municipios y caso de uso de predicción para mañana.
- `infrastructure`: API REST/OpenAPI, cliente AEMET, caché y configuración.

El adaptador AEMET solicita la URL de datos de cada servicio y después recupera el documento indicado por esa URL. Se valida que el segundo enlace pertenezca al mismo host AEMET para evitar seguir URLs externas inesperadas. No se incorpora OAuth: esta API es pública y de solo lectura y el MVP no gestiona cuentas ni datos privados de usuario.

La caché Caffeine en memoria:

- Municipios: 12 horas, una sola entrada y carga sincronizada para evitar consultas paralelas en frío.
- Predicciones: 60 minutos por municipio y fecha, con un máximo de 10.000 entradas.

Esto reduce notablemente las solicitudes repetidas y amortigua picos de uso. La caché es local al proceso: para varias réplicas se debe añadir una caché compartida (por ejemplo Redis) y límites de tasa por cliente; una política de caducidad breve/stale-while-revalidate mantiene actualizados los pronósticos sin golpear AEMET en cada visita.

### Frontend

Angular 22 con componentes standalone, rutas con carga diferida, formularios accesibles, servicios HTTP tipados y una página de funcionalidad aislada. El autocompletado aplica debounce y cancela búsquedas obsoletas; la selección de municipio y cada cambio de unidad vuelven a consultar la predicción. El frontend no almacena datos de ubicación ni requiere autenticación.

### Contenedores

`docker-compose.yml` levanta dos imágenes separadas: backend Java y frontend estático Nginx. Nginx sirve la aplicación Angular y reenvía `/api` al backend dentro de la red de Compose. La clave AEMET se inyecta como variable de entorno y no se incluye en ninguna imagen.

## Pruebas y desarrollo local

Backend (Java 25 y Maven 3.9+):

```powershell
cd backend
mvn verify
```

Frontend (Node.js 24.15+):

```powershell
cd frontend
npm ci
npm test -- --watch=false
npm run build
npx playwright install chromium
npm run e2e
```

Para desarrollar con el servidor Angular y el proxy local a `http://localhost:8080`, ejecuta `npm start` desde `frontend`. El backend puede iniciarse con `AEMET_API_KEY=... mvn spring-boot:run` (en PowerShell: `$env:AEMET_API_KEY="..."; mvn spring-boot:run`).

## Respuestas a las preguntas de la prueba

1. **Listado lento de municipios:** mantenerlo en caché, refrescarlo con una caducidad razonable y cargarlo fuera del camino crítico de cada búsqueda. En esta versión se conserva 12 horas en memoria; los filtros se hacen en el backend sobre la copia ya descargada.
2. **URL intermedia de predicción:** permite separar la solicitud de metadatos de la descarga del conjunto de datos, facilitando a AEMET distribuir/almacenar el resultado y responder a consultas mientras prepara o localiza el contenido.
3. **Recordar el último municipio:** guardar el identificador y nombre elegidos en `localStorage` y restaurarlos al abrir la aplicación; validar el contenido y ofrecer al usuario una acción para borrarlo. No es necesario solicitar geolocalización ni crear cuentas.
4. **Reducir carga en AEMET:** cachear respuestas en el backend, coalescer solicitudes concurrentes, aplicar límites de tasa, servir datos recientes durante una actualización en segundo plano y monitorizar errores/cuotas. Esta primera versión implementa caché en memoria (12 h para municipios y 60 min para cada pronóstico); al escalar horizontalmente, conviene compartirla mediante Redis y conservar un cortacircuitos para fallos de AEMET.

## Integración continua y publicación

La integración continua valida los cambios de la rama principal y las solicitudes de incorporación: ejecuta las pruebas del backend, las pruebas unitarias y e2e del frontend, y comprueba la construcción de ambas imágenes Docker. Al publicar una versión con una etiqueta `v*`, se generan y publican las imágenes del backend y del frontend en GitHub Container Registry con la etiqueta de versión y `latest`.
