--
-- Tigase XMPP Server - The instant messaging server
-- Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, version 3 of the License.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program. Look for COPYING file in the top folder.
-- If not, see http://www.gnu.org/licenses/.
--

-- QUERY START:
do $$
begin
    if to_regclass('public.msg_history') is not null then
        if to_regclass('public.tig_offline_messages') is null then
            alter table msg_history rename to tig_offline_messages;
        end if;
    else
        create table if not exists tig_offline_messages (
            msg_id bigserial,
            ts timestamp with time zone default now(),
            expired timestamp with time zone,
            sender varchar(2049),
            receiver varchar(2049) not null,
            msg_type int not null default 0,
            message text not null,

            primary key(msg_id)
        );
    end if;

    if not exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_offline_messages' and column_name = 'msg_type') then
        alter table tig_offline_messages
            add msg_type int not null default 0;
    end if;

    if exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_offline_messages' and column_name = 'message' and udt_name = 'varchar') then
        alter table tig_offline_messages alter message type text;
    end if;

    if not exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_offline_messages' and column_name = 'receiver') then
        alter table tig_offline_messages
            add receiver varchar(2049),
            add sender varchar(2049),
            alter msg_id type bigint;
    end if;

    if to_regclass('public.index_expired') is not null then
        drop index index_expired;
    end if;
    if to_regclass('public.index_receiver_uid_sender_uid') is not null then
        drop index index_receiver_uid_sender_uid;
    end if;
    if to_regclass('public.index_sender_uid_receiver_uid') is not null then
        drop index index_sender_uid_receiver_uid;
    end if;

    if to_regclass('public.user_jid') is not null and exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_offline_messages' and column_name = 'receiver_uid') then
    	update tig_offline_messages
    	set receiver = (select jid from user_jid where jid_id = receiver_uid),
        	sender = (select jid from user_jid where jid_id = sender_uid)
    	where receiver is null;
    end if;

    alter table tig_offline_messages
        drop if exists receiver_uid,
        drop if exists sender_uid;

    alter table tig_offline_messages
        alter receiver set not null;

    if to_regclass('public.tig_offline_messages_pkey') is null then
        alter table tig_offline_messages add primary key (msg_id);
    end if;

    if to_regclass('public.broadcast_msgs') is not null then
        if to_regclass('tig_broadcast_messages') is null then
            alter table broadcast_msgs rename to tig_broadcast_messages;
            alter table tig_broadcast_messages alter msg type text;
        end if;
    else
        create table if not exists tig_broadcast_messages (
            id varchar(128) not null,
            expired timestamp with time zone not null,
			msg text not null,
			primary key (id)
			);
    end if;

    create table if not exists tig_broadcast_jids (
        jid_id bigserial,
        jid varchar(2049) not null,

        primary key (jid_id)
    );
    if to_regclass('public.tig_broadcast_jids_jid') is null then
        create index tig_broadcast_jids_jid on tig_broadcast_jids (lower(jid));
    end if;

    if to_regclass('public.user_jid') is not null and to_regclass('public.broadcast_msgs_recipients') is not null then
        insert into tig_broadcast_jids (jid)
            select u.jid
            from user_jid u
            inner join broadcast_msgs_recipients b on u.jid_id = b.jid_id
            where not exists (select 1 from tig_broadcast_jids bj where bj.jid = u.jid);
    end if;

    create table if not exists tig_broadcast_recipients (
        msg_id varchar(128) not null references tig_broadcast_messages(id),
        jid_id bigint not null references tig_broadcast_jids(jid_id),
        primary key (msg_id, jid_id)
    );

    if to_regclass('public.user_jid') is not null and to_regclass('public.broadcast_msgs_recipients') is not null then
        insert into tig_broadcast_recipients (msg_id, jid_id)
            select x.msg_id, x.jid_id
            from (select bmr.msg_id, bj.jid_id
                from broadcast_msgs_recipients bmr
                inner join user_jid uj on uj.jid_id = bmr.jid_id
                inner join tig_broadcast_jids bj on lower(bj.jid) = lower(uj.jid)
            ) x
            where not exists (select 1 from tig_broadcast_recipients br where br.msg_id = x.msg_id and br.jid_id = x.jid_id);
    end if;

    if to_regclass('public.broadcast_msgs_recipients') is not null then
        drop table broadcast_msgs_recipients;
    end if;

    if to_regclass('public.user_jid') is not null then
        drop table user_jid;
    end if;
end$$;
-- QUERY END:

-- QUERY START:
do $$
begin
    if to_regclass('public.tig_offline_messages_expired') is null then
        create index tig_offline_messages_expired on tig_offline_messages (expired);
    end if;
    if to_regclass('public.tig_offline_messages_receiver') is null then
        create index tig_offline_messages_receiver on tig_offline_messages (lower(receiver));
    end if;
    if to_regclass('public.tig_offline_messages_receiver_sender') is null then
        create index tig_offline_messages_receiver_sender on tig_offline_messages (lower(receiver), lower(sender));
    end if;
end$$;
-- QUERY END:

-- QUERY START:
create table if not exists tig_cluster_nodes (
    hostname varchar(512) not null,
	secondary varchar(512),
    password varchar(255) not null,
    last_update timestamp with time zone default current_timestamp,
    port int,
    cpu_usage double precision not null,
    mem_usage double precision not null,
    primary key (hostname)
);
-- QUERY END:

-- QUERY START:
do $$
begin
    alter table tig_offline_messages
        alter column ts type timestamp with time zone,
        alter column expired type timestamp with time zone;
    alter table tig_broadcast_messages
        alter column expired type timestamp with time zone;

    alter table tig_cluster_nodes
        alter column last_update type timestamp with time zone;
        
    alter table tig_users
        alter column acc_create_time type timestamp with time zone,
        alter column last_login type timestamp with time zone,
	    alter column last_logout type timestamp with time zone;
end$$;
-- QUERY END:

-- ------------- Credentials support
-- QUERY START:
create table if not exists tig_user_credentials (
    uid bigint not null references tig_users(uid),
    username varchar(2049) not null,
    mechanism varchar(128) not null,
    value text not null,

    primary key (uid, username, mechanism)
);
-- QUERY END:

-- QUERY START:
do $$
begin
    if not exists (select 1 from tig_user_credentials) and exists( select 1 from pg_proc where proname = lower('TigGetDBProperty')) then
        insert into tig_user_credentials (uid, username, mechanism, value)
            select uid, 'default', COALESCE(TigGetDBProperty('password-encoding'), 'PLAIN'), user_pw
            from tig_users where user_pw is not null;
            
        update tig_users set user_pw = null where user_pw is not null;
    end if;
end$$;
-- QUERY END:
