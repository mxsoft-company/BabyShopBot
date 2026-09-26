CREATE TABLE companies (
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL,
    slug                   VARCHAR(100) NOT NULL UNIQUE,
    bot_token              VARCHAR(255) UNIQUE,
    bot_username           VARCHAR(255) NOT NULL,
    onec_base_url          VARCHAR(500) NOT NULL,
    onec_username          VARCHAR(100) NOT NULL,
    onec_password          VARCHAR(255) NOT NULL,
    service_auth_username  VARCHAR(100) NOT NULL,
    service_auth_password  VARCHAR(255) NOT NULL,
    hrpulse_base_url       VARCHAR(500),
    hrpulse_token          VARCHAR(500),
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE employees (
    id                  BIGSERIAL PRIMARY KEY,
    company_id          BIGINT       NOT NULL REFERENCES companies(id),
    telegram_id         BIGINT       NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    employee_id_1c      VARCHAR(100) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    department          VARCHAR(255),
    employee_number     VARCHAR(100),
    hrpulse_employee_id VARCHAR(50),
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (company_id, telegram_id)
);

CREATE TABLE appeals (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES companies(id),
    employee_id     BIGINT       NOT NULL REFERENCES employees(id),
    type            VARCHAR(20)  NOT NULL,
    message         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    one_c_appeal_id VARCHAR(50),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (company_id, one_c_appeal_id)
);

CREATE TABLE appeal_messages (
    id         BIGSERIAL PRIMARY KEY,
    company_id BIGINT      NOT NULL REFERENCES companies(id),
    appeal_id  BIGINT      NOT NULL REFERENCES appeals(id),
    sender     VARCHAR(20) NOT NULL,
    message    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_log (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT       NOT NULL REFERENCES companies(id),
    employee_id BIGINT REFERENCES employees(id),
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT         NOT NULL,
    object_id   VARCHAR(100),
    status      VARCHAR(20)  NOT NULL DEFAULT 'SENT',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_company_id ON employees(company_id);
CREATE INDEX idx_employees_employee_id_1c ON employees(company_id, employee_id_1c);
CREATE INDEX idx_appeals_company_id ON appeals(company_id);
CREATE INDEX idx_appeals_employee_id ON appeals(employee_id);
CREATE INDEX idx_appeal_messages_appeal_id ON appeal_messages(appeal_id);
CREATE INDEX idx_notification_log_employee_id ON notification_log(employee_id);
