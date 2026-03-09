# Take Care - Plataforma de Acompañamiento Terapéutico
## Descripción
Take Care es una solución integral para la salud mental que conecta a pacientes con especialistas verificados. El sistema permite el registro seguro, la validación de credenciales profesionales, la gestión de citas y el seguimiento terapéutico bajo un entorno de alta seguridad y diseño empático.

## Tecnologías Utilizadas
### Backend
* Spring Boot
* Spring Data JPA
* Hibernate
* Apache Maven
* MySQL

### Frontend
* Angular 18+
* TypeScript
* SCSS
* Angular Material

## Equipo
* Gutierrez Davila Valeria Emilse 
* Iporre Cervantes Alexandra Gabriel
* Montes Iñiguez Isabella Catherine
* Rivera Mendez Henry Alejandro
* Salinas Vega Alejandra

## Flujo de Trabajo (Git)
Para mantener el orden, el equipo utiliza el nombre de las Historias de Usuario para las ramas:
* main: Rama estable.
* HUXX-Nombre-Historia: Ramas de desarrollo.


README BACKEND

------------------------------------------------------------------------------------------------------

# Cómo ejecutar el backend del proyecto

Este proyecto es un backend desarrollado con **Spring Boot** que utiliza **Maven** como gestor de dependencias y **MySQL** como base de datos.

## Requisitos previos

Antes de ejecutar el proyecto asegúrate de tener instalado:

* Java (versión 17 o superior)
* Apache Maven
* MySQL
* (Opcional) MySQL Workbench para administrar la base de datos

---

# Verificar instalación de Java

En la terminal ejecutar:

```bash
java -version
```

Debería mostrar algo similar a:

```text
java version "17.x.x"
```

También verificar el compilador:

```bash
javac -version
```

---

# Verificar instalación de Maven

Ejecutar:

```bash
mvn -version
```

Salida esperada:

```text
Apache Maven 3.x.x
Java version: 17
```

# Configurar la base de datos

Crear la base de datos:

```sql
CREATE DATABASE takecarebd;
```

Importar el dump de nuestra bd.

---

# Configurar conexión a la base de datos

Editar el archivo:

```text
src/main/resources/application.properties
```

Configurar los datos de conexión:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/takecarebd
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD

spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate
```
---

# Compilar el proyecto

Antes de ejecutar se recomienda compilar:

```bash
mvn clean install
```

Esto descarga dependencias y genera el `jar`.

---

# Ejecutar el proyecto

Desde la carpeta `backend` ejecutar:

```bash
mvn spring-boot:run
```

Si todo está configurado correctamente aparecerá en consola:

```text
Tomcat started on port(s): 8080
Started TakeCareApplication
```

---

# Estructura del proyecto

El backend sigue una arquitectura por capas basada en **Clean Architecture**:

```
com.takecare.backend

config
shared
usuario
especialista
cita
reporte
contenido
```

Cada módulo contiene:

```
controller
service
repository
model
```

---

# 🛠 Tecnologías utilizadas

* Spring Boot
* Spring Data JPA
* Hibernate
* Apache Maven
* MySQL

-------------------------------------------------------------------------------------------------------

