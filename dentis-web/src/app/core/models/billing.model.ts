export type BudgetStatus = 'DRAFT' | 'PRESENTED' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'PENDING_APPROVAL' | 'COMPLETED';
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'MOBILE_PAYMENT' | 'CRYPTOCURRENCY' | 'OTHER';
export type PaymentSummaryStatus = 'PAID' | 'PARTIALLY_PAID' | 'PENDING';
export type TariffCategory =
  | 'LABORATORY'
  | 'SUPPLIES'
  | 'ORTHODONTICS'
  | 'SURGERY'
  | 'GENERAL_DENTISTRY'
  | 'SPECIALTY'
  | 'IMAGING'
  | 'ENDODONTICS'
  | 'PERIODONTICS'
  | 'PEDIATRIC_DENTISTRY'
  | 'ADMINISTRATIVE'
  | 'OTHER';

export interface Tariff {
  id: string;
  code: string;
  name: string;
  description?: string;
  basePrice: number;
  category: TariffCategory;
  discountAllowed: boolean;
  active: boolean;
}

export interface CreateTariffRequest {
  code: string;
  name: string;
  description?: string;
  category: TariffCategory;
  basePrice: number;
  discountAllowed?: boolean;
}

export interface BudgetItem {
  id?: string;
  tariffId: string;
  tariffName?: string;
  description?: string;
  quantity: number;
  unitPrice: number;
  total?: number;
  discountPercentage?: number;
  performed?: boolean;
  paymentStatus?: string;
  notes?: string;
}

export interface Budget {
  id: string;
  patientId: string;
  patientName?: string;
  dentistId: string;
  dentistName?: string;
  treatmentPlanId?: string;
  items: BudgetItem[];
  status: BudgetStatus;
  totalAmount?: number;
  notes?: string;
  createdAt: string;
  approvedAt?: string;
}

export interface CreateBudgetRequest {
  patientId: string;
  dentistId: string;
  treatmentPlanId?: string;
  items: Array<{
    tariffId: string;
    description: string;
    quantity: number;
    unitPrice: number;
    discountPercentage?: number;
  }>;
  notes?: string;
}

export interface BudgetSummary {
  budgetId: string;
  grandTotal: number;
  totalPaid: number;
  balance: number;
  status: PaymentSummaryStatus;
}

export interface Payment {
  id: string;
  budgetId: string;
  patientId: string;
  amount: number;
  paymentMethod: PaymentMethod;
  invoiceReference?: string;
  notes?: string;
  paidAt: string;
}

