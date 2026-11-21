# TicketYa - Listado de Endpoints (Entrega 2)

Todas las rutas `/api/**` se llaman a través del **API Gateway** (`http://localhost:8080`).
La documentación OpenAPI completa está en **http://localhost:8080/swagger-ui.html**, donde se elige `catalogo-service` o
`ventas-service` arriba a la derecha. También está en cada servicio: `http://localhost:8081/swagger-ui.html` y
`http://localhost:8082/swagger-ui.html`. El JSON OpenAPI está en `/v3/api-docs`.

## Autenticación y autorización

Las rutas protegidas requieren un JWT en el encabezado:

```http
Authorization: Bearer <token>
```

Obtener un token con `POST /api/auth/login`:

```json
{ "username": "cliente-1", "password": "cliente123" }
```

La respuesta contiene `token`, `usuarioId`, `username`, `rol` y `expiresInMinutes`.
El endpoint `POST /api/auth/register` registra usuarios `CLIENTE` u `ORGANIZADOR`;
no se permite registrarse como `ADMIN` ni `PERSONAL_ACCESO`.

| Rol | Permisos principales |
|---|---|
| `CLIENTE` | Reservas, compras y solicitud de devolución |
| `ORGANIZADOR` | Crear lugares y eventos, publicar sus eventos y consultar reportes propios |
| `ADMIN` | Gestión administrativa, resolver devoluciones y consultar reportes |
| `PERSONAL_ACCESO` | Validar códigos QR |

Las consultas `GET /api/eventos/**` y `GET /api/lugares/**` son públicas.
Las rutas protegidas responden `401` si falta el token o es inválido/vencido, y `403`
si el usuario autenticado no tiene el rol o la propiedad requerida. La identidad se toma
del token: no enviar `X-User-Id` ni `X-User-Role`.

Usuarios semilla para desarrollo:

| Usuario | Contraseña | Rol |
|---|---|---|
| `cliente-1` | `cliente123` | `CLIENTE` |
| `org-1` | `organizador123` | `ORGANIZADOR` |
| `admin-1` | `admin123` | `ADMIN` |
| `acceso-1` | `acceso123` | `PERSONAL_ACCESO` |

El Gateway y ambos servicios deben compartir el mismo `JWT_SECRET`. Catálogo y ventas
deben compartir `INTERNAL_SERVICE_SECRET` para la comunicación interna.

Los errores se devuelven en formato **ProblemDetail** (RFC 9457):

```json
{ "title": "Conflict", "status": 409, "detail": "El lugar elegido acaba de ser reservado por otro cliente", "instance": "/api/reservas" }
```

---

## Autenticación (API Gateway, puerto 8080)

| Método | Ruta | Acceso | Body | Respuestas |
|---|---|---|---|---|
| POST | `/api/auth/login` | Público | `username`, `password` | 200, 401 |
| POST | `/api/auth/register` | Público | `username`, `password`, `role` (`CLIENTE` u `ORGANIZADOR`) | 201, 400, 409 |

La colección Postman permite iniciar sesión con cualquiera de los usuarios semilla y
guarda el token en la variable `jwt`.

---

## catalogo-service (puerto 8081)

| Método | Ruta | Parámetros | Respuestas |
|---|---|---|---|
| POST | `/api/lugares` | `ADMIN` u `ORGANIZADOR`; body `LugarDTO` | 201, 401, 403 |
| GET | `/api/lugares` | Público | 200 |
| GET | `/api/lugares/{id}` | Público; path `id` | 200, 404 |
| POST | `/api/eventos` | `ORGANIZADOR`; body `EventoCreateDTO` | 201, 401, 403, 404 |
| PUT | `/api/eventos/{id}/publicar` | `ORGANIZADOR` dueño; path `id` | 200, 401, 403, 404, 409 |
| PUT | `/api/eventos/{id}/suspender` | `ADMIN`; path `id` | 200, 401, 403, 404, 409 |
| PUT | `/api/eventos/{id}/reactivar` | `ADMIN`; path `id` | 200, 401, 403, 404, 409 |
| PUT | `/api/eventos/{id}/cancelar` | `ADMIN` u `ORGANIZADOR` dueño; path `id` | 200, 401, 403, 404, 409, 503 |
| GET | `/api/eventos` | Público; filtros `q`, `tipo`, `organizadorId`, `participante`, `ciudad`, fechas, orden y paginación | 200 |
| GET | `/api/eventos/{id}` | Público; path `id` | 200, 404 |

