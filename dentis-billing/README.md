# dentis-billing

Bounded context de aranceles, presupuestos y pagos. Controla el ciclo financiero del tratamiento: desde la creación del catálogo de prestaciones hasta el registro de pagos y el cálculo de saldos.

## Modelo de Dominio

```
Tariff (catálogo de prestaciones)
├── code: String            (código único, ej: ORT-001)
├── name: String
├── category: TariffCategory
├── basePrice: BigDecimal
├── discountAllowed: boolean
└── active: boolean

TariffCategory:
  LABORATORY | SUPPLIES | ORTHODONTICS | SURGERY | GENERAL_DENTISTRY
  SPECIALTY | IMAGING | ENDODONTICS | PERIODONTICS | PEDIATRIC_DENTISTRY
  ADMINISTRATIVE | OTHER

Budget (presupuesto de tratamiento)
├── patientId / dentistId / treatmentPlanId: UUID
├── items: List<BudgetItem>
├── status: BudgetStatus    (DRAFT → PRESENTED → APPROVED → REJECTED | EXPIRED)
└── grandTotal()            (calculado: subtotal - descuentos)

BudgetItem
├── tariffId / description: String
├── quantity: int
├── unitPrice / discountPercentage: BigDecimal
├── performed: boolean       (¿se realizó el procedimiento?)
└── paymentStatus: ProcedurePaymentStatus  (PENDING | PARTIALLY_PAID | PAID)

Payment (registro de pago — no facturación)
├── patientId / budgetId: UUID
├── amount: BigDecimal
├── paymentMethod: PaymentMethod
│   (CASH | BANK_TRANSFER | DEBIT_CARD | CREDIT_CARD | MOBILE_PAYMENT | CRYPTOCURRENCY | OTHER)
├── invoiceReference: String  (número de factura manual, opcional)
└── notes: String

BudgetSummary (vista financiera)
├── grandTotal / totalPaid / balance: BigDecimal
└── status: PaymentSummaryStatus  (PENDING | PARTIALLY_PAID | PAID)
```

## Flujo de Trabajo

```
1. Crear aranceles en el catálogo (Tariff)
2. Crear presupuesto (Budget en estado DRAFT)
3. Presentar al paciente (PRESENTED)
4. Paciente aprueba → (APPROVED)
5. Registrar pagos (Payment) → saldo se actualiza en BudgetSummary
6. Marcar procedimientos como realizados (BudgetItem.performed)
```

## Reglas de Negocio

- Solo se pueden registrar pagos en presupuestos con estado `APPROVED`.
- Un pago no puede superar el saldo pendiente del presupuesto.
- `discountAllowed = false` en el `Tariff` impide aplicar descuento al item.
- El módulo registra el tipo de pago y el monto — **no genera facturas** (campo `invoiceReference` es referencia manual).

## API (expuesta por dentis-api)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/billing/budgets` | Crear presupuesto |
| `GET` | `/api/v1/billing/budgets/{id}` | Obtener presupuesto |
| `PATCH` | `/api/v1/billing/budgets/{id}/approve` | Aprobar presupuesto |
| `GET` | `/api/v1/billing/budgets/{id}/summary` | Resumen financiero (saldo) |
| `POST` | `/api/v1/billing/payments` | Registrar pago |
| `GET` | `/api/v1/billing/payments/budget/{id}` | Pagos de un presupuesto |
| `GET` | `/api/v1/billing/payments/patient/{id}` | Historial de pagos del paciente |

## Tests

```bash
mvn test -pl dentis-billing
```

Cobertura: `BudgetService` — creación, aprobación, cálculo de estados PAID/PARTIALLY_PAID/PENDING.
