package com.rus.rus.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rus.rus.domain.UserSetting;

public interface UserSettingRepository extends JpaRepository<UserSetting, String>{

}
