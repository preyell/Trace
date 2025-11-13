CREATE TABLE IF NOT EXISTS app_user (
  id          BIGSERIAL PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(100) NOT NULL,
  enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
  first_name  VARCHAR(100) NOT NULL,
  last_name   VARCHAR(100) NOT NULL,
  email       VARCHAR(200) NOT NULL UNIQUE
);


CREATE TABLE IF NOT EXISTS user_role (
  user_id   BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  role_name VARCHAR(50) NOT NULL,
  PRIMARY KEY (user_id, role_name)
);


-- Optional helpful indexes
CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(username);

-- New table (fresh setup)
CREATE TABLE customers (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(128) NOT NULL,
  contact_name VARCHAR(128),
  email        VARCHAR(128),
  phone        VARCHAR(32),
  active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT now(),
  updated_at   TIMESTAMP
);

-- Ensure names are unique (case-insensitive) if you want to prevent dupes
CREATE UNIQUE INDEX uq_customers_name_ci ON customers (lower(name));



-- verticals
CREATE TABLE IF NOT EXISTS vertical (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ
);
-- case-insensitive unique name
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='uk_vertical_name_ci'
  ) THEN
    CREATE UNIQUE INDEX uk_vertical_name_ci ON vertical (LOWER(name));
  END IF;
END$$;

CREATE TABLE IF NOT EXISTS user_locations (
  user_id BIGINT NOT NULL,
  location VARCHAR(32) NOT NULL,
  CONSTRAINT fk_user_locations_user
    FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS user_vertical (
  user_id    BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  vertical_id BIGINT NOT NULL REFERENCES vertical(id) ON DELETE CASCADE,
  PRIMARY KEY(user_id, vertical_id)
);

CREATE TABLE IF NOT EXISTS activation_token (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  token VARCHAR(100) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_activation_token_user ON activation_token(user_id);

CREATE TABLE IF NOT EXISTS orders (
  id               BIGSERIAL PRIMARY KEY,
  customer_id      BIGINT NOT NULL,
  description      VARCHAR(500),
  sales_manager_id BIGINT NOT NULL,
  location         VARCHAR(32) NOT NULL, -- enum stored as text: 'KENYA' / 'TANZANIA'
  created_by_id    BIGINT NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,

  CONSTRAINT fk_orders_sales_manager
    FOREIGN KEY (sales_manager_id) REFERENCES app_user(id) ON DELETE RESTRICT,

  CONSTRAINT fk_orders_created_by
    FOREIGN KEY (created_by_id) REFERENCES app_user(id) ON DELETE RESTRICT
    
    );
    CREATE TABLE IF NOT EXISTS order_vertical (
  order_id    BIGINT NOT NULL,
  vertical_id BIGINT NOT NULL,
  PRIMARY KEY(order_id, vertical_id),
  CONSTRAINT fk_order_vertical_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_vertical_vertical
    FOREIGN KEY (vertical_id) REFERENCES vertical(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_orders_location ON orders(location);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_sales_manager ON orders(sales_manager_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_by ON orders(created_by_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);


CREATE TABLE IF NOT EXISTS password_reset_token (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  token VARCHAR(100) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_prt_user ON password_reset_token(user_id);
CREATE INDEX IF NOT EXISTS idx_prt_active ON password_reset_token(token) WHERE used_at IS NULL;

create table margin_reports (
  id bigint generated always as identity primary key,
  order_id bigint not null,
  vertical_id bigint not null,
  uploaded_by bigint not null,
  buying_price numeric(24,6) not null,
  buying_currency varchar(3) not null,
  selling_price numeric(24,6) not null,
  selling_currency varchar(3) not null,
  conversion_rate numeric(24,8) not null,
  file_name varchar(255) not null,
  storage_key varchar(255) not null,
  approval_status varchar(20) not null,
  comments varchar(1000) not null,
  uploaded_on timestamp not null
);

alter table margin_reports add constraint fk_mr_order foreign key (order_id) references orders(id);
alter table margin_reports add constraint fk_mr_vertical foreign key (vertical_id) references vertical(id);
alter table margin_reports add constraint fk_mr_user foreign key (uploaded_by) references app_user(id);

create index idx_mr_order on margin_reports(order_id);
create index idx_mr_uploaded on margin_reports(uploaded_on desc);


CREATE TABLE margin_report_audit(
  id BIGSERIAL PRIMARY KEY,
  margin_report_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  actor_id BIGINT NOT NULL,
  acted_on TIMESTAMP NOT NULL,
  note VARCHAR(1000),
  CONSTRAINT fk_mra_mr FOREIGN KEY (margin_report_id) REFERENCES margin_reports(id),
  CONSTRAINT fk_mra_user FOREIGN KEY (actor_id) REFERENCES app_user(id)
);
CREATE INDEX idx_mra_mr ON margin_report_audit(margin_report_id);

-- V20251030__mr_audit_on_delete_cascade.sql
ALTER TABLE margin_report_audit
  DROP CONSTRAINT IF EXISTS fk_mra_mr;

ALTER TABLE margin_report_audit
  ADD CONSTRAINT fk_mra_mr
  FOREIGN KEY (margin_report_id)
  REFERENCES margin_reports (id)
  ON DELETE CASCADE;
