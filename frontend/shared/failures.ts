import { authFailureCode } from '@apocarteres/auth';

const MESSAGES: Readonly<Record<string, string>> = {
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
  'rate-limited': 'Слишком много попыток. Подождите и попробуйте снова.',
};

// MVP-01, REQ-API-003
export function failureMessage(failure: unknown): string {
  const code = authFailureCode(failure);
  return (code && MESSAGES[code]) || 'Не получилось. Попробуйте ещё раз.';
}