### Crear lugar - `POST /api/lugares`

Request:
```json
{
  "nombre": "Teatro de Prueba",
  "direccion": "Calle 123",
  "ciudad": "Tandil",
  "latitud": -37.32,
  "longitud": -59.13,
  "sectores": [
    { "nombre": "Platea", "capacidad": 100, "esNumerado": true, "filas": 10, "asientosPorFila": 10 }
  ]
}
```
Response `201`:
```json
{
  "id": 1, "nombre": "Teatro de Prueba", "direccion": "Calle 123", "ciudad": "Tandil",
  "latitud": -37.32, "longitud": -59.13,
  "sectores": [ { "id": 1, "nombre": "Platea", "capacidad": 100, "esNumerado": true, "filas": 10, "asientosPorFila": 10 } ]
}
```

### Crear evento - `POST /api/eventos`

Request: `Authorization: Bearer <token de ORGANIZADOR>`
```json
{
  "nombre": "Recital de Prueba",
  "descripcion": "Evento para probar la API",
  "tipo": "RECITAL",
  "participantes": ["Banda A"],
  "fechaHora": "2026-12-15T20:00:00",
  "lugarId": 1,
  "precios": [ { "sectorId": 1, "precio": 15000 } ]
}
```
Response `201`:
```json
{
  "id": 1, "nombre": "Recital de Prueba", "descripcion": "Evento para probar la API", "tipo": "RECITAL",
  "organizadorId": "org-1", "participantes": ["Banda A"], "fechaHora": "2026-12-15T20:00:00",
  "lugar": { "id": 1, "nombre": "Teatro de Prueba", "ciudad": "Tandil", "...": "..." },
  "estado": "BORRADOR", "precioDesde": 15000,
  "preciosSectores": [ { "id": 1, "sector": { "id": 1, "nombre": "Platea", "...": "..." }, "precio": 15000 } ]
}
```
Error `404`: `{ "title": "Not Found", "status": 404, "detail": "Lugar no encontrado con ID: 99" }`

### Publicar, suspender, reactivar y cancelar

No llevan body. Responden `200` con el evento y su nuevo `estado` (`PUBLICADO`, `SUSPENDIDO`, `PUBLICADO` o `CANCELADO`).
Ejemplo de error `409` al suspender un borrador:
```json
{ "title": "Conflict", "status": 409, "detail": "Solo se puede suspender un evento publicado o agotado" }
```

### Buscar eventos - `GET /api/eventos`

Ejemplos:
- `/api/eventos?ciudad=Tandil&tipo=RECITAL&sort=fechaHora,asc`
- `/api/eventos?ciudad=Tandil&sort=precioDesde,asc`
- `/api/eventos?q=piojos&orden=relevancia`

Response `200` (página de Spring Data):
```json
{
  "content": [ { "id": 1, "nombre": "Recital de Prueba", "estado": "PUBLICADO", "precioDesde": 15000, "...": "..." } ],
  "number": 0, "size": 10, "totalElements": 1, "totalPages": 1, "first": true, "last": true
}
```

---

## ventas-service (puerto 8082)

| Método | Ruta | Parámetros | Respuestas |
|---|---|---|---|
| POST | `/api/reservas` | `CLIENTE`; body `ReservaCreateDTO` | 201, 400, 401, 403, 409 |
| GET | `/api/reservas` | `CLIENTE`; lista sus reservas | 200, 401, 403 |
| POST | `/api/compras/confirmar` | `CLIENTE`; body `ConfirmarCompraDTO` | 200, 401, 402, 403, 404, 409 |
| GET | `/api/compras` | `CLIENTE`; lista sus compras | 200, 401, 403 |
| POST | `/api/devoluciones/entradas/{entradaId}` | `CLIENTE`; body `SolicitudDevolucionDTO` | 201, 401, 403, 404, 409 |
| PUT | `/api/devoluciones/{devolucionId}/resolver` | `ADMIN`; body `ResolverDevolucionDTO` | 200, 400, 401, 403, 404, 409 |
| POST | `/api/accesos/validar-qr` | `PERSONAL_ACCESO` o `ADMIN` | 200, 400, 401, 403, 404, 409 |
| GET | `/api/reportes/eventos/{eventoId}` | `ORGANIZADOR` dueño o `ADMIN` | 200, 401, 403, 404 |
| POST | `/internal/eventos/{eventoId}/cancelacion` | Interno; requiere `X-Internal-Token`; no pasa por Gateway | 200, 401 |

