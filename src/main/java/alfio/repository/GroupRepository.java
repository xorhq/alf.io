/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.repository;

import alfio.model.group.Group;
import alfio.model.group.GroupMember;
import alfio.model.group.LinkedGroup;
import ch.digitalfondue.npjt.*;

import java.util.List;
import java.util.Optional;

@QueryRepository
public interface GroupRepository {

    String BY_EVENT_ID = "select * from group_link_active where event_id_fk = :eventId";

    @Query("insert into a_group(name, description, organization_id_fk) values(:name, :description, :orgId)")
    @AutoGeneratedKey("id")
    AffectedRowCountAndKey<Integer> insert(@Bind("name") String name,
                                           @Bind("description") String description,
                                           @Bind("orgId") int organizationId);

    @Query("select * from group_active where id = :id")
    Group getById(@Bind("id") int id);

    @Query("update a_group set name = :name, description = :description where id = :id")
    int update(@Bind("id") int id, @Bind("name") String name, @Bind("description") String description);

    @Query("select * from group_active where id = :id")
    Optional<Group> getOptionalById(@Bind("id") int id);

    @Query("select * from group_active where organization_id_fk = :organizationId")
    List<Group> getAllForOrganization(@Bind("organizationId") int organizationId);

    @Query("insert into group_link(a_group_id_fk, event_id_fk, ticket_category_id_fk, type, match_type, max_allocation)" +
        " values(:groupId, :eventId, :ticketCategoryId, :type, :matchType, :maxAllocation)")
    @AutoGeneratedKey("id")
    AffectedRowCountAndKey<Integer> createConfiguration(@Bind("groupId") int groupId,
                                                        @Bind("eventId") int eventId,
                                                        @Bind("ticketCategoryId") Integer ticketCategoryId,
                                                        @Bind("type") LinkedGroup.Type type,
                                                        @Bind("matchType") LinkedGroup.MatchType matchType,
                                                        @Bind("maxAllocation") Integer maxAllocation);

    @Query(type = QueryType.TEMPLATE,
        value = "insert into group_member(a_group_id_fk, value, description) values(:groupId, :value, :description)")
    String insertItemTemplate();

    @Query("select * from group_member_active where a_group_id_fk = :groupId order by value")
    List<GroupMember> getItems(@Bind("groupId") int groupId);

    @Query("insert into whitelisted_ticket(group_member_id_fk, group_link_id_fk, ticket_id_fk, requires_unique_value)" +
        " values(:itemId, :configurationId, :ticketId, :requiresUniqueValue)")
    int insertWhitelistedTicket(@Bind("itemId") int itemId, @Bind("configurationId") int configurationId, @Bind("ticketId") int ticketId, @Bind("requiresUniqueValue") Boolean requiresUniqueValue);

    @Query(BY_EVENT_ID +
        " and ticket_category_id_fk = :categoryId" +
        " union all select * from group_link_active where event_id_fk = :eventId and ticket_category_id_fk is null")
    List<LinkedGroup> findActiveConfigurationsFor(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId);

    @Query(BY_EVENT_ID)
    List<LinkedGroup> findActiveConfigurationsForEvent(@Bind("eventId") int eventId);

    @Query("select count(*) from group_link_active where event_id_fk = :eventId")
    Integer countByEventId(@Bind("eventId") int eventId);

    @Query("select * from group_link_active where id = :configurationId")
    LinkedGroup getConfiguration(@Bind("configurationId") int configurationId);

    @Query("select * from group_link where id = :configurationId for update")
    LinkedGroup getConfigurationForUpdate(@Bind("configurationId") int configurationId);

    @Query("select count(*) from whitelisted_ticket where group_link_id_fk = :configurationId")
    int countWhitelistedTicketsForConfiguration(@Bind("configurationId") int configurationId);

    @Query("update group_link set a_group_id_fk = :groupId, event_id_fk = :eventId, ticket_category_id_fk = :categoryId, type = :type, match_type = :matchType, max_allocation = :maxAllocation where id = :id")
    int updateConfiguration(@Bind("id") int configurationId,
                            @Bind("groupId") int groupId,
                            @Bind("eventId") int eventId,
                            @Bind("categoryId") Integer categoryId,
                            @Bind("type") LinkedGroup.Type type,
                            @Bind("matchType") LinkedGroup.MatchType matchType,
                            @Bind("maxAllocation") Integer maxAllocation);

    @Query("update group_link set active = false where id = :id")
    int disableLink(@Bind("id") int id);

    @Query("update group_link set active = false where a_group_id_fk = :groupId")
    int disableAllLinks(@Bind("groupId") int groupId);

    @Query("select * from group_member_active wi where wi.a_group_id_fk = :groupId and lower(wi.value) = lower(:value)")
    Optional<GroupMember> findItemByValueExactMatch(@Bind("groupId") int groupId, @Bind("value") String value);

    @Query("select * from group_member_active wi where wi.a_group_id_fk = :groupId and lower(wi.value) like lower(:value) limit 1")
    Optional<GroupMember> findItemEndsWith(@Bind("configurationId") int configurationId,
                                           @Bind("groupId") int groupId,
                                           @Bind("value") String value);

    @Query("select count(*) from whitelisted_ticket where group_member_id_fk = :itemId and group_link_id_fk = :configurationId")
    int countExistingWhitelistedTickets(@Bind("itemId") int itemId,
                                        @Bind("configurationId") int configurationId);

    @Query("delete from whitelisted_ticket where ticket_id_fk in (:ticketIds)")
    int deleteExistingWhitelistedTickets(@Bind("ticketIds") List<Integer> ticketIds);

    @Query(type = QueryType.TEMPLATE, value = "update group_member set active = false, value = 'DISABLED-' || :disabledPlaceholder where id = :memberId and a_group_id_fk = :groupId")
    String deactivateGroupMember();


    @Query("update a_group set active = false where id = :groupId")
    int deactivateGroup(@Bind("groupId") int groupId);
}
