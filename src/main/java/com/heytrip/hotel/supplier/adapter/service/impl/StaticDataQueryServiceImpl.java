package com.heytrip.hotel.supplier.adapter.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCityResponse;
import com.heytrip.common.response.other.XCountryResponse;
import com.heytrip.hotel.supplier.adapter.service.StaticDataQueryService;
import com.heytrip.hotel.supplier.constant.CacheNames;
import com.heytrip.hotel.supplier.dto.basic.XHotelGiata;
import com.heytrip.hotel.supplier.dto.basic.XNationality;
import com.heytrip.hotel.supplier.entity.primary.SyncLog;
import com.heytrip.hotel.supplier.entity.supplier.*;
import com.heytrip.hotel.supplier.repository.primary.SyncLogRepository;
import com.heytrip.hotel.supplier.repository.supplier.*;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 静态数据查询服务实现
 * 说明：
 * - 按 supplierId/supplierCode 做强隔离
 * - 返回 Heytrip 标准实体
 * - 启用分页，默认 page>=0, size 在 Controller 层做上限 100
 */
@Service
@DS("aos")  // 默认使用 aos 数据源查询 supplier 数据
public class StaticDataQueryServiceImpl implements StaticDataQueryService {

    private static final Logger logger = LoggerFactory.getLogger(StaticDataQueryServiceImpl.class);

    @Resource
    private CountryRepository countryRepo;
    @Resource
    private CityRepository cityRepo;
    @Resource
    private HotelRepository hotelRepo;
    @Resource
    private NationalityRepository nationalityRepo;
    @Resource
    private HotelGiataRepository giataRepo;
    @Resource
    private SyncLogRepository syncLogRepository;
    @Resource
    private RoomRepository roomRepo;
    @Resource
    private RatePlanRepository ratePlanRepo;


