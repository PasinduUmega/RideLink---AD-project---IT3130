import { createClient } from '../../shared/api.js';
import { SERVICES } from '../../shared/config.js';

const client = createClient(SERVICES.driverVehicle);

export const driverVehicleApi = {
  listDrivers: () => client.get('/api/drivers'),
  createDriver: (body) => client.post('/api/drivers', body),
  setDriverStatus: (id, status) => client.patch(`/api/drivers/${id}/status`, { status }),
  listVehicles: () => client.get('/api/vehicles'),
  createVehicle: (body) => client.post('/api/vehicles', body),
  activateVehicle: (id) => client.patch(`/api/vehicles/${id}/activate`),
  deactivateVehicle: (id) => client.patch(`/api/vehicles/${id}/deactivate`),
  deleteVehicle: (id) => client.del(`/api/vehicles/${id}`),
};
