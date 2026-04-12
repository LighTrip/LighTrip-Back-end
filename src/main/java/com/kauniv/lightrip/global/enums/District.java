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

    // 경기도 (31개 시·군)
    SUWON("수원시"),
    SEONGNAM("성남시"),
    GOYANG("고양시"),
    YONGIN("용인시"),
    BUCHEON("부천시"),
    ANSAN("안산시"),
    ANYANG("안양시"),
    NAMYANGJU("남양주시"),
    HWASEONG("화성시"),
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