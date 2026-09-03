alter table users modify nickname varchar(50) null;
create unique index uk_users_phone on users (phone);

create table account_verifications (
    id varchar(64) not null,
    phone varchar(32) not null,
    code_hash varchar(128) not null,
    expires_at datetime(6) not null,
    resend_available_at datetime(6) not null,
    consumed_at datetime(6),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create index idx_account_verification_phone on account_verifications (phone);

create table account_sessions (
    id varchar(64) not null,
    account_id varchar(64) not null,
    created_at datetime(6) not null,
    revoked_at datetime(6),
    primary key (id),
    constraint fk_account_sessions_account foreign key (account_id) references users (id)
) engine=InnoDB;

create index idx_account_sessions_account on account_sessions (account_id);
