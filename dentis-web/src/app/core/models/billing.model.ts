export type BudgetStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'OTHER';

export interface Tariff {
  id: string;
  name: string;
  description?: string;
  price: number;
  category: string;
  active: boolean;
}

export interface BudgetItem {
  id?: string;
  tariffId: string;
  tariffName: string;
  quantity: number;
  unitPrice: number;
  total: number;
  notes?: string;
}

export interface Budget {
  id: string;
  patientId: string;
  patientName: string;
  dentistId: string;
  dentistName: string;
  items: BudgetItem[];
  status: BudgetStatus;
  totalAmount: number;
  notes?: string;
  createdAt: string;
}

export interface BudgetSummary {
  budgetId: string;
  totalAmount: number;
  totalPaid: number;
  totalPending: number;
  status: BudgetStatus;
}

export interface Payment {
  id: string;
  budgetId: string;
  patientId: string;
  amount: number;
  method: PaymentMethod;
  reference?: string;
  notes?: string;
  paidAt: string;
}

