package com.taipei.iot.announcement.repository;

import com.taipei.iot.announcement.entity.AnnouncementAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementAttachmentRepository extends JpaRepository<AnnouncementAttachment, Long> {

    List<AnnouncementAttachment> findByAnnouncementIdOrderByIdAsc(Long announcementId);

    List<AnnouncementAttachment> findByAnnouncementIdInOrderByIdAsc(List<Long> announcementIds);
}
