package com.heytrip.hotel.supplier.adapter.service.impl;

import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCityResponse;
import com.heytrip.common.response.other.XCountryResponse;
import com.heytrip.hotel.supplier.adapter.service.StaticDataQueryService;
import com.heytrip.hotel.supplier.constant.StaticCacheNames;
import com.heytrip.hotel.supplier.dto.basic.XHotelGiata;
import com.heytrip.hotel.supplier.dto.basic.XNationality;
import com.heytrip.hotel.supplier.entity.*;
import com.heytrip.hotel.supplier.repository.*;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 静态数据查询服务实现（仅先实现国家/城市/酒店分页查询，其他留空骨架）
 * 说明：
 * - 按 supplierId/supplierCode 做强隔离
 * - 返回 Heytrip 标准实体
 * - 启用分页，默认 page>=0, size 在 Controller 层做上限 100
 */
@Service
public class StaticDataQueryServiceImpl implements StaticDataQueryService {

    private static final Logger logger = LoggerFactory.getLogger(StaticDataQueryServiceImpl.class);

    @Resource private CountryRepository countryRepo;
    @Resource private CityRepository cityRepo;
    @Resource private HotelRepository hotelRepo;
    @Resource private NationalityRepository nationalityRepo;
    @Resource private HotelGiataRepository giataRepo;
    @Resource private SyncLogRepository syncLogRepository;
    @Resource private RoomRepository roomRepo;
    @Resource private RatePlanRepository ratePlanRepo;