    /**
     * 国家分页查询
     *
     * @param supplierId
     * @param supplierCode
     * @param countryCode
     * @param page
     * @param size
     * @return
     */
    @Override
    @Cacheable(cacheNames = CacheNames.COUNTRY, key = "#supplierId + ':' + #supplierCode + ':' + #countryCode +':' + #page + ':' + #size")
    public Page<XCountryResponse> pageCountries(Long supplierId, String supplierCode, String countryCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<Country> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.like(root.get("countryCode"), "%" + countryCode + "%"));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Country> pageData = countryRepo.findAll(spec, pageable);
        List<XCountryResponse> content = pageData.getContent().stream().map(this::toXCountry).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }
    @Override
    @Cacheable(cacheNames = CacheNames.COUNTRY, key = "#supplierId + ':' + #supplierCode + ':'  + #page + ':' + #size")
    public Page<XCountryResponse> pageCountries(Long supplierId, String supplierCode, int page, int size) {
        return pageCountries(supplierId, supplierCode, null, page, size);
    }


    /**
     * 静态数据同步日志分页查询
     *
     * @param supplierId   供应商ID
     * @param supplierCode 供应商代码
     * @param businessType 业务类型（countries/cities/hotels/nationality/giata/all）可选
     * @param success      是否成功 可选
     * @param page
     * @param size
     * @return
     */
    @Override
    @DS("primary")  // SyncLog 在 primary 数据源
    public Page<SyncLog> pageSyncLogs(Long supplierId, String supplierCode, String businessType, Boolean success, int page, int size) {
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



    /**
     * 城市分页查询
     *
     * @param supplierId
     * @param supplierCode
     * @param cityCode
     * @param countryCode
     * @param page
     * @param size
     * @return
     */
    @Override
    @Cacheable(cacheNames = CacheNames.CITY, key = "#supplierId + ':' + #supplierCode + ':' + #cityCode + ':' + #countryCode + ':' + #page + ':' + #size")
    public Page<XCityResponse> pageCities(Long supplierId, String supplierCode, String cityCode, String countryCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<City> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (cityCode != null && !cityCode.isEmpty()) {
                ps.add(cb.equal(root.get("cityCode"), cityCode));
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.equal(root.get("countryCode"), countryCode));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<City> pageData = cityRepo.findAll(spec, pageable);
        List<XCityResponse> content = pageData.getContent().stream().map(this::toXCity).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CITY, key = "#supplierId + ':' + #supplierCode + ':'  + #page + ':' + #size")
    public  Page<XCityResponse> pageCities(Long supplierId, String supplierCode, int page, int size) {
        return pageCities(supplierId, supplierCode, null, null, page, size);
    }

    /**
     * 国籍分页查询
     *
     * @param supplierId
     * @param supplierCode
     * @param nationalityCode
     * @param isoCode
     * @param page
     * @param size
     * @return
     */
    @Override
    @Cacheable(cacheNames = CacheNames.NATIONALITY, key = "#supplierId + ':' + #supplierCode + ':' + #nationalityCode + ':'  + #isoCode + ':' + #page + ':' + #size")
    public Page<XNationality> pageNationalities(Long supplierId, String supplierCode, String nationalityCode, String isoCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<Nationality> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (nationalityCode != null && !nationalityCode.isEmpty()) {
                ps.add(cb.like(root.get("nationalityCode"), "%" + nationalityCode + "%"));
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
    @Cacheable(cacheNames = CacheNames.NATIONALITY , key = "#supplierId + ':' + #supplierCode + ':'  + #page + ':' + #size")
    public Page<XNationality> pageNationalities(Long supplierId, String supplierCode, int page, int size) {
        return pageNationalities(supplierId, supplierCode, null, null, page, size);
    }


    /**
     * 酒店分页查询
     *
     * @param supplierId
     * @param supplierCode
     * @param cityCode
     * @param countryCode
     * @param page
     * @param size
     * @return
     */
    @Override
    @Cacheable(cacheNames = CacheNames.HOTEL, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #cityCode + ':' + #countryCode + ':' + #page + ':' + #size")
    public Page<XHotel> pageHotels(Long supplierId, String supplierCode, String cityCode, String countryCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Specification<Hotel> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));

            if (cityCode != null && !cityCode.isEmpty()) {
                ps.add(cb.equal(root.get("cityCode"), cityCode));
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.equal(root.get("countryCode"), countryCode));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Hotel> pageData = hotelRepo.findAll(spec, pageable);
        List<XHotel> content = pageData.getContent().stream().map(this::toXHotel).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.HOTEL, key = "#supplierId + ':' + #supplierCode + ':'  + #page + ':' + #size")
    public Page<XHotel> pageHotels(Long supplierId, String supplierCode, int page, int size) {
        return pageHotels(supplierId, supplierCode, null, null, page, size);
    }

    /**
     * 房型分页查询
     *
     * @param supplierId
     * @param supplierCode
     * @param hotelCode
     * @param roomCode
     * @param page
     * @param size
     * @return
     */
    @Override
    @Cacheable(cacheNames = CacheNames.ROOM, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #roomCode + ':' + #page + ':' + #size")
    public Page<XRoom> pageRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        Specification<Room> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (hotelCode != null && !hotelCode.isEmpty()) {
                ps.add(cb.equal(root.get("hotelCode"), hotelCode));
            }
            if (roomCode != null && !roomCode.isEmpty()) {
                ps.add(cb.like(root.get("roomCodeMd5"), "%" + roomCode + "%"));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        // 进行数据库分页查询
        Page<Room> pageData = roomRepo.findAll(spec, pageable);
        // 转换为XRatePlan列表
        List<XRoom> content = pageData.getContent().stream().map(this::toXRoom).collect(Collectors.toList());
        // 暂返回空列表（DTO 字段需确认），但返回正确 total 与分页信息
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROOM, key = "#supplierId + ':' + #supplierCode + ':'  + #page + ':' + #size")
    public Page<XRoom> pageRooms(Long supplierId, String supplierCode, int page, int size) {
        return pageRooms(supplierId, supplierCode, null, null, page, size);
    }

    /**
     * 价计划分页查询
     *
     * @param supplierId
     * @param supplierCode
     * @param hotelCode
     * @param roomCode
     * @param ratePlanCode
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<XRatePlan> pageRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode, int page, int size) {
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
                ps.add(cb.like(root.get("ratePlanCodeMd5"), "%" + ratePlanCode + "%"));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<RatePlan> pageData = ratePlanRepo.findAll(spec, pageable);
        // 转换为XRatePlan列表
        List<XRatePlan> content = pageData.getContent().stream().map(this::toXRatePlan).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageData.getTotalElements());
    }


    /**
     * Giata 映射分页查询
     *
     * @param supplierId
     * @param supplierCode
     * @param hotelCode
     * @param giataId
     * @param page
     * @param size
     * @return
     */
    @Override
    @Cacheable(cacheNames = CacheNames.GIATA, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #giataId + ':' + #page + ':' + #size")
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

    @Override
    @Cacheable(cacheNames = CacheNames.GIATA, key = "#supplierId + ':' + #supplierCode + ':'  + #page + ':' + #size")
    public Page<XHotelGiata> pageGiataMappings(Long supplierId, String supplierCode, int page, int size) {
        return pageGiataMappings(supplierId, supplierCode, null, null, page, size);
    }


    // ================= 不分页查询（list*） =================
    @Override
    @Cacheable(cacheNames = CacheNames.COUNTRY, key = "#supplierId + ':' + #supplierCode + ':' + #countryCode")
    public List<XCountryResponse> listCountries(Long supplierId, String supplierCode, String countryCode) {
        Specification<Country> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.like(root.get("countryCode"), "%" + countryCode + "%"));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        return countryRepo.findAll(spec, PageRequest.of(0, 100)).getContent().stream().map(this::toXCountry).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CITY, key = "#supplierId + ':' + #supplierCode + ':' + #cityCode + ':' + #countryCode")
    public List<XCityResponse> listCities(Long supplierId, String supplierCode, String cityCode, String countryCode) {
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

            return cb.and(ps.toArray(new Predicate[0]));
        };
        return cityRepo.findAll(spec, PageRequest.of(0, 100)).getContent().stream().map(this::toXCity).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.HOTEL, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #cityCode + ':' + #countryCode")
    public List<XHotel> listHotels(Long supplierId, String supplierCode, String hotelCode, String cityCode, String countryCode) {
        Specification<Hotel> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (hotelCode != null && !hotelCode.isEmpty()) {
                ps.add(cb.like(root.get("hotelCodeMd5"), "%" + hotelCode + "%"));
            }
            if (cityCode != null && !cityCode.isEmpty()) {
                ps.add(cb.equal(root.get("cityCode"), cityCode));
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                ps.add(cb.equal(root.get("countryCode"), countryCode));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        return hotelRepo.findAll(spec, PageRequest.of(0, 100)).getContent().stream().map(this::toXHotel).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROOM, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #roomCode ")
    public List<XRoom> listRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode) {
        Specification<Room> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (hotelCode != null && !hotelCode.isEmpty()) {
                ps.add(cb.equal(root.get("hotelCode"), hotelCode));
            }
            if (roomCode != null && !roomCode.isEmpty()) {
                ps.add(cb.like(root.get("roomCodeMd5"), "%" + roomCode + "%"));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        // 执行查询并转换为XRoom列表
        return roomRepo.findAll(spec, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(this::toXRoom)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.RATE_PLAN, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #roomCode + ':' + #ratePlanCode ")
    public List<XRatePlan> listRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode) {
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
                ps.add(cb.like(root.get("ratePlanCodeMd5"), "%" + ratePlanCode + "%"));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
        // 执行查询并转换为XRatePlan列表
        return ratePlanRepo.findAll(spec, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(this::toXRatePlan)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.NATIONALITY, key = "#supplierId + ':' + #supplierCode + ':' + #nationalityCode + ':' + #isoCode")
    public List<XNationality> listNationalities(Long supplierId, String supplierCode, String nationalityCode, String isoCode) {
        Specification<Nationality> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("supplierId"), supplierId));
            ps.add(cb.equal(root.get("supplierCode"), supplierCode));
            if (nationalityCode != null && !nationalityCode.isEmpty()) {
                ps.add(cb.like(root.get("nationalityCode"), "%" + nationalityCode + "%"));
            }

            if (isoCode != null && !isoCode.isEmpty()) {
                ps.add(cb.equal(cb.lower(root.get("isoCode")), isoCode.toLowerCase()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return nationalityRepo.findAll(spec, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(this::toXNationality)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.GIATA, key = "#supplierId + ':' + #supplierCode + ':' + #hotelCode + ':' + #giataId")
    public List<XHotelGiata> listGiataMappings(Long supplierId, String supplierCode, String hotelCode, String giataId) {
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
        return giataRepo.findAll(spec, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(this::toXGiata)
                .collect(Collectors.toList());
    }


    // ================= 单条查询（ByCode） =================

    @Override
    @Cacheable(cacheNames = CacheNames.HOTEL, key = "'ONE:'+ #supplierId + ':' + #supplierCode + ':' + #hotelCode")
    public Optional<XHotel> getHotelByHotelCode(Long supplierId, String supplierCode, String hotelCode) {
        Specification<Hotel> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("supplierId"), supplierId),
                cb.equal(root.get("supplierCode"), supplierCode),
                cb.equal(root.get("hotelCodeMd5"), hotelCode)
        );
        return hotelRepo.findAll(spec, PageRequest.of(0, 1)).get().findFirst().map(this::toXHotel);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROOM, key = "'ONE:'+ #supplierId + ':' + #supplierCode + ':' +  #roomCode")
    public Optional<XRoom> getRoomByRoomCode(Long supplierId, String supplierCode,String roomCode) {
        // DTO 映射未完成，先返回空
        return Optional.empty();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.RATE_PLAN, key = "'ONE:'+ #supplierId + ':' + #supplierCode + ':' + #ratePlanCode")
    public Optional<XRatePlan> getRatePlanByRatePlanCode(Long supplierId, String supplierCode,  String ratePlanCode) {
        // DTO 映射未完成，先返回空
        return Optional.empty();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.NATIONALITY, key = "'ONE:'+ #supplierId + ':' + #supplierCode + ':' + #nationalityCode")
    public Optional<XNationality> getNationalityByNationalityCode(Long supplierId, String supplierCode, String nationalityCode) {
        Specification<Nationality> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("supplierId"), supplierId),
                cb.equal(root.get("supplierCode"), supplierCode),
                cb.equal(root.get("nationalityCode"), nationalityCode)
        );
        return nationalityRepo.findAll(spec, PageRequest.of(0, 1)).get().findFirst().map(this::toXNationality);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.COUNTRY, key = "'ONE:'+ #supplierId + ':' + #supplierCode + ':' + #countryCode")
    public Optional<XCountryResponse> getCountryByCountryCode(Long supplierId, String supplierCode, String countryCode) {
        Specification<Country> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("supplierId"), supplierId),
                cb.equal(root.get("supplierCode"), supplierCode),
                cb.equal(root.get("countryCode"), countryCode)
        );
        return countryRepo.findAll(spec, PageRequest.of(0, 1)).get().findFirst().map(this::toXCountry);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CITY, key = "'ONE:'+ #supplierId + ':' + #supplierCode + ':' + #cityCode")
    public Optional<XCityResponse> getCityByCityCode(Long supplierId, String supplierCode, String cityCode) {
        Specification<City> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("supplierId"), supplierId),
                cb.equal(root.get("supplierCode"), supplierCode),
                cb.equal(root.get("cityCode"), cityCode)
        );
        return cityRepo.findAll(spec, PageRequest.of(0, 1)).get().findFirst().map(this::toXCity);
    }


    // ================== 映射方法 ==================


    /**
     * 国家实体转XCountry DTO
     * @param e
     * @return
     */
    private XCountryResponse toXCountry(Country e) {
        XCountryResponse x = new XCountryResponse();
        x.setId(e.getCountryCode());
        x.setNameEn(e.getCountryName());
        x.setExt(e.getCountryCode());
        return x;
    }

    /**
     * 城市实体转XCity DTO
     * @param e
     * @return
     */
    private XCityResponse toXCity(City e) {
        XCityResponse x = new XCityResponse();
        x.setNameEn(e.getName());
        x.setId(e.getCityCode());
        x.setExt(e.getCountryCode());
        return x;
    }


    /**
     * 国籍实体转XNationality DTO
     * @param e
     * @return
     */
    private XNationality toXNationality(Nationality e) {
        XNationality x = new XNationality();
        x.setNationalityCode(e.getNationalityCode());
        x.setNationality(e.getNationality());
        x.setIsoCode(e.getIsoCode());
        return x;
    }

    /**
     * Giata实体转XHotelGiata DTO
     *
     * @param e Giata实体
     * @return XHotelGiata DTO
     */
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



    /**
     * Hotel实体转XHotel DTO
     *
     * @param e Hotel实体
     * @return XHotel DTO
     */
    private XHotel toXHotel(Hotel e) {
        XHotel x = new XHotel();
        x.setHotelId(e.getHotelCode());
        x.setHotelName(e.getHotelName());
        x.setLocaleName(e.getLocaleName());
        x.setCountryCode(e.getCountryCode());
        x.setCountryId(Integer.valueOf(e.getCountryId()));
        x.setCountry(e.getCountry());
        x.setCityId(e.getCityId());
        x.setCity(e.getCity());
        x.setAreaId(e.getAreaId());
        x.setArea(e.getArea());
        x.setAddress(e.getAddress());
        x.setAddressLocale(e.getAddressLocale());
        x.setPhone(e.getPhone());
        x.setLatitude(e.getLatitude());
        x.setLongitude(e.getLongitude());
        try {
            if (e.getRating() != null && !e.getRating().trim().isEmpty()) {
                x.setRating(new BigDecimal(e.getRating().trim()));
            } else {
                x.setRating(BigDecimal.ZERO);
            }
        } catch (NumberFormatException ex) {
            x.setRating(BigDecimal.ZERO);
        }
        x.setHotelType(e.getHotelType());
        x.setBrand(e.getBrand());
        x.setDescription(e.getDescription());
        x.setMinPrice(e.getMinPrice());
        x.setStatus(e.getStatus());
        x.setNumberOfRooms(e.getNumberOfRooms());
        x.setYearPropertyOpened(e.getYearPropertyOpened());
        x.setMostRecentRenovation(e.getMostRecentRenovation());
        x.setCheckInFrom(e.getCheckInFrom());
        x.setCheckOutUtil(e.getCheckOutUtil());
        x.setPostalCode(e.getPostalCode());
        x.setHeroImg(e.getHeroImg());
        x.setEmail(e.getEmail());
        x.setExt(e.getExt());
        return x;
    }

    /**
     * Room实体转XRoom DTO
     * 
     * @param room Room实体
     * @return XRoom DTO
     */
    private XRoom toXRoom(Room room) {
        XRoom xRoom = new XRoom();
        
        // 基础信息
        xRoom.setRoomId(room.getRoomCodeMd5());
        xRoom.setRoomName(room.getRoomName());
        xRoom.setRoomNameEn(room.getRoomNameEn());
        xRoom.setDescription(room.getDescription());
        
        // 床型信息
        //xRoom.setBedTypeDesc(room.getBedTypeDesc());
        //xRoom.setBedTypeDescEn(room.getBedTypeDescEn());
        xRoom.setBedWidth(room.getBedWidth());
        
        // 房型属性
        xRoom.setMaxOccupancy(room.getMaxOccupancy());
        xRoom.setFloor(room.getFloor());
        xRoom.setArea(room.getArea());
        xRoom.setViews(room.getViews());
        

        
        // 价格信息
        xRoom.setMinPrice(room.getMinPrice());
        xRoom.setMinBasePrice(room.getMinBasePrice());
        xRoom.setExt(room.getExt());
        
        return xRoom;
    }

    /**
     * RatePlan实体转XRatePlan DTO
     * 
     * @param ratePlan RatePlan实体
     * @return XRatePlan DTO
     */
    private XRatePlan toXRatePlan(RatePlan ratePlan) {
        XRatePlan xRatePlan = new XRatePlan();
        
        // 基础信息
        xRatePlan.setRatePlanId(ratePlan.getRatePlanCode());
        xRatePlan.setRatePlanName(ratePlan.getName());
        xRatePlan.setDescription(ratePlan.getName());
        
        // 餐食信息

        // 取消政策（JSON字符串字段暂时设为原始值，需要解析JSON转换为取消规则列表）
        // TODO: 需要实现JSON字符串到XCancelRule列表的转换
        
        // 默认值设置（这些字段在静态数据中通常没有具体值，需要在报价时填充）
        xRatePlan.setAvailable(0); // 可售数量需要从实时报价获取，默认为0
        xRatePlan.setCancelable(false); // 是否可取消需要从实时报价获取，默认为false
        xRatePlan.setInstantConfirm(false); // 是否即时确认需要从实时报价获取，默认为false，需要调用取消规则接口确费后才能 立即预定
        
        // 餐食标识（需要根据meal字段解析）
        if (ratePlan.getMeal() != null) {
            String meal = ratePlan.getMeal().toLowerCase();
            xRatePlan.setBreakfast(meal.contains("breakfast") ? 1 : 0);
            xRatePlan.setLunch(meal.contains("lunch") ? 1 : 0);
            xRatePlan.setDinner(meal.contains("dinner") ? 1 : 0);
        } else {
            xRatePlan.setBreakfast(0);
            xRatePlan.setLunch(0);
            xRatePlan.setDinner(0);
        }
        
        return xRatePlan;
    }

    // ================= 增量查询实现（基于自增ID） =================

    @Override
    public Page<Hotel> getIncrementalHotels(Long supplierId, String supplierCode, Long maxId, int pageSize) {
        logger.info("开始执行酒店增量查询，供应商ID：{}，供应商代码：{}，最大ID：{}，页面大小：{}", 
                   supplierId, supplierCode, maxId, pageSize);
        
        try {
            // 参数校验和规范化
            Long validMaxId = maxId != null ? Math.max(maxId, 0L) : 0L;
            int validPageSize = Math.min(Math.max(pageSize, 1), 1000);
            
            // 创建分页对象，按ID升序排列确保增量顺序
            Pageable pageable = PageRequest.of(0, validPageSize);
            
            // 执行增量查询
            Page<Hotel> pageData = hotelRepo.findIncrementalHotels(supplierId, supplierCode, validMaxId, pageable);

            
            logger.info("酒店增量查询完成，返回{}条记录，总记录数：{}", pageData.getContent().size(), pageData.getTotalElements());
            
            return new PageImpl<>(pageData.getContent(), pageable, pageData.getTotalElements());
            
        } catch (Exception e) {
            logger.error("酒店增量查询异常，供应商ID：{}，供应商代码：{}，最大ID：{}", 
                        supplierId, supplierCode, maxId, e);
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), 0);
        }
    }

    @Override
    public Page<Hotel> getIncrementalHotelsByTime(Long supplierId, String supplierCode, Long maxId,
                                                  LocalDateTime minTime, int pageSize) {
        logger.info("开始执行酒店增量查询（带时间过滤），供应商ID：{}，供应商代码：{}，最大ID：{}，最小时间：{}，页面大小：{}", 
                   supplierId, supplierCode, maxId, minTime, pageSize);
        
        try {
            // 参数校验和规范化
            Long validMaxId = maxId != null ? Math.max(maxId, 0L) : 0L;
            int validPageSize = Math.min(Math.max(pageSize, 1), 1000);
            LocalDateTime validMinTime = minTime != null ? minTime : LocalDateTime.now().minusDays(30);
            
            // 创建分页对象，按ID升序排列确保增量顺序
            Pageable pageable = PageRequest.of(0, validPageSize);
            
            // 执行增量查询（带时间过滤）
            Page<Hotel> pageData = hotelRepo.findIncrementalHotelsByTime(supplierId, supplierCode, validMaxId, validMinTime, pageable);

            logger.info("酒店增量查询（带时间过滤）完成，返回{}条记录，总记录数：{}", pageData.getContent().size(), pageData.getTotalElements());
            
            return new PageImpl<>(pageData.getContent(), pageable, pageData.getTotalElements());
            
        } catch (Exception e) {
            logger.error("酒店增量查询（带时间过滤）异常，供应商ID：{}，供应商代码：{}，最大ID：{}", 
                        supplierId, supplierCode, maxId, e);
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), 0);
        }
    }

    @Override
    public Page<Room> getIncrementalRooms(Long supplierId, String supplierCode, Long maxId, int pageSize) {
        logger.info("开始执行房型增量查询，供应商ID：{}，供应商代码：{}，最大ID：{}，页面大小：{}", 
                   supplierId, supplierCode, maxId, pageSize);
        
        try {
            // 参数校验和规范化
            Long validMaxId = maxId != null ? Math.max(maxId, 0L) : 0L;
            int validPageSize = Math.min(Math.max(pageSize, 1), 1000);
            
            // 创建分页对象，按ID升序排列确保增量顺序
            Pageable pageable = PageRequest.of(0, validPageSize);
            
            // 执行增量查询
            Page<Room> pageData = roomRepo.findIncrementalRooms(supplierId, supplierCode, validMaxId, pageable);
            logger.info("房型增量查询完成，返回{}条记录，总记录数：{}", pageData.getContent().size(), pageData.getTotalElements());

            return new PageImpl<>(pageData.getContent(), pageable, pageData.getTotalElements());
            
        } catch (Exception e) {
            logger.error("房型增量查询异常，供应商ID：{}，供应商代码：{}，最大ID：{}", 
                        supplierId, supplierCode, maxId, e);
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), 0);
        }
    }

    @Override
    public Page<Room> getIncrementalRoomsByTime(Long supplierId, String supplierCode, Long maxId,
                                                LocalDateTime minTime, int pageSize) {
        logger.info("开始执行房型增量查询（带时间过滤），供应商ID：{}，供应商代码：{}，最大ID：{}，最小时间：{}，页面大小：{}", 
                   supplierId, supplierCode, maxId, minTime, pageSize);
        
        try {
            // 参数校验和规范化
            Long validMaxId = maxId != null ? Math.max(maxId, 0L) : 0L;
            int validPageSize = Math.min(Math.max(pageSize, 1), 1000);
            LocalDateTime validMinTime = minTime != null ? minTime : LocalDateTime.now().minusDays(30);
            
            // 创建分页对象，按ID升序排列确保增量顺序
            Pageable pageable = PageRequest.of(0, validPageSize);
            
            // 执行增量查询（带时间过滤）
            Page<Room> pageData = roomRepo.findIncrementalRoomsByTime(supplierId, supplierCode, validMaxId, validMinTime, pageable);

            logger.info("房型增量查询（带时间过滤）完成，返回{}条记录，总记录数：{}", pageData.getContent().size(), pageData.getTotalElements());

            return new PageImpl<>(pageData.getContent(), pageable, pageData.getTotalElements());
            
        } catch (Exception e) {
            logger.error("房型增量查询（带时间过滤）异常，供应商ID：{}，供应商代码：{}，最大ID：{}", 
                        supplierId, supplierCode, maxId, e);
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), 0);
        }
    }


}
