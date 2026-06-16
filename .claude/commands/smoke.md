# /smoke — Smoke test del contexto Spring

Ejecuta `ApplicationContextSmokeTest` para verificar que el contexto de Spring arranca correctamente antes de un despliegue.

Este test detecta bugs de wiring (beans faltantes, @Configuration rotos) que los tests unitarios no pueden detectar porque nunca levantan el `ApplicationContext`.

## Pasos

1. Ejecuta el smoke test:

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis && \
  mvn test -pl dentis-api -Dtest=ApplicationContextSmokeTest --no-transfer-progress 2>&1 | \
  grep -E "BUILD|Tests run|ERROR|shouldBe|APPLICATION FAILED"
```

2. Si el resultado es `BUILD SUCCESS` con `Tests run: 2, Failures: 0, Errors: 0` — el contexto arranca correctamente y el deploy puede proceder.

3. Si falla, muestra el error completo:

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis && \
  mvn test -pl dentis-api -Dtest=ApplicationContextSmokeTest --no-transfer-progress 2>&1 | \
  grep -A 20 "Caused by\|APPLICATION FAILED\|UnsatisfiedDep\|BeanCreation"
```

4. Diagnostica y corrige el error de wiring antes de continuar con el deploy.
