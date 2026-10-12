import { createClient } from '../../shared/api.js';
import { SERVICES } from '../../shared/config.js';

const client = createClient(SERVICES.farePayment);

export const farePaymentApi = {
  estimate: (body) => client.post('/api/fares/estimate', body),
  listFares: () => client.get('/api/fares'),
  createFare: (body) => client.post('/api/fares', body),
  voidFare: (id) => client.patch(`/api/fares/${id}/void`),
  listPayments: () => client.get('/api/payments'),
  pay: (body) => client.post('/api/payments', body),
  refund: (id) => client.post(`/api/payments/${id}/refund`),
};
