import { createClient } from '../../shared/api.js';
import { SERVICES } from '../../shared/config.js';

const client = createClient(SERVICES.rideManagement);

export const rideApi = {
  listRides: (status) => client.get(`/api/rides${status ? `?status=${status}` : ''}`),
  createRide: (body) => client.post('/api/rides', body),
  accept: (id) => client.patch(`/api/rides/${id}/accept`),
  start: (id) => client.patch(`/api/rides/${id}/start`),
  complete: (id) => client.patch(`/api/rides/${id}/complete`),
  cancel: (id) => client.patch(`/api/rides/${id}/cancel`),
  decline: (id) => client.patch(`/api/rides/${id}/decline`),
  autoAssign: (id) => client.post(`/api/rides/${id}/auto-assign`),
};
