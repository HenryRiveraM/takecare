# HU19 - Contrato de integracion para historial de citas

La interfaz administrativa consume el siguiente endpoint para mostrar el historial:

```http
GET /api/v1/admin/sessions
X-Admin-Id: {idDelAdministrador}
```

## Respuesta esperada

El endpoint debe retornar `200 OK` con un arreglo JSON. Los nombres de los campos coinciden con `SessionResponseDTO`, agregando correos de forma opcional para mejorar la busqueda.

```json
[
  {
    "id": 18,
    "patientId": 4,
    "specialistId": 9,
    "scheduleId": 32,
    "patientName": "Ana Perez",
    "specialistName": "Dra. Sofia Lima",
    "patientEmail": "ana@email.com",
    "specialistEmail": "sofia@email.com",
    "status": 4,
    "typeOfSession": 1,
    "createdDate": "2026-05-20T10:15:00",
    "scheduleDate": "2026-05-22",
    "startTime": "14:00:00",
    "endTime": "15:00:00"
  }
]
```

## Catalogos utilizados por frontend

| Campo | Valor | Significado |
| --- | --- | --- |
| `status` | `1` | Pendiente |
| `status` | `2` | Aceptada |
| `status` | `3` | Rechazada |
| `status` | `4` | Finalizada |
| `status` | `5` | Cancelada |
| `typeOfSession` | `1` | Virtual |
| `typeOfSession` | `2` | Presencial |

## Comportamiento del frontend

- Envia el encabezado `X-Admin-Id` del usuario guardado en `localStorage`, igual que las consultas actuales del panel.
- Busca por nombre, correo o identificador de cita.
- Filtra por estado y por rango de `scheduleDate`; si no existe, usa la fecha de `createdDate`.
- Espera un `403` cuando el usuario no tenga rol administrador.

No se requieren operaciones de edicion o eliminacion de citas para esta historia de usuario.
