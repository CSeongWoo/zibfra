package com.example.zipfra.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * 입지 점수 카테고리 정의 (AGENTS.md §5, 2026-06-13 4분류 재편).
 * 4그룹(교통/교육/상업/편의) + 카테고리별 산출 모델을 보유한다.
 * <b>가중치는 그룹 단위</b>로 적용한다(와이어프레임 페르소나 슬라이더 4개와 1:1).
 *
 * <ul>
 *   <li>TRANSIT(subway·bus_stop) = 지하철 one_is_enough / 버스 more_is_better</li>
 *   <li>EDUCATION(school·academy) = 학교 one_is_enough / 학원 more_is_better</li>
 *   <li>COMMERCE(restaurant·cafe·cinema·mart) = 전부 more_is_better</li>
 *   <li>CONVENIENCE(convenience_store·hospital·pharmacy·bank) = 전부 more_is_better</li>
 * </ul>
 * 전국 적용(서울 제한 없음). ENV_PENALTY·서울 한정 로직은 폐기됨.
 */
public enum Category {

    SUBWAY("subway", Group.TRANSIT, Model.ONE_IS_ENOUGH),
    BUS_STOP("bus_stop", Group.TRANSIT, Model.MORE_IS_BETTER),
    SCHOOL("school", Group.EDUCATION, Model.ONE_IS_ENOUGH),
    ACADEMY("academy", Group.EDUCATION, Model.MORE_IS_BETTER),
    RESTAURANT("restaurant", Group.COMMERCE, Model.MORE_IS_BETTER),
    CAFE("cafe", Group.COMMERCE, Model.MORE_IS_BETTER),
    CINEMA("cinema", Group.COMMERCE, Model.MORE_IS_BETTER),
    MART("mart", Group.COMMERCE, Model.MORE_IS_BETTER),
    CONVENIENCE_STORE("convenience_store", Group.CONVENIENCE, Model.MORE_IS_BETTER),
    HOSPITAL("hospital", Group.CONVENIENCE, Model.MORE_IS_BETTER),
    PHARMACY("pharmacy", Group.CONVENIENCE, Model.MORE_IS_BETTER),
    BANK("bank", Group.CONVENIENCE, Model.MORE_IS_BETTER);

    /** breakdown 그룹 (응답 키 + 가중치 키와 1:1). 가중치는 이 그룹 단위로 적용한다. */
    public enum Group {
        TRANSIT("transit"),
        EDUCATION("education"),
        COMMERCE("commerce"),
        CONVENIENCE("convenience");

        private final String key;

        Group(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    /** 산출 모델. */
    public enum Model { ONE_IS_ENOUGH, MORE_IS_BETTER }

    private final String key;   // 응답 JSON 카테고리 키 (소문자)
    private final Group group;
    private final Model model;

    Category(String key, Group group, Model model) {
        this.key = key;
        this.group = group;
        this.model = model;
    }

    public String key() {
        return key;
    }

    public Group group() {
        return group;
    }

    public Model model() {
        return model;
    }

    /** PostGIS poi.category(대문자) → enum 매핑. 미일치 시 empty. */
    public static Optional<Category> fromDbCategory(String dbCategory) {
        if (dbCategory == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(dbCategory))
                .findFirst();
    }
}
