package com.kauniv.lightrip.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum District {

    // 서울특별시 (25개 구)
    JONGNO("종로구"),
    JUNG("중구"),
    YONGSAN("용산구"),
    SEONGDONG("성동구"),
    GWANGJIN("광진구"),
    DONGDAEMUN("동대문구"),
    JUNGNANG("중랑구"),
    SEONGBUK("성북구"),
    GANGBUK("강북구"),
    DOBONG("도봉구"),
    NOWON("노원구"),
    EUNPYEONG("은평구"),
    SEODAEMUN("서대문구"),
    MAPO("마포구"),
    YANGCHEON("양천구"),
    GANGSEO("강서구"),
    GURO("구로구"),
    GEUMCHEON("금천구"),
    YEONGDEUNGPO("영등포구"),
    DONGJAK("동작구"),
    GWANAK("관악구"),
    SEOCHO("서초구"),
    GANGNAM("강남구"),
    SONGPA("송파구"),
    GANGDONG("강동구"),

    // 경기도 — 구가 있는 시는 구 단위
    // 수원시
    SUWON_JANGAN("수원 장안구"),
    SUWON_GWONSEON("수원 권선구"),
    SUWON_PALDAL("수원 팔달구"),
    SUWON_YEONGTONG("수원 영통구"),
    // 성남시
    SEONGNAM_SUJEONG("성남 수정구"),
    SEONGNAM_JUNGWON("성남 중원구"),
    SEONGNAM_BUNDANG("성남 분당구"),
    // 고양시
    GOYANG_DEOKYANG("고양 덕양구"),
    GOYANG_ILSANDONG("고양 일산동구"),
    GOYANG_ILSANSEO("고양 일산서구"),
    // 용인시
    YONGIN_CHEOIN("용인 처인구"),
    YONGIN_GIHEUNG("용인 기흥구"),
    YONGIN_SUJI("용인 수지구"),
    // 부천시
    BUCHEON_WONMI("부천 원미구"),
    BUCHEON_SOSA("부천 소사구"),
    BUCHEON_OJEONG("부천 오정구"),
    // 안산시
    ANSAN_SANGNOK("안산 상록구"),
    ANSAN_DANWON("안산 단원구"),
    // 안양시
    ANYANG_MANAN("안양 만안구"),
    ANYANG_DONGAN("안양 동안구"),
    // 화성시 (2026.2 구 신설)
    HWASEONG_MANSE("화성 만세구"),
    HWASEONG_HYOHAENG("화성 효행구"),
    HWASEONG_BYEONGJEOM("화성 병점구"),
    HWASEONG_DONGTAN("화성 동탄구"),

    // 경기도 — 구가 없는 시·군
    NAMYANGJU("남양주시"),
    PYEONGTAEK("평택시"),
    UIJEONGBU("의정부시"),
    SIHEUNG("시흥시"),
    PAJU("파주시"),
    GIMPO("김포시"),
    GWANGMYEONG("광명시"),
    GWANGJU_GG("광주시"),
    GUNPO("군포시"),
    HANAM("하남시"),
    OSAN("오산시"),
    ICHEON("이천시"),
    YANGJU("양주시"),
    ANSEONG("안성시"),
    GURI("구리시"),
    POCHEON("포천시"),
    UIWANG("의왕시"),
    YEOJU("여주시"),
    DONGDUCHEON("동두천시"),
    GAPYEONG("가평군"),
    YANGPYEONG("양평군"),
    YEONCHEON("연천군"),
    GWACHEON("과천시");

    private final String displayName;
}