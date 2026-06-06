package com.example.zipfra.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * PostGIS DataSource (spatialDataSource) Bean 구성.
 *
 * AGENTS.md §3 멀티 데이터소스 규칙:
 * - @Primary 없음: primaryDataSource와 구분
 * - Mapper 스캔: com.example.zipfra.mapper.postgis.*
 * - TransactionManager: spatialTransactionManager (서비스 메서드당 단독 사용)
 * - 용도: 공간·집계 읽기 (ST_DWithin + ::geography 캐스팅, GiST 인덱스)
 *
 * AGENTS.md §4 좌표 단위 주의:
 * - 미터 반경 검색 시 ::geography 캐스팅 필수
 * - geometry && ST_MakeEnvelope(...) 은 bbox 필터에 사용 (Mapper XML에 명시)
 */
@Configuration
@MapperScan(
        basePackages = "com.example.zipfra.mapper.postgis",
        sqlSessionFactoryRef = "spatialSqlSessionFactory"
)
public class SpatialDataSourceConfig {

    @Bean(name = "spatialDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.spatial")
    public DataSource spatialDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "spatialSqlSessionFactory")
    public SqlSessionFactory spatialSqlSessionFactory(
            @Qualifier("spatialDataSource") DataSource dataSource) throws Exception {

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // Mapper XML 위치: resources/mapper/postgis/**/*.xml
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/postgis/**/*.xml")
        );

        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(config);

        return factoryBean.getObject();
    }

    @Bean(name = "spatialTransactionManager")
    public DataSourceTransactionManager spatialTransactionManager(
            @Qualifier("spatialDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "spatialSqlSessionTemplate")
    public SqlSessionTemplate spatialSqlSessionTemplate(
            @Qualifier("spatialSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
