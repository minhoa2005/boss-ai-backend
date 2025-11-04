package ai.content.auto.mapper;

import ai.content.auto.dtos.WorkspaceDto;
import ai.content.auto.entity.Workspace;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

    WorkspaceMapper INSTANCE = Mappers.getMapper(WorkspaceMapper.class);

    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "owner.username", target = "ownerUsername")
    @Mapping(source = "owner.email", target = "ownerEmail")
    @Mapping(source = ".", target = "memberUsagePercent", qualifiedByName = "calculateMemberUsagePercent")
    @Mapping(source = ".", target = "contentUsagePercent", qualifiedByName = "calculateContentUsagePercent")
    @Mapping(source = ".", target = "storageUsagePercent", qualifiedByName = "calculateStorageUsagePercent")
    @Mapping(source = ".", target = "apiUsagePercent", qualifiedByName = "calculateApiUsagePercent")
    @Mapping(source = ".", target = "approachingMemberLimit", qualifiedByName = "isApproachingMemberLimit")
    @Mapping(source = ".", target = "approachingContentLimit", qualifiedByName = "isApproachingContentLimit")
    @Mapping(source = ".", target = "approachingStorageLimit", qualifiedByName = "isApproachingStorageLimit")
    @Mapping(source = ".", target = "approachingApiLimit", qualifiedByName = "isApproachingApiLimit")
    @Mapping(source = "createdAt", target = "daysSinceCreation", qualifiedByName = "calculateDaysSinceCreation")
    WorkspaceDto toDto(Workspace workspace);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Workspace toEntity(WorkspaceDto workspaceDto);

    @Named("calculateMemberUsagePercent")
    default Double calculateMemberUsagePercent(Workspace workspace) {
        if (workspace.getMemberLimit() == null || workspace.getMemberLimit() == 0) {
            return 0.0;
        }
        return Math.round((workspace.getCurrentMemberCount().doubleValue() / workspace.getMemberLimit()) * 100 * 100.0)
                / 100.0;
    }

    @Named("calculateContentUsagePercent")
    default Double calculateContentUsagePercent(Workspace workspace) {
        if (workspace.getContentLimit() == null || workspace.getContentLimit() == 0) {
            return 0.0;
        }
        return Math.round(
                (workspace.getCurrentContentCount().doubleValue() / workspace.getContentLimit()) * 100 * 100.0) / 100.0;
    }

    @Named("calculateStorageUsagePercent")
    default Double calculateStorageUsagePercent(Workspace workspace) {
        if (workspace.getStorageLimitMb() == null || workspace.getStorageLimitMb() == 0) {
            return 0.0;
        }
        return Math.round(
                (workspace.getCurrentStorageUsedMb().doubleValue() / workspace.getStorageLimitMb()) * 100 * 100.0)
                / 100.0;
    }

    @Named("calculateApiUsagePercent")
    default Double calculateApiUsagePercent(Workspace workspace) {
        if (workspace.getApiCallsLimit() == null || workspace.getApiCallsLimit() == 0) {
            return 0.0;
        }
        return Math
                .round((workspace.getCurrentApiCallsUsed().doubleValue() / workspace.getApiCallsLimit()) * 100 * 100.0)
                / 100.0;
    }

    @Named("isApproachingMemberLimit")
    default Boolean isApproachingMemberLimit(Workspace workspace) {
        if (workspace.getMemberLimit() == null || workspace.getMemberLimit() == 0) {
            return false;
        }
        return workspace.getCurrentMemberCount() >= workspace.getMemberLimit() * 0.9;
    }

    @Named("isApproachingContentLimit")
    default Boolean isApproachingContentLimit(Workspace workspace) {
        if (workspace.getContentLimit() == null || workspace.getContentLimit() == 0) {
            return false;
        }
        return workspace.getCurrentContentCount() >= workspace.getContentLimit() * 0.9;
    }

    @Named("isApproachingStorageLimit")
    default Boolean isApproachingStorageLimit(Workspace workspace) {
        if (workspace.getStorageLimitMb() == null || workspace.getStorageLimitMb() == 0) {
            return false;
        }
        return workspace.getCurrentStorageUsedMb() >= workspace.getStorageLimitMb() * 0.9;
    }

    @Named("isApproachingApiLimit")
    default Boolean isApproachingApiLimit(Workspace workspace) {
        if (workspace.getApiCallsLimit() == null || workspace.getApiCallsLimit() == 0) {
            return false;
        }
        return workspace.getCurrentApiCallsUsed() >= workspace.getApiCallsLimit() * 0.9;
    }

    @Named("calculateDaysSinceCreation")
    default Long calculateDaysSinceCreation(Instant createdAt) {
        if (createdAt == null) {
            return 0L;
        }
        return (Instant.now().getEpochSecond() - createdAt.getEpochSecond()) / 86400;
    }
}