### Crear reserva - `POST /api/reservas`

Request: `Authorization: Bearer <token de CLIENTE>`
```json
{
  "eventoId": 1,
  "sectorId": 1,
  "esNumerado": true,
  "asientos": [ { "fila": 2, "asiento": 3, "precio": 15000 } ]
}
```
Para un sector no numerado se manda `"esNumerado": false`, `"capacidadMaximaSector": 500` y un elemento en `asientos`
por cada entrada, sin fila ni asiento.

Response `201`:
```json
{
  "id": 1, "clienteId": "cliente-1", "eventoId": 1,
  "fechaCreacion": "2026-11-09T22:19:32", "fechaExpiracion": "2026-11-09T22:29:32",
  "estado": "PENDIENTE", "fechaEvento": "2026-12-15T20:00:00",
  "entradas": [ { "id": 1, "eventoId": 1, "sectorId": 1, "fila": 2, "asiento": 3, "precio": 15000, "codigoQR": null, "estado": "RESERVADA" } ]
}
```
Error `409` si otro cliente ya tomó el asiento:
```json
{ "title": "Conflict", "status": 409, "detail": "El lugar elegido acaba de ser reservado por otro cliente", "instance": "/api/reservas" }
```

### Confirmar compra - `POST /api/compras/confirmar`

Request (`"medioPago": "RECHAZAR"` simula un pago rechazado):
```json
{ "reservaId": 1, "medioPago": "TARJETA_CREDITO" }
```
Response `200`:
```json
{
  "id": 1, "reservaId": 1, "clienteId": "cliente-1", "eventoId": 1, "montoTotal": 15000.00,
  "fechaCompra": "2026-11-09T22:19:32", "medioPago": "TARJETA_CREDITO", "transaccionExternaId": "PAY-6733B28D",
  "entradas": [ { "id": 1, "fila": 2, "asiento": 3, "precio": 15000.00, "codigoQR": "QR-b8b2ecf3-8f74-4c1b-be6b-363be6f4ce6c", "estado": "VENDIDA" } ]
}
```
Error `402`: `{ "title": "Payment Required", "status": 402, "detail": "Pago rechazado por la pasarela. Se liberaron los asientos." }`

### Solicitar devolución - `POST /api/devoluciones/entradas/{entradaId}`

Request:
```json
{ "motivo": "No puedo asistir" }
```
Response `201`:
```json
{
  "id": 1, "clienteId": "cliente-1", "motivo": "No puedo asistir",
  "fechaSolicitud": "2026-11-09T22:19:32", "fechaResolucion": null, "transaccionReintegroId": null,
  "estado": "SOLICITADA", "entrada": { "id": 1, "estado": "VENDIDA", "...": "..." }
}
```

### Resolver devolución - `PUT /api/devoluciones/{devolucionId}/resolver`

Request: `Authorization: Bearer <token de ADMIN>`
```json
{ "aprobar": true }
```
Response `200`: la devolución con `"estado": "APROBADA"`, `"transaccionReintegroId": "REF-F0A225C8"` y la entrada en `DEVUELTA`.

### Cancelación interna - `POST /internal/eventos/{eventoId}/cancelacion`

La llama `catalogo-service` al cancelar un evento con `X-Internal-Token`, cuyo valor
proviene de `INTERNAL_SERVICE_SECRET`. No incluir esta credencial en colecciones públicas.
Response `200`:
```json
{ "eventoId": 1, "reservasCanceladas": 1, "entradasReintegradas": 3, "clientesNotificados": 2 }
```
