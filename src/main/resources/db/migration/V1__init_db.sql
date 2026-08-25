create table t_subjects (
    id serial primary key,
    name VARCHAR(100) NOT NULL,
    created_at timestamp with time zone default current_timestamp
);

create table t_users (
    id bigint primary key,
    username varchar(100),
    first_name varchar(100) not null,
    current_state varchar(50) not null default 'CHOOSING_SUBJECT',
    selected_subject_id int,
    notification_time time,
    created_at timestamp with time zone default current_timestamp,

    foreign key (selected_subject_id) references t_subjects(id) on delete set null
);

create table tr_user_subject (
    user_id bigint not null,
    subject_id int not null,
    role varchar(30) default 'MEMBER',
    created_at timestamp with time zone default current_timestamp,

    primary key (user_id, subject_id),
    foreign key (user_id) references t_users(id) on delete cascade,
    foreign key (subject_id) references t_subjects(id) on delete cascade
);
