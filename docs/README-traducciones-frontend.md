# Guia de traducciones del frontend

Este documento explica como funciona la traduccion de idiomas en el frontend de **Take Care** y como agregar nuevos textos sin romper el proyecto.

La aplicacion usa **Angular + ngx-translate** para cambiar entre espanol (`es`) e ingles (`en`).

## 1. Archivos principales de traduccion

Estos son los archivos que debes conocer:

- [frontend/src/app/app.config.ts](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/app/app.config.ts)
- [frontend/src/app/services/language.service.ts](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/app/services/language.service.ts)
- [frontend/src/assets/i18n/es.json](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/assets/i18n/es.json)
- [frontend/src/assets/i18n/en.json](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/assets/i18n/en.json)

### Que hace cada uno

`app.config.ts`

- configura `ngx-translate`
- define que el idioma por defecto es `es`
- define que los archivos de idioma se cargan desde `./assets/i18n/`

`language.service.ts`

- cambia el idioma activo
- guarda el idioma en `localStorage`
- recupera el idioma guardado cuando el usuario vuelve a entrar

`es.json` y `en.json`

- contienen todos los textos visibles de la interfaz
- deben mantener la misma estructura de claves

## 2. Como esta configurado ngx-translate

En [frontend/src/app/app.config.ts](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/app/app.config.ts) se configura asi:

```ts
provideTranslateService({
  lang: 'es',
  fallbackLang: 'es',
  loader: provideTranslateHttpLoader({
    prefix: './assets/i18n/',
    suffix: '.json'
  })
})
```

Esto significa:

- el idioma inicial es espanol
- si falta una traduccion, se intenta usar espanol
- los textos salen de `frontend/src/assets/i18n/es.json` y `frontend/src/assets/i18n/en.json`

## 3. Como cambiar el idioma

El proyecto tiene un servicio central:

[frontend/src/app/services/language.service.ts](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/app/services/language.service.ts)

```ts
setLanguage(lang: 'es' | 'en') {
  this.translate.use(lang);
  localStorage.setItem('lang', lang);
}
```

### Flujo actual

1. El usuario selecciona un idioma.
2. `LanguageService` cambia el idioma con `translate.use(...)`.
3. El idioma se guarda en `localStorage`.
4. Cuando la app vuelve a cargar, se reutiliza ese idioma.

## 4. Como traducir texto en HTML

En componentes standalone, normalmente se importa `TranslatePipe`.

Ejemplo real:

[frontend/src/app/shared/navbar/navbar.component.ts](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/app/shared/navbar/navbar.component.ts)

```ts
imports: [CommonModule, RouterModule, TranslatePipe]
```

Luego en el HTML se usa asi:

```html
<span>{{ 'navbar.home' | translate }}</span>
```

### Recomendacion

Usa claves organizadas por pantalla o componente, por ejemplo:

- `login.title`
- `login.submit`
- `patientSearch.searchButton`
- `admin.actions.suspend`

## 5. Como traducir texto en TypeScript

Cuando necesitas mostrar mensajes desde el `.ts`, usa `TranslateService`.

Ejemplo real:

[frontend/src/app/pages/login/login.component.ts](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/app/pages/login/login.component.ts)

```ts
constructor(
  private translate: TranslateService
) {}
```

Y luego:

```ts
this.errorMsg = this.translate.instant('login.errors.completeFields');
```

### Cuando usar `TranslatePipe`

- cuando el texto se muestra directamente en el HTML

### Cuando usar `TranslateService`

- cuando el texto se arma en TypeScript
- cuando se muestra en alerts, mensajes de error o logs visuales
- cuando necesitas traducir dinamicamente segun una condicion

## 6. Como agregar un nuevo texto traducible

Supongamos que quieres agregar un boton nuevo en login llamado "Volver".

### Paso 1. Agrega la clave en espanol

En [frontend/src/assets/i18n/es.json](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/assets/i18n/es.json):

```json
"login": {
  "back": "Volver"
}
```

### Paso 2. Agrega la misma clave en ingles

En [frontend/src/assets/i18n/en.json](/Users/henryriveramendez/Documents/taller/takecare/frontend/src/assets/i18n/en.json):

```json
"login": {
  "back": "Back"
}
```

### Paso 3. Usa la clave en el HTML

```html
<button>{{ 'login.back' | translate }}</button>
```

## 7. Regla importante: mantener la misma estructura en ambos JSON

Si agregas una clave en `es.json`, debes agregarla tambien en `en.json`.

Ejemplo correcto:

```json
// es.json
"patientSearch": {
  "searchButton": "Buscar"
}
```

```json
// en.json
"patientSearch": {
  "searchButton": "Search"
}
```

Si una clave existe solo en un idioma, la interfaz puede mostrar el nombre de la clave o dejar partes sin traducir.

## 8. Buenas practicas que debe seguir el equipo

- No escribir textos fijos directamente en el HTML si deben cambiar por idioma.
- No mezclar espanol e ingles en los templates.
- Agrupar claves por modulo o pantalla.
- Mantener nombres de claves claros y consistentes.
- Si usas un texto en TypeScript, traducelo con `TranslateService`.
- Revisar siempre ambos archivos: `es.json` y `en.json`.

## 9. Que cosas si se traducen y que cosas no

### Si se traducen

- botones
- labels
- placeholders
- titulos
- mensajes de error
- textos de ayuda
- nombres fijos de filtros o estados

### No se traducen automaticamente

- biografias escritas por usuarios
- descripciones guardadas en base de datos
- direcciones reales
- texto libre creado por especialistas o pacientes

Esto es importante porque **no todo lo que se ve en pantalla viene del frontend**. Si un texto viene del backend como contenido libre, no debe inventarse una traduccion desde Angular.

## 10. Ejemplo real del proyecto

En la pantalla de busqueda de especialistas:

- el titulo de la pagina si se traduce con `patientSearch.title`
- el boton `Search` si se traduce con `patientSearch.searchButton`
- la descripcion del especialista no siempre se traduce, porque puede venir desde backend como texto libre

## 11. Como agregar un nuevo idioma

Si algun dia quieren agregar portugues o frances, el proceso base seria:

1. Crear un nuevo archivo en `frontend/src/assets/i18n/`, por ejemplo `pt.json`
2. Copiar la estructura de `es.json`
3. Traducir todos los valores
4. Actualizar `LanguageService` para aceptar el nuevo idioma
5. Agregar la opcion al selector de idioma

Ejemplo en `language.service.ts`:

```ts
setLanguage(lang: 'es' | 'en' | 'pt') {
  this.translate.use(lang);
  localStorage.setItem('lang', lang);
}
```

## 12. Checklist antes de subir cambios

Antes de hacer commit, revisa esto:

- agregue la clave en `es.json`
- agregue la misma clave en `en.json`
- use `| translate` en el HTML cuando correspondia
- use `TranslateService` en TypeScript cuando correspondia
- no deje textos hardcodeados por error
- probé cambiar de `ES` a `EN`

## 13. Resumen corto

En este proyecto la traduccion funciona asi:

- `app.config.ts` carga los archivos de idioma
- `LanguageService` cambia y guarda el idioma
- `es.json` y `en.json` contienen los textos
- el HTML usa `| translate`
- el TypeScript usa `TranslateService`

Si siguen esa estructura, cualquier pagina nueva podra traducirse de forma ordenada y sin duplicar codigo.
