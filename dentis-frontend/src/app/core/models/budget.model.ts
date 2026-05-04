export interface Budget {
  id: string;
  patientId: string;
  patientName: string;
  dentistId?: string;
  dentistName?: string;
  treatmentPlanId?: string;
  issueDate: string;
  expiryDate: string;
  status: BudgetStatus;
  paymentStatus: PaymentStatus;
  notes?: string;
  currency?: string;
  totalAmount: number;
  totalPaid: number;
  balance: number;
  items: BudgetItem[];
  payments: Payment[];
  createdAt: string;
}

export interface BudgetItem {
  id: string;
  feeItemId?: string;
  feeItemName?: string;
  description: string;
  unitPrice: number;
  discountPercentage: number;
  quantity: number;
  totalPrice: number;
  toothNumber?: string;
  procedureStatus: TreatmentStatus;
  performedDate?: string;
  invoiceReference?: string;
}

export interface Payment {
  id: string;
  budgetId: string;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentDate: string;
  reference?: string;
  invoiceNumber?: string;
  currency?: string;
  notes?: string;
}

export type BudgetStatus = 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'PARTIALLY_COMPLETED' | 'COMPLETED';
export type PaymentStatus = 'PENDING' | 'PARTIAL' | 'PAID' | 'OVERPAID' | 'REFUNDED' | 'CANCELLED';
export type TreatmentStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'ON_HOLD';
export type PaymentMethod = 'CASH' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'BANK_TRANSFER' | 'MOBILE_PAYMENT' | 'CHECK' | 'OTHER';

export interface CreateBudgetRequest {
  patientId: string;
  dentistId?: string;
  treatmentPlanId?: string;
  issueDate: string;
  expiryDate: string;
  notes?: string;
  currency?: string;
  items: CreateBudgetItemRequest[];
}

export interface CreateBudgetItemRequest {
  feeItemId?: string;
  description: string;
  unitPrice: number;
  discountPercentage?: number;
  quantity: number;
  toothNumber?: string;
}

export interface CreatePaymentRequest {
  budgetId: string;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentDate: string;
  reference?: string;
  invoiceNumber?: string;
  currency?: string;
  notes?: string;
}
