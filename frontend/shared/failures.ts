import { ApiFailure } from '@apocarteres/http';

// RBOT-API-001, MVP-02, REQ-API-003
export const FAILURE_TEXTS: Readonly<Record<string, string>> = {
  'credentials-rejected': 'Неверная почта или пароль.',
  'account-blocked': 'Учётная запись заблокирована. Обратитесь к администратору.',
  'email-unverified': 'Учётная запись не подтверждена. Обратитесь к администратору.',
  'entry-closed': 'Это действие недоступно.',
  'email-rejected': 'Почта указана с ошибкой.',
  'password-rejected': 'Пароль не подходит: проверьте длину.',
  'email-taken': 'Учётная запись с такой почтой уже есть.',
  'roles-rejected': 'Выберите роль из списка.',
  'account-missing': 'Учётная запись не найдена.',
  'self-change-refused': 'Свою учётную запись заблокировать нельзя.',
  'interval-rejected': 'Начало промежутка должно быть раньше конца.',
  'intervals-overlap': 'Промежутки пересекаются.',
  'range-rejected': 'Неверный диапазон дат.',
  'settings-rejected': 'Параметр записи вне допустимых границ.',
  'settings-incomplete': 'Заполните параметры записи в настройках.',
  'session-type-rejected': 'Проверьте название, длительность и цену.',
  'session-type-missing': 'Тип сессии не найден.',
  'authentication-required': 'Сессия закончилась. Войдите снова.',
  'rate-limited': 'Слишком много попыток. Подождите и попробуйте снова.',
  'rate-limit-unavailable': 'Служба временно недоступна. Попробуйте позже.',
};

// RBOT-API-001, REQ-API-003
export function failureMessage(failure: unknown): string {
  const code = failure instanceof ApiFailure ? failure.problem.code : '';
  return FAILURE_TEXTS[code] ?? 'Не получилось. Попробуйте ещё раз.';
}
