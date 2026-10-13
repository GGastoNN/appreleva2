# Grupo IDEA - Relevamientos v1.3.6

- La marca pasa a ser corporativa y fija para Grupo IDEA.
- Se incorpora el logo oficial provisto dentro del APK como recurso de la aplicación.
- El logo ya no puede importarse, quitarse ni reemplazarse desde la interfaz.
- La pantalla Marca queda únicamente como vista informativa de la identidad corporativa.
- El sello de fotografías usa siempre la marca fija de Grupo IDEA.
- Se conserva la generación normal de APK debug mediante GitHub Actions.
- applicationId y namespace cambiados a `com.grupoidea.relevapp`.
- Sin cambios en el formato `.gidea`; sigue siendo compatible con respaldos anteriores.

## Importante al instalar

El nuevo `applicationId` hace que Android trate esta versión como una aplicación distinta de `com.illu.relevametal`. Los datos locales de una instalación anterior no se trasladan automáticamente. Para conservar una obra, exportarla primero a `.gidea` desde la versión anterior e importarla en esta versión.
