CREATE TABLE consent_text (
  id UUID PRIMARY KEY,
  practitioner_id UUID NOT NULL,
  version INT NOT NULL CHECK (version > 0),
  body TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT consent_text_practice_version UNIQUE (practitioner_id, version)
);

INSERT INTO consent_text (id, practitioner_id, version, body, created_at)
SELECT gen_random_uuid(), c.practitioner_id, 1, 'Черновик, версия 1. Я соглашаюсь, что сервис записи к психологу обрабатывает мой идентификатор Telegram, подпись, которую дал мне психолог, и сведения о моих записях (дата, время, тип встречи), чтобы записывать меня на сессии, переносить и отменять их. Данные хранятся на сервере в России. Сообщения бота передаются через Telegram и сервер-посредник в Финляндии. Согласие можно отозвать, написав психологу.', now()
FROM consent s JOIN client c ON c.id = s.client_id
GROUP BY c.practitioner_id;
