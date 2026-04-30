# 5. Control de Versiones con Git

## Uso de Git y GitHub

El código fuente del proyecto se gestiona completamente con **Git** y está alojado en un repositorio público de **GitHub**, facilitando la colaboración, la trazabilidad de cambios y la transparencia del proceso de desarrollo.

- **Repositorio:** Yupay Turismo
- **URL:** `https://github.com/MaLu-afk/rural-tourism-project`

## Flujo de trabajo (Git Flow simplificado)

Se adopta un flujo de trabajo basado en ramas que permite el desarrollo paralelo de funcionalidades y una integración controlada:

### Ramas principales
- `main`: rama de producción. Contiene únicamente código estable, probado y listo para ser entregado.
- `develop`: rama de integración. Todas las funcionalidades se fusionan aquí antes de pasar a `main`.

### Ramas auxiliares
- `feature/<nombre>`: cada nueva funcionalidad o módulo se desarrolla en una rama dedicada (ej. `feature/registro-offline`, `feature/insights-dashboard`).
- `bugfix/<descripción>`: para corrección de errores detectados durante las pruebas.
- `release/<versión>`: se crea cuando `develop` está listo para una versión candidata (ej. `release/v0.1`).

### Ciclo de trabajo
1. El desarrollador crea una rama `feature/` desde `develop`.
2. Realiza commits atómicos con mensajes descriptivos siguiendo la convención de **Conventional Commits** (`feat`, `fix`, `docs`, `refactor`, `test`, `chore`).
3. Al finalizar, abre un **Pull Request** hacia `develop`.
4. Al menos otro miembro del equipo revisa el código y, una vez aprobado, se fusiona.
5. Periódicamente se fusiona `develop` en `main` y se etiqueta con el número de versión (ej. `v0.1.0`).

## Convenciones de commits

- **feat:** nueva funcionalidad
- **fix:** corrección de error
- **docs:** cambios en documentación
- **refactor:** reestructuración de código sin cambiar funcionalidad
- **test:** adición o modificación de pruebas
- **chore:** tareas de mantenimiento, configuración del proyecto

*Ejemplo:*  
`feat(registro): añadir formulario dinámico según tipo de emprendimiento`