    @Override
    @Cacheable(cacheNames = StaticCacheNames.COUNTRY, key = "#supplierId + ':' + #supplierCode + ':' + #countryCode + ':' + #countryName + ':' + #page + ':' + #size")
    public Page<XCountryResponse> pageCountries(Long supplierId, String supplierCode, String countryCode, String countryName, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<Country> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.like(root.get("countryCode"), "%" + countryCode + "%"));
            }
            if (countryName != null && !countryName.isEmpty()) {
                ps.add(cb.like(root.get("countryName"), "%" + countryName + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Country> pageData = countryRepo.findAll(spec, pageable);
        List<XCountryResponse> content = pageData.getContent().stream().map(this::toXCountry).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    @Override
    public Page<SyncLog> pageStaticSyncLogs(Long supplierId, String supplierCode, String businessType, Boolean success, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<SyncLog> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (businessType != null && !businessType.isEmpty()) {
                ps.add(cb.equal(root.get("businessType"), businessType));
            }
            if (success != null) {
                ps.add(cb.equal(root.get("isSuccess"), success));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return syncLogRepository.findAll(spec, pageable);
    }

    @Override
    @Cacheable(cacheNames = StaticCacheNames.CITY, key = "#supplierId + ':' + #supplierCode + ':' + #cityCode + ':' + #countryCode + ':' + #name + ':' + #page + ':' + #size")
    public Page<XCityResponse> pageCities(Long supplierId, String supplierCode, String cityCode, String countryCode, String name, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<City> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (cityCode != null && !cityCode.isEmpty()) {
                ps.add(cb.like(root.get("cityCode"), "%" + cityCode + "%"));
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.equal(root.get("countryCode"), countryCode));
            }
            if (name != null && !name.isEmpty()) {
                ps.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<City> pageData = cityRepo.findAll(spec, pageable);
        List<XCityResponse> content = pageData.getContent().stream().map(this::toXCity).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    @Override
    @Cacheable(cacheNames = StaticCacheNames.HOTEL, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #cityCode + ':' + #countryCode + ':' + #name + ':' + #page + ':' + #size")
    public Page<XHotel> pageHotels(Long supplierId, String supplierCode, String hotelCode, String cityCode, String countryCode, String name, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<Hotel> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (hotelCode != null && !hotelCode.isEmpty()) {
                ps.add(cb.like(root.get("hotelCode"), "%" + hotelCode + "%"));
            }
            if (cityCode != null && !cityCode.isEmpty()) {
                ps.add(cb.equal(root.get("cityCode"), cityCode));
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.equal(root.get("countryCode"), countryCode));
            }
            if (name != null && !name.isEmpty()) {
                ps.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Hotel> pageData = hotelRepo.findAll(spec, pageable);
        List<XHotel> content = pageData.getContent().stream().map(this::toXHotel).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    @Override
    public Page<XRoom> pageRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode, String name, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<Room> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (hotelCode != null && !hotelCode.isEmpty()) {
                ps.add(cb.equal(root.get("hotelCode"), hotelCode));
            }
            if (roomCode != null && !roomCode.isEmpty()) {
                ps.add(cb.like(root.get("roomCode"), "%" + roomCode + "%"));
            }
            if (name != null && !name.isEmpty()) {
                ps.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        // 进行数据库分页查询
        Page<Room> pageData = roomRepo.findAll(spec, pageable);
        // 暂返回空列表（DTO 字段需确认），但返回正确 total 与分页信息
        return new PageImpl<>(java.util.Collections.emptyList(), pageable, pageData.getTotalElements());
    }

    @Override
    public Page<XRatePlan> pageRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode, String name, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<RatePlan> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (hotelCode != null && !hotelCode.isEmpty()) {
                ps.add(cb.equal(root.get("hotelCode"), hotelCode));
            }
            if (roomCode != null && !roomCode.isEmpty()) {
                ps.add(cb.equal(root.get("roomCode"), roomCode));
            }
            if (ratePlanCode != null && !ratePlanCode.isEmpty()) {
                ps.add(cb.like(root.get("ratePlanCode"), "%" + ratePlanCode + "%"));
            }
            if (name != null && !name.isEmpty()) {
                ps.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<RatePlan> pageData = ratePlanRepo.findAll(spec, pageable);
        return new PageImpl<>(java.util.Collections.emptyList(), pageable, pageData.getTotalElements());
    }

    @Override
    @Cacheable(cacheNames = StaticCacheNames.NATIONALITY, key = "#supplierId + ':' + #supplierCode + ':' + #nationalityCode + ':' + #nationality + ':' + #isoCode + ':' + #page + ':' + #size")
    public Page<XNationality> pageNationalities(Long supplierId, String supplierCode, String nationalityCode, String nationality, String isoCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<Nationality> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (nationalityCode != null && !nationalityCode.isEmpty()) {
                ps.add(cb.like(root.get("nationalityCode"), "%" + nationalityCode + "%"));
            }
            if (nationality != null && !nationality.isEmpty()) {
                ps.add(cb.like(root.get("nationality"), "%" + nationality + "%"));
            }
            if (isoCode != null && !isoCode.isEmpty()) {
                ps.add(cb.equal(cb.lower(root.get("isoCode")), isoCode.toLowerCase()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Nationality> pageData = nationalityRepo.findAll(spec, pageable);
        List<XNationality> content = pageData.getContent().stream().map(this::toXNationality).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    @Override
    @Cacheable(cacheNames = StaticCacheNames.GIATA, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #giataId + ':' + #page + ':' + #size")
    public Page<XHotelGiata> pageGiataMappings(Long supplierId, String supplierCode, String hotelCode, String giataId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<HotelGiata> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (hotelCode != null && !hotelCode.isEmpty()) {
                ps.add(cb.like(root.get("hotelCode"), "%" + hotelCode + "%"));
            }
            if (giataId != null && !giataId.isEmpty()) {
                ps.add(cb.like(root.get("giataId"), "%" + giataId + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<HotelGiata> pageData = giataRepo.findAll(spec, pageable);
        List<XHotelGiata> content = pageData.getContent().stream().map(this::toXGiata).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    // ================== 映射方法 ==================

    private XCountryResponse toXCountry(Country e) {
        XCountryResponse x = new XCountryResponse();
        x.setId(e.getCountryCode());
        x.setNameEn(e.getCountryName());
        return x;
    }

    private XCityResponse toXCity(City e) {
        XCityResponse x = new XCityResponse();
        x.setNameEn(e.getName());
        x.setId(e.getCityCode());
        x.setExt(e.getCountryCode());
        return x;
    }

    private XHotel toXHotel(Hotel e) {
        XHotel x = new XHotel();
        x.setHotelId(e.getHotelCode());
        x.setHotelName(e.getName());
        x.setCity(e.getCityCode());
        x.setCountryCode(e.getCountryCode());
        x.setHeroImg(e.getMainImage());
        x.setDescription(e.getLongDesc());
        x.setLatitude(String.valueOf(e.getLatitude()));
        x.setLongitude(String.valueOf(e.getLongitude()));
        x.setRating(BigDecimal.valueOf(e.getRating()));
        x.setAddress(e.getAddress());
        x.setPhone(e.getPhone());
        return x;
    }

    private XNationality toXNationality(Nationality e) {
        XNationality x = new XNationality();
        x.setNationalityCode(e.getNationalityCode());
        x.setNationality(e.getNationality());
        x.setIsoCode(e.getIsoCode());
        return x;
    }

    private XHotelGiata toXGiata(HotelGiata e) {
        XHotelGiata x = new XHotelGiata();
        x.setHotelCode(e.getHotelCode());
        x.setGiataId(e.getGiataId());
        x.setName(e.getName());
        x.setCityCode(e.getCityCode());
        x.setCityName(e.getCityName());
        x.setCountryCode(e.getCountryCode());
        x.setLongDesc(e.getLongDesc());
        x.setLatitude(e.getLatitude());
        x.setLongitude(e.getLongitude());
        x.setRating(e.getRating());
        x.setAddress(e.getAddress());
        x.setMainImage(e.getMainImage());
        return x;
    }
}
