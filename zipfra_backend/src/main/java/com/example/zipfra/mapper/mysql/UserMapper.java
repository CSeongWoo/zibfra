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

    /**
     * 회원 정보 수정
     */
    void updateUser(User user);

    /**
     * 회원 계정 비활성화 (Soft delete)
     */
    void deactivateUser(@Param("id") Long id);

    /**
     * 비밀번호 찾기 시 이메일로 비밀번호 업데이트
     */
    void updatePasswordByEmail(@Param("email") String email, @Param("password") String password);

    /**
     * 닉네임으로 유저 검색 (Like 검색)
     */
    java.util.List<User> searchByNickname(@Param("nickname") String nickname);
}
