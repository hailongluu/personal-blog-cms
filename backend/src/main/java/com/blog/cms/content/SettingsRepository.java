package com.blog.cms.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingsRepository extends JpaRepository<Setting, String> {

    List<Setting> findByIsPublicTrue();
}
