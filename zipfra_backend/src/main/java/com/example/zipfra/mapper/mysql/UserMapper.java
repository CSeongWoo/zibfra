package com.example.zipfra.mapper.mysql;

import com.example.zipfra.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * MySQL users 테이블 MyBatis Mapper.
 * primarySqlSessionFactory(@MapperScan)가 이 패키지를 스캔한다.
 * §3: 공간 연산 금지 — 순수 CRUD 전용.
 */
@Mapper
public interface UserMapper {

    /**
     * 이메일로 사용자 단건 조회.
     * AUTH-02 로그인 비밀번호 검증에 사용.
     */
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * PK로 사용자 단건 조회.
     * USER-01 프로필 조회에 사용.
     */
    Optional<User> findById(@Param("id") Long id);

    /**
     * 회원 가입 INSERT.
     * AUTH-01에 사용. 삽입된 PK는 user.id에 자동 주입(useGeneratedKeys).
     */
    void insertUser(User user);
}
