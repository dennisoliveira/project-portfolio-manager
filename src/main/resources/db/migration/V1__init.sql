-- ===========================
-- V1 - Initial schema
-- ===========================

create table member (
  id            bigserial primary key,
  external_id   varchar(100) not null,
  name          varchar(150) not null,
  role          varchar(50)  not null,
  created_at    timestamp not null default now(),
  updated_at    timestamp not null default now(),
  constraint chk_member_role check (role in ('FUNCIONARIO','GERENTE','OUTRO'))
);

create unique index ux_member_external_id on member (external_id);

create table project (
  id                 bigserial primary key,
  name               varchar(150) not null,
  start_date         date not null,
  expected_end_date  date not null,
  actual_end_date    date,
  total_budget       numeric(15,2) not null,
  description        text,
  manager_id         bigint not null,
  status             varchar(30) not null,
  created_at         timestamp not null default now(),
  updated_at         timestamp not null default now(),
  constraint fk_project_manager foreign key (manager_id) references member(id),
  constraint chk_project_status check (status in (
    'EM_ANALISE','ANALISE_REALIZADA','ANALISE_APROVADA','INICIADO',
    'PLANEJADO','EM_ANDAMENTO','ENCERRADO','CANCELADO'
  )),
  constraint chk_project_budget_positive check (total_budget > 0),
  constraint chk_project_dates_order check (expected_end_date >= start_date)
);

create index ix_project_status   on project(status);
create index ix_project_manager  on project(manager_id);

-- índices opcionais para filtros (remova se não precisar)
create index ix_project_start_date    on project(start_date);
create index ix_project_expected_date on project(expected_end_date);
create index ix_project_total_budget  on project(total_budget);

create table project_member (
  project_id bigint not null,
  member_id  bigint not null,
  created_at timestamp not null default now(),
  primary key (project_id, member_id),
  constraint fk_pm_project foreign key (project_id) references project(id) on delete cascade,
  constraint fk_pm_member  foreign key (member_id)  references member(id)  on delete restrict
);

create index ix_pm_member on project_member(member_id);