--liquibase formatted sql

--changeset vonhu:issue-promotion-0003
drop table if exists promotion;

--changeset vonhu:issue-promotion-0004
create table promotion(
    id bigint not null auto_increment,
    name varchar(255) not null,
    slug varchar(255) not null,
    description varchar(255) null,
    coupon_code varchar(255),
    discount_percentage bigint not null,
    discount_amount bigint not null,
    is_active boolean not null,
    start_date timestamp null,
    end_date timestamp null,
    created_by varchar(255),
    created_on timestamp,
    last_modified_by varchar(255),
    last_modified_on timestamp,
    primary key (id)
);
