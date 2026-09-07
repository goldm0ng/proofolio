create table users (
    id            uuid primary key,
    username      text not null unique,
    display_name  text,
    created_at    timestamptz not null default now()
);
insert into users (id, username, display_name) values ('00000000-0000-0000-0000-000000000001', 'me', 'Me');

create table companies (
    id              uuid primary key,
    owner_id        uuid not null references users(id),
    name            varchar(200) not null,
    industry        varchar(200),
    website         varchar(500),
    talent_profile  text,
    core_values     text[] not null default '{}',
    tech_stack      text[] not null default '{}',
    hiring_process  text,
    notes           text,
    created_at      timestamptz not null,
    updated_at      timestamptz not null,
    unique (owner_id, name)
);
create index idx_companies_owner on companies(owner_id);

create table applications (
    id              uuid primary key,
    owner_id        uuid not null references users(id),
    company_id      uuid not null references companies(id),
    position_title  varchar(300) not null,
    status          varchar(30) not null,
    deadline_at     timestamptz,
    posting_url     varchar(1000),
    employment_type varchar(100),
    location        varchar(200),
    notes           text,
    result          text,
    retrospective   text,
    created_at      timestamptz not null,
    updated_at      timestamptz not null
);
create index idx_applications_owner on applications(owner_id);
create index idx_applications_company on applications(company_id);
create index idx_applications_status on applications(status);
create index idx_applications_deadline on applications(deadline_at);

create table application_status_history (
    id              uuid primary key,
    application_id  uuid not null references applications(id) on delete cascade,
    status          varchar(30) not null,
    note            text,
    changed_at      timestamptz not null
);
create index idx_status_history_app on application_status_history(application_id);

create table requirements (
    id              uuid primary key,
    application_id  uuid not null references applications(id) on delete cascade,
    title           varchar(300) not null,
    kind            varchar(30) not null,
    done            boolean not null default false,
    note            text,
    sort_order      int not null default 0
);
create index idx_requirements_app on requirements(application_id);

create table essay_questions (
    id              uuid primary key,
    application_id  uuid not null references applications(id) on delete cascade,
    question        text not null,
    max_length      int,
    draft           text,
    status          varchar(20) not null default 'EMPTY',
    sort_order      int not null default 0,
    updated_at      timestamptz not null
);
create index idx_essay_questions_app on essay_questions(application_id);

create table projects (
    id          uuid primary key,
    owner_id    uuid not null references users(id),
    name        varchar(200) not null,
    tagline     varchar(500),
    started_at  date,
    ended_at    date,
    team_size   int,
    role        varchar(200),
    repo_url    varchar(500),
    deploy_url  varchar(500),
    visibility  varchar(20) not null default 'PRIVATE',
    created_at  timestamptz not null,
    updated_at  timestamptz not null
);
create index idx_projects_owner on projects(owner_id);

create table project_sections (
    id          uuid primary key,
    project_id  uuid not null references projects(id) on delete cascade,
    type        varchar(30) not null,
    body        text not null default '',
    sort_order  int not null default 0,
    unique (project_id, type)
);

create table project_tech_stack (
    id          uuid primary key,
    project_id  uuid not null references projects(id) on delete cascade,
    category    varchar(50) not null,
    name        varchar(100) not null,
    sort_order  int not null default 0
);
create index idx_project_tech_stack_project on project_tech_stack(project_id);

create table project_metrics (
    id          uuid primary key,
    project_id  uuid not null references projects(id) on delete cascade,
    label       varchar(200) not null,
    value       varchar(200) not null,
    sort_order  int not null default 0
);
create index idx_project_metrics_project on project_metrics(project_id);

create table experiences (
    id          uuid primary key,
    owner_id    uuid not null references users(id),
    project_id  uuid references projects(id) on delete set null,
    title       varchar(300) not null,
    situation   text not null default '',
    task        text not null default '',
    action      text not null default '',
    result      text not null default '',
    tags        text[] not null default '{}',
    visibility  varchar(20) not null default 'PRIVATE',
    created_at  timestamptz not null,
    updated_at  timestamptz not null
);
create index idx_experiences_owner on experiences(owner_id);
create index idx_experiences_project on experiences(project_id);
create index idx_experiences_tags on experiences using gin (tags);

create table experience_evidence (
    id              uuid primary key,
    experience_id   uuid not null references experiences(id) on delete cascade,
    type            varchar(20) not null,
    url             varchar(1000) not null,
    label           varchar(300),
    sort_order      int not null default 0
);
create index idx_experience_evidence_exp on experience_evidence(experience_id);

create table ai_calls (
    id                  uuid primary key,
    owner_id            uuid not null references users(id),
    task                varchar(100) not null,
    model               varchar(100) not null,
    status              varchar(10) not null,
    input_tokens        bigint not null default 0,
    output_tokens       bigint not null default 0,
    cache_read_tokens   bigint not null default 0,
    cost_usd            numeric(12,6) not null default 0,
    duration_ms         bigint not null default 0,
    error_message       text,
    created_at          timestamptz not null
);
create index idx_ai_calls_owner_created on ai_calls(owner_id, created_at desc);
