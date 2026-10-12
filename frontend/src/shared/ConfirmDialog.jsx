import { useCallback, useState } from 'react';
import Icon from './Icon.jsx';

export function useConfirm() {
  const [request, setRequest] = useState(null);

  const confirm = useCallback(
    (message, { title = 'Are you sure?', confirmLabel = 'Confirm', danger = true } = {}) =>
      new Promise((resolve) => setRequest({ message, title, confirmLabel, danger, resolve })),
    [],
  );

  function close(result) {
    request.resolve(result);
    setRequest(null);
  }

  const dialog = request && (
    <div className="modal-backdrop" onMouseDown={() => close(false)}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="confirm-title" onMouseDown={(e) => e.stopPropagation()}>
        <div className={`modal-icon ${request.danger ? 'is-danger' : ''}`}>
          <Icon name={request.danger ? 'alert' : 'bolt'} size={22} />
        </div>
        <h2 id="confirm-title">{request.title}</h2>
        <p className="muted">{request.message}</p>
        <div className="modal-actions">
          <button className="secondary" onClick={() => close(false)}>
            Keep it
          </button>
          <button className={request.danger ? 'danger-solid' : undefined} onClick={() => close(true)} autoFocus>
            {request.confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );

  return { confirm, dialog };
}
