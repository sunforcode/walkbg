create table account_avatar_media (
    id varchar(64) not null,
    media_reference varchar(64) not null,
    account_id varchar(64) not null,
    content_type varchar(32) not null,
    byte_size bigint not null,
    storage_name varchar(128) not null,
    created_at datetime(6) not null,
    primary key (id),
    constraint uk_account_avatar_media_reference unique (media_reference),
    constraint uk_account_avatar_media_storage_name unique (storage_name),
    constraint fk_account_avatar_media_account foreign key (account_id) references users (id)
) engine=InnoDB;

create index idx_account_avatar_media_account on account_avatar_media (account_id);
