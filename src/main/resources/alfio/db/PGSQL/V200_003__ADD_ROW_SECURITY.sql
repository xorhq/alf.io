--
-- This file is part of alf.io.
--
-- alf.io is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- alf.io is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
--

-- we need to switch from the "postgres" role so we can apply the policy
create role application_user;


-- this function return true if the paramenter $1 (which is the organization id)
-- is present in the alfio.currentUserOrgs array OR simply
-- alfio.checkRowAccess is not set
create or replace function alfio_check_row_access(integer)
returns boolean
as
$$
    select
        coalesce(current_setting('alfio.checkRowAccess', true)::boolean, false) and
        $1 = ANY(coalesce(current_setting('alfio.currentUserOrgs', true), '{}')::integer[]) -- check if org_id is present in alfio.currentUserOrgs
$$ language sql;


-- enable row level security and create policy
alter table organization enable row level security;
create policy organization_access_policy on organization to application_user
    using (alfio_check_row_access(id))
    with check (alfio_check_row_access(id));


alter table j_user_organization enable row level security;
create policy j_user_organization_access_policy on j_user_organization to application_user
    using (alfio_check_row_access(org_id))
    with check (alfio_check_row_access(org_id));

--

alter table event enable row level security;
create policy event_access_policy on event to application_user
    using (alfio_check_row_access(org_id))
    with check (alfio_check_row_access(org_id));


-- resources tables
alter table resource_organizer enable row level security;
create policy resource_organizer_access_policy on resource_organizer to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));


alter table resource_event enable row level security;
create policy resource_event_access_policy on resource_event to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));
--

-- configuration tables
alter table configuration_organization enable row level security;
create policy configuration_organization_access_policy on configuration_organization to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));


alter table configuration_event enable row level security;
create policy configuration_event_access_policy on configuration_event to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));


alter table configuration_ticket_category enable row level security;
create policy configuration_ticket_category_access_policy on configuration_ticket_category to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));
--

--
alter table invoice_sequences enable row level security;
create policy invoice_sequences_access_policy on invoice_sequences to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));
--
alter table a_group enable row level security;
create policy a_group_access_policy on a_group to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));
--

update promo_code set organization_id_fk = (select org_id from event where event.id = event_id_fk) where organization_id_fk is null;
alter table promo_code alter column organization_id_fk set not null;

alter table promo_code enable row level security;
create policy promo_code_access_policy on promo_code to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access(organization_id_fk));
--

-- tables where event_id_fk is present


-- trigger for tables where the event fk is named event_id
create or replace function set_organization_id_fk_from_event_id() returns trigger
as $$ begin
    if new.organization_id_fk is null then
      new.organization_id_fk = (select org_id from event where event.id = new.event_id);
    end if;
    return new;
end;
$$ language plpgsql;

-- trigger for tables where the event fk is named event_id_fk
create or replace function set_organization_id_fk_from_event_id_fk() returns trigger
as $$ begin
    if new.organization_id_fk is null then
      new.organization_id_fk = (select org_id from event where event.id = new.event_id_fk);
    end if;
    return new;
end;
$$ language plpgsql;


-- ticket table
alter table ticket add column organization_id_fk integer;
alter table ticket add foreign key(organization_id_fk) references organization(id);
update ticket set organization_id_fk = (select org_id from event where event.id = event_id);
alter table ticket alter column organization_id_fk set not null;

create trigger ticket_insert_org_id_fk_trigger
    before insert on ticket
    for each row execute procedure set_organization_id_fk_from_event_id();

alter table ticket enable row level security;
create policy ticket_access_policy on ticket to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access((select org_id from event where event.id = event_id)));
--

-- tickets_reservation table
alter table tickets_reservation add column organization_id_fk integer;
alter table tickets_reservation add foreign key(organization_id_fk) references organization(id);
update tickets_reservation set organization_id_fk = (select org_id from event where event.id = event_id_fk);
alter table tickets_reservation alter column organization_id_fk set not null;

create trigger tickets_reservation_insert_org_id_fk_trigger
    before insert on tickets_reservation
    for each row execute procedure set_organization_id_fk_from_event_id_fk();

alter table tickets_reservation enable row level security;
create policy tickets_reservation_access_policy on tickets_reservation to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access((select org_id from event where event.id = event_id_fk)));
--

-- email_message
alter table email_message add column organization_id_fk integer;
alter table email_message add foreign key(organization_id_fk) references organization(id);
update email_message set organization_id_fk = (select org_id from event where event.id = event_id);
alter table email_message alter column organization_id_fk set not null;

create trigger email_message_insert_org_id_fk_trigger
    before insert on email_message
    for each row execute procedure set_organization_id_fk_from_event_id();

alter table email_message enable row level security;
create policy email_message_access_policy on email_message to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access((select org_id from event where event.id = event_id)));
--

-- admin_reservation_request
alter table admin_reservation_request add column organization_id_fk integer;
alter table admin_reservation_request add foreign key(organization_id_fk) references organization(id);
update admin_reservation_request set organization_id_fk = (select org_id from event where event.id = event_id);
alter table admin_reservation_request alter column organization_id_fk set not null;

create trigger admin_reservation_request_insert_org_id_fk_trigger
    before insert on admin_reservation_request
    for each row execute procedure set_organization_id_fk_from_event_id();

alter table admin_reservation_request enable row level security;
create policy admin_reservation_request_access_policy on admin_reservation_request to application_user
    using (alfio_check_row_access(organization_id_fk))
    with check (alfio_check_row_access((select org_id from event where event.id = event_id)));
--