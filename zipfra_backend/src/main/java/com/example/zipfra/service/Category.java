package com.example.zipfra.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * 입지 점수 카테고리 정의 (AGENTS.md §5).
 * 응답 breakdown 그룹 + 산출 모델을 함께 보유한다.
 *
 * <ul>
 *   <li>ESSENTIAL(pharmacy·mart·bank) = one_is_enough (가장 가까운 1개의 W만)</li>
 *   <li>LEISURE(restaurant·cafe·cinema) = more_is_better (반경 내 전부 ΣW)</li>
 *   <li>TRANSIT(subway·bus_stop) = 지하철 one_is_enough / 버스 more_is_better (전국 적용)</li>
 *   <li>ENV_PENALTY(noise·waste) = penalty (ΣW, contribution 음수, 서울 한정)</li>
 * </ul>
 */
public enum Category {

    PHARMACY("pharmacy", Group.ESSENTIAL, Model.ONE_IS_ENOUGH),
    MART("mart", Group.ESSENTIAL, Model.ONE_IS_ENOUGH),
    BANK("bank", Group.ESSENTIAL, Model.ONE_IS_ENOUGH),
    RESTAURANT("restaurant", Group.LEISURE, Model.MORE_IS_BETTER),
    CAFE("cafe", Group.LEISURE, Model.MORE_IS_BETTER),
    CINEMA("cinema", Group.LEISURE, Model.MORE_IS_BETTER),
    SUBWAY("subway", Group.TRANSIT, Model.ONE_IS_ENOUGH),
    BUS_STOP("bus_stop", Group.TRANSIT, Model.MORE_IS_BETTER),
    NOISE("noise", Group.ENV_PENALTY, Model.PENALTY),
    WASTE("waste", Group.ENV_PENALTY, Model.PENALTY);

    /** breakdown 그룹 (응답 키와 1:1). */
    public enum Group { ESSENTIAL, LEISURE, TRANSIT, ENV_PENALTY }

    /** 산출 모델. */
    public enum Model { ONE_IS_ENOUGH, MORE_IS_BETTER, PENALTY }

    private final String key;   // 요청 weights / 응답 JSON 키 (소문자)
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
