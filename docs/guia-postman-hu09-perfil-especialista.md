# Guia rapida Postman - HU09-BE Perfil de Especialista

Esta guia te permite probar los nuevos endpoints del perfil de especialista de forma simple.

## Base URL

- Local: `http://localhost:8080`
- Base path del proyecto: `/api/v1`

Endpoint final:
- GET perfil: `GET /api/v1/specialists/{id}/profile`
- PUT actualizar perfil: `PUT /api/v1/specialists/{id}/profile`

## 1) Obtener perfil del especialista

### Request

- Metodo: `GET`
- URL ejemplo: `http://localhost:8080/api/v1/specialists/12/profile`
- Headers:
  - `Accept: application/json`

### Response 200 (OK)

```json
{
  "id": 12,
  "fullName": "Ana Maria Quispe Rojas",
  "email": "ana.quispe@takecare.com",
  "biography": "Especialista en terapia cognitivo conductual."
}
```

### Response 404 (Not Found)

Cuando no existe el especialista con ese `id`.

## 2) Actualizar perfil del especialista

### Request

- Metodo: `PUT`
- URL ejemplo: `http://localhost:8080/api/v1/specialists/12/profile`
- Headers:
  - `Content-Type: application/json`
  - `Accept: application/json`

### Body (raw JSON)

```json
{
  "fullName": "Ana Maria Quispe Rojas",
  "email": "ana.quispe@takecare.com",
  "biography": "Especialista en terapia cognitivo conductual con experiencia en adolescentes."
}
```

### Validaciones importantes

- `fullName`: obligatorio.
- `email`: obligatorio y formato valido.
- `biography`: opcional, maximo 500 caracteres.

### Response 200 (OK)

```json
{
  "id": 12,
  "fullName": "Ana Maria Quispe Rojas",
  "email": "ana.quispe@takecare.com",
  "biography": "Especialista en terapia cognitivo conductual con experiencia en adolescentes."
}
```

### Response 400 (Bad Request)

Cuando el body no cumple validaciones (por ejemplo, email invalido o fullName vacio).

Ejemplo de body invalido:

```json
{
  "fullName": "",
  "email": "correo-invalido",
  "biography": "Texto"
}
```

### Response 404 (Not Found)

Cuando no existe el especialista con ese `id`.

## Recomendacion Postman

Puedes crear una variable de entorno:

- `{{baseUrl}} = http://localhost:8080`

Y usar URLs asi:

- `{{baseUrl}}/api/v1/specialists/12/profile`
