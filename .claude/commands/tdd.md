# /tdd — Ciclo TDD para dentis-api

Implementa una feature siguiendo TDD estricto: test primero, luego código, luego verificación.

**Argumento:** `$ARGUMENTS` — descripción de la feature a implementar.

## Pasos

1. **Entiende el contexto** — busca código relacionado con `$ARGUMENTS` para saber dónde encaja la nueva feature (dominio, servicio, repositorio, controller).

2. **Escribe el test primero** — crea o añade a un test existente siguiendo las convenciones del proyecto:
   - Clase: `*Test.java` con `@ExtendWith(MockitoExtension.class)` para tests unitarios
   - Método: `shouldBe<DescripciónDelComportamiento>()` con `@DisplayName`
   - Usa `@Mock` / `@InjectMocks` + AssertJ para assertions
   - El test debe **fallar** en este punto (red)

3. **Verifica que el test falla** — compila y ejecuta solo ese test para confirmar que está en rojo:

```bash
mvn test -pl dentis-api -Dtest=<NombreDelTest> --no-transfer-progress -q 2>&1 | tail -15
```

4. **Implementa el mínimo código** para que el test pase — sin gold-plating, sin features extras.

5. **Verifica que el test pasa** — vuelve a ejecutar el test (green):

```bash
mvn test -pl dentis-api -Dtest=<NombreDelTest> --no-transfer-progress -q 2>&1 | tail -5
```

6. **Ejecuta la suite completa** para detectar regresiones:

```bash
mvn test -pl dentis-api --no-transfer-progress -q 2>&1 | tail -8
```

7. **Refactoriza si es necesario** — mejora legibilidad sin cambiar comportamiento. Vuelve al paso 6 para confirmar que todo sigue verde.
