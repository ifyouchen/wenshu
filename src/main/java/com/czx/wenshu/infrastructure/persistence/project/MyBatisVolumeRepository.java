package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.Volume;
import com.czx.wenshu.domain.project.VolumeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisVolumeRepository implements VolumeRepository {

    private final VolumeMapper volumeMapper;

    public MyBatisVolumeRepository(VolumeMapper volumeMapper) {
        this.volumeMapper = volumeMapper;
    }

    @Override
    public Volume save(Volume volume) {
        VolumeRecord record = toRecord(volume);
        if (volumeMapper.findById(volume.id().toString()) == null) {
            volumeMapper.insert(record);
        } else {
            volumeMapper.update(record);
        }
        return volume;
    }

    @Override
    public Optional<Volume> findById(UUID id) {
        return Optional.ofNullable(volumeMapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<Volume> findByProjectId(UUID projectId) {
        return volumeMapper.findByProjectId(projectId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        volumeMapper.deleteById(id.toString());
    }

    @Override
    public int countByProjectId(UUID projectId) {
        return volumeMapper.countByProjectId(projectId.toString());
    }

    private Volume toDomain(VolumeRecord r) {
        return Volume.rehydrate(UUID.fromString(r.getId()), UUID.fromString(r.getProjectId()), r.getTitle(),
                r.getConflict(), r.getSortOrder(), r.getCreatedAt());
    }

    private VolumeRecord toRecord(Volume v) {
        VolumeRecord r = new VolumeRecord();
        r.setId(v.id().toString());
        r.setProjectId(v.projectId().toString());
        r.setTitle(v.title());
        r.setConflict(v.conflict());
        r.setSortOrder(v.sortOrder());
        r.setCreatedAt(v.createdAt());
        return r;
    }
}