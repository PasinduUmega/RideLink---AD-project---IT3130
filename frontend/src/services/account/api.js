import { createClient } from '../../shared/api.js';
import { SERVICES } from '../../shared/config.js';

const client = createClient(SERVICES.account);

export const accountApi = {
  login: (body) => client.post('/api/auth/login', body),
  register: (body) => client.post('/api/auth/register', body),
  listUsers: () => client.get('/api/users'),
  activateUser: (id) => client.patch(`/api/users/${id}/activate`),
  deactivateUser: (id) => client.patch(`/api/users/${id}/deactivate`),
};